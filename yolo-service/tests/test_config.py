from app import create_app
from app.core.config import BaseConfig


def test_default_database_uses_sqlite(monkeypatch):
    monkeypatch.delenv("DATABASE_URL", raising=False)
    app = create_app({"TESTING": True, "ENABLED_MODULES": "health"})
    assert app.config["SQLALCHEMY_DATABASE_URI"].startswith("sqlite:///")


def test_mysql_database_url_can_override(monkeypatch):
    monkeypatch.setenv("DATABASE_URL", "mysql+pymysql://u:p@127.0.0.1:3306/demo")
    app = create_app(
        {"TESTING": True, "ENABLED_MODULES": "health", "AUTO_CREATE_TABLES": False}
    )
    assert app.config["SQLALCHEMY_DATABASE_URI"].startswith("mysql+pymysql://")


def test_storage_related_config_can_override(monkeypatch):
    monkeypatch.setenv("STORAGE_ROOT", "instance/custom_storage")
    monkeypatch.setenv("MAX_UPLOAD_SIZE_MB", "32")
    monkeypatch.setenv("TABULAR_ALLOWED_EXTENSIONS", "csv,xlsx")

    app = create_app(
        {
            "TESTING": True,
            "ENABLED_MODULES": "health",
            "AUTO_CREATE_TABLES": False,
        }
    )
    assert app.config["STORAGE_ROOT"] == "instance/custom_storage"
    assert app.config["MAX_UPLOAD_SIZE_MB"] == 32
    assert app.config["TABULAR_ALLOWED_EXTENSIONS"] == "csv,xlsx"


def test_upload_scene_config_can_override(monkeypatch):
    monkeypatch.setenv("UPLOAD_SCENE_DIRS", "default:uploads,avatar:avatars")

    app = create_app(
        {
            "TESTING": True,
            "ENABLED_MODULES": "health",
            "AUTO_CREATE_TABLES": False,
        }
    )
    assert app.config["UPLOAD_SCENE_DIRS"] == "default:uploads,avatar:avatars"


def test_enabled_modules_constant_matches_joined_default_modules():
    assert BaseConfig.ENABLED_MODULES == ",".join(BaseConfig.DEFAULT_ENABLED_MODULES)


def test_model_paths_can_override(monkeypatch):
    monkeypatch.setenv("MODEL_PATH", "model/custom.pt")
    monkeypatch.setenv("MODEL_LABELS_PATH", "model/custom_labels.json")

    app = create_app(
        {
            "TESTING": True,
            "ENABLED_MODULES": "health",
            "AUTO_CREATE_TABLES": False,
        }
    )
    assert app.config["MODEL_PATH"] == "model/custom.pt"
    assert app.config["MODEL_LABELS_PATH"] == "model/custom_labels.json"
