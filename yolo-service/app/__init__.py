from pathlib import Path

from dotenv import load_dotenv
from flask import Flask

from app.core.config import BaseConfig, apply_env_overrides
from app.core.docs import init_docs
from app.core.errors import register_error_handlers
from app.core.extensions import db
from app.core.logging import configure_logging
from app.core.middleware import register_middlewares
from app.modules import register_enabled_modules
from app.api.engine_tasks import blp as engine_tasks_blp
from app.api.test_trigger_api import blp as test_trigger_blp
from app.services.ai_ws_push_service import ai_ws_push_service
from app.services.model_inference_service import warmup_model
from app.services.rabbitmq_publisher_service import rabbitmq_publisher_service
from app.metrics.inference_metrics import *


def _ensure_sqlite_parent_dir(uri: str):
    if not uri.startswith("sqlite:///"):
        return

    if uri == "sqlite:///:memory:":
        return

    path_text = uri.replace("sqlite:///", "", 1).split("?", 1)[0]
    db_path = Path(path_text)
    db_path.parent.mkdir(parents=True, exist_ok=True)


def _repair_sqlite_file_if_invalid(uri: str):
    if not uri.startswith("sqlite:///"):
        return

    if uri == "sqlite:///:memory:":
        return

    path_text = uri.replace("sqlite:///", "", 1).split("?", 1)[0]
    db_path = Path(path_text)
    if not db_path.exists() or db_path.stat().st_size == 0:
        return

    try:
        with db_path.open("rb") as fp:
            header = fp.read(16)
    except Exception:
        return

    if header != b"SQLite format 3\x00":
        db_path.unlink(missing_ok=True)


def _sqlite_db_path(uri: str) -> Path | None:
    if not uri.startswith("sqlite:///"):
        return None
    if uri == "sqlite:///:memory:":
        return None
    path_text = uri.replace("sqlite:///", "", 1).split("?", 1)[0]
    return Path(path_text)


def _should_recreate_sqlite(uri: str, exc: Exception) -> bool:
    if _sqlite_db_path(uri) is None:
        return False
    return "unsupported file format" in str(exc).lower()


def _recreate_sqlite_file(uri: str):
    db_path = _sqlite_db_path(uri)
    if db_path is None:
        return
    db_path.unlink(missing_ok=True)


def create_app(config_overrides: dict | None = None) -> Flask:
    is_testing = bool(config_overrides and config_overrides.get("TESTING"))
    if not is_testing:
        load_dotenv(override=False)

    app = Flask(__name__)
    app.config.from_object(BaseConfig)
    apply_env_overrides(app)

    if config_overrides:
        app.config.update(config_overrides)

    if app.config.get("TESTING") and (
        not config_overrides or "SQLALCHEMY_DATABASE_URI" not in config_overrides
    ):
        current_uri = str(app.config.get("SQLALCHEMY_DATABASE_URI", ""))
        if current_uri.startswith("sqlite:///"):
            app.config["SQLALCHEMY_DATABASE_URI"] = "sqlite:///:memory:"

    _ensure_sqlite_parent_dir(app.config["SQLALCHEMY_DATABASE_URI"])
    _repair_sqlite_file_if_invalid(app.config["SQLALCHEMY_DATABASE_URI"])

    configure_logging(app)
    register_middlewares(app)

    db.init_app(app)
    init_docs(app)
    enabled_modules = register_enabled_modules(app)
    app.extensions["docs_api"].register_blueprint(engine_tasks_blp)
    app.extensions["docs_api"].register_blueprint(test_trigger_blp)
    import app.models as _app_models  # noqa: F401

    if not app.config.get("TESTING", False):
        app.logger.info("=" * 60)
        app.logger.info("Enabled modules: %s", ", ".join(enabled_modules))
        app.logger.info("=" * 60)

    if app.config.get("AUTO_CREATE_TABLES", True):
        with app.app_context():
            db_uri = str(app.config.get("SQLALCHEMY_DATABASE_URI", ""))
            try:
                db.create_all()
            except Exception as exc:
                if not _should_recreate_sqlite(db_uri, exc):
                    raise
                _recreate_sqlite_file(db_uri)
                db.create_all()

    with app.app_context():
        warmup_model()

    if not app.config.get("TESTING", False):
        java_base_url = str(
            app.config.get("JAVA_BACKEND_BASE_URL", "http://127.0.0.1:8300")
        ).rstrip("/")
        if java_base_url.endswith("/api"):
            ws_path = "/ws/ai-push"
        else:
            ws_path = "/api/ws/ai-push"
        ws_url = (
            java_base_url.replace("http://", "ws://").replace("https://", "wss://")
            + ws_path
        )
        ai_ws_push_service.start(ws_url)

        rabbitmq_url = str(app.config.get("RABBITMQ_URL", "")).strip()
        if rabbitmq_url:
            rabbitmq_exchange = str(
                app.config.get("RABBITMQ_EXCHANGE", "alert.topic")
            ).strip()
            rabbitmq_publisher_service.start(rabbitmq_url, exchange=rabbitmq_exchange)

    register_error_handlers(app)

    if app.config.get("METRICS_ENABLED", True) and not app.config.get("TESTING", False):
        from prometheus_client import start_http_server

        metrics_port = int(app.config.get("METRICS_PORT", 9091))
        try:
            start_http_server(metrics_port)
            app.logger.info(
                "Prometheus metrics server started on port %d", metrics_port
            )
        except Exception as exc:
            app.logger.warning("Failed to start metrics server: %s", exc)
    return app
