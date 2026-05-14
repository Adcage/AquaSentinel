import json
import os


def _env_bool(name: str, default: bool) -> bool:
    raw = os.getenv(name)
    if raw is None:
        return default

    value = raw.strip().lower()
    return value in {"1", "true", "yes", "on"}


def _env_int(name: str, default: int) -> int:
    raw = os.getenv(name)
    if raw is None:
        return default

    try:
        return int(raw.strip())
    except ValueError:
        return default


def _env_float(name: str, default: float) -> float:
    raw = os.getenv(name)
    if raw is None:
        return default

    try:
        return float(raw.strip())
    except ValueError:
        return default


class BaseConfig:
    TESTING = False
    APP_ENV = "dev"
    LOG_LEVEL = "INFO"
    SQLALCHEMY_DATABASE_URI = "sqlite:///instance/app.db"
    SQLALCHEMY_TRACK_MODIFICATIONS = False
    DEFAULT_ENABLED_MODULES = ("health",)
    ENABLED_MODULES = ",".join(DEFAULT_ENABLED_MODULES)
    AUTO_CREATE_TABLES = True
    STORAGE_ROOT = "instance/storage"
    MAX_UPLOAD_SIZE_MB = 10
    TABULAR_ALLOWED_EXTENSIONS = "csv,xlsx"
    UPLOAD_ALLOWED_EXTENSIONS = "jpg,jpeg,png,gif,webp,pdf,txt,csv,xlsx"
    UPLOAD_SCENE_DIRS = (
        "default:uploads,avatar:avatars,recognition_image:recognition/images,"
        "recognition_video:recognition/videos,recognition_report:recognition/reports"
    )
    PDF_DEFAULT_FONT = "Helvetica"
    VIDEO_ALLOWED_EXTENSIONS = "mp4,avi,mov,mkv"
    MAX_VIDEO_SIZE_MB = 500
    MODEL_PATH = "model/best.pt"
    MODEL_LABELS_PATH = "model/labels.json"
    MODEL_CONF_THRESHOLD = 0.25
    VIDEO_INFER_FRAME_INTERVAL = 0.2
    ENGINE_DEFAULT_FRAME_INTERVAL = 0.2
    ENGINE_CALLBACK_INTERVAL_SEC = 3.0
    ENGINE_TRACK_IOU_THRESHOLD = 0.3
    ENGINE_TRACK_MAX_AGE_SEC = 1.5
    ENGINE_DROWNING_MIN_DURATION_SEC = 3.0
    ENGINE_DROWNING_POSTURE_THRESHOLD = 0.7
    ENGINE_DROWNING_THERMAL_THRESHOLD = 0.85
    ENGINE_DROWNING_EVENT_COOLDOWN_SEC = 15.0
    MODEL_VERSION = "v1"
    MODEL_VERSION_PATHS = {}
    RECOGNITION_USE_FAKE_MODEL = False
    CALLBACK_URL = ""
    CALLBACK_KEY = ""
    CALLBACK_SECRET = ""
    CALLBACK_RETRY_TIMES = 3
    CALLBACK_TIMEOUT_SEC = 5.0
    CAMERA_MAX_UPLOAD_SIZE_MB = 2
    CAMERA_TARGET_WIDTH = 640
    CAMERA_INFER_TIMEOUT_MS = 8000
    RABBITMQ_URL = "amqp://localhost:5672/"
    RABBITMQ_EXCHANGE = "alert.topic"
    METRICS_ENABLED = True
    METRICS_PORT = 9091
    OVERLAY_SERVER_SIDE_ENABLED = False


def apply_env_overrides(app):
    """Apply environment variable overrides on top of BaseConfig.

    ENABLED_MODULES is code-configured on purpose and should be changed in
    BaseConfig or create_app(config_overrides), not via environment variables.
    """
    app.config["APP_ENV"] = os.getenv("APP_ENV", app.config["APP_ENV"])
    app.config["SQLALCHEMY_DATABASE_URI"] = os.getenv(
        "DATABASE_URL", app.config["SQLALCHEMY_DATABASE_URI"]
    )
    app.config["AUTO_CREATE_TABLES"] = _env_bool(
        "AUTO_CREATE_TABLES", app.config["AUTO_CREATE_TABLES"]
    )
    app.config["STORAGE_ROOT"] = os.getenv("STORAGE_ROOT", app.config["STORAGE_ROOT"])
    app.config["MAX_UPLOAD_SIZE_MB"] = _env_int(
        "MAX_UPLOAD_SIZE_MB", app.config["MAX_UPLOAD_SIZE_MB"]
    )
    app.config["TABULAR_ALLOWED_EXTENSIONS"] = os.getenv(
        "TABULAR_ALLOWED_EXTENSIONS", app.config["TABULAR_ALLOWED_EXTENSIONS"]
    )
    app.config["UPLOAD_ALLOWED_EXTENSIONS"] = os.getenv(
        "UPLOAD_ALLOWED_EXTENSIONS", app.config["UPLOAD_ALLOWED_EXTENSIONS"]
    )
    app.config["UPLOAD_SCENE_DIRS"] = os.getenv(
        "UPLOAD_SCENE_DIRS", app.config["UPLOAD_SCENE_DIRS"]
    )
    app.config["VIDEO_ALLOWED_EXTENSIONS"] = os.getenv(
        "VIDEO_ALLOWED_EXTENSIONS", app.config["VIDEO_ALLOWED_EXTENSIONS"]
    )
    app.config["MAX_VIDEO_SIZE_MB"] = _env_int(
        "MAX_VIDEO_SIZE_MB", app.config["MAX_VIDEO_SIZE_MB"]
    )
    app.config["MODEL_PATH"] = os.getenv("MODEL_PATH", app.config["MODEL_PATH"])
    app.config["MODEL_LABELS_PATH"] = os.getenv(
        "MODEL_LABELS_PATH", app.config["MODEL_LABELS_PATH"]
    )
    model_conf_raw = os.getenv("MODEL_CONF_THRESHOLD")
    if model_conf_raw is not None:
        try:
            app.config["MODEL_CONF_THRESHOLD"] = float(model_conf_raw)
        except ValueError:
            pass
    app.config["VIDEO_INFER_FRAME_INTERVAL"] = _env_float(
        "VIDEO_INFER_FRAME_INTERVAL", app.config["VIDEO_INFER_FRAME_INTERVAL"]
    )
    app.config["ENGINE_DEFAULT_FRAME_INTERVAL"] = _env_float(
        "ENGINE_DEFAULT_FRAME_INTERVAL", app.config["ENGINE_DEFAULT_FRAME_INTERVAL"]
    )
    app.config["ENGINE_CALLBACK_INTERVAL_SEC"] = _env_float(
        "ENGINE_CALLBACK_INTERVAL_SEC", app.config["ENGINE_CALLBACK_INTERVAL_SEC"]
    )
    app.config["ENGINE_TRACK_IOU_THRESHOLD"] = _env_float(
        "ENGINE_TRACK_IOU_THRESHOLD", app.config["ENGINE_TRACK_IOU_THRESHOLD"]
    )
    app.config["ENGINE_TRACK_MAX_AGE_SEC"] = _env_float(
        "ENGINE_TRACK_MAX_AGE_SEC", app.config["ENGINE_TRACK_MAX_AGE_SEC"]
    )
    app.config["ENGINE_DROWNING_MIN_DURATION_SEC"] = _env_float(
        "ENGINE_DROWNING_MIN_DURATION_SEC",
        app.config["ENGINE_DROWNING_MIN_DURATION_SEC"],
    )
    app.config["ENGINE_DROWNING_POSTURE_THRESHOLD"] = _env_float(
        "ENGINE_DROWNING_POSTURE_THRESHOLD",
        app.config["ENGINE_DROWNING_POSTURE_THRESHOLD"],
    )
    app.config["ENGINE_DROWNING_THERMAL_THRESHOLD"] = _env_float(
        "ENGINE_DROWNING_THERMAL_THRESHOLD",
        app.config["ENGINE_DROWNING_THERMAL_THRESHOLD"],
    )
    app.config["ENGINE_DROWNING_EVENT_COOLDOWN_SEC"] = _env_float(
        "ENGINE_DROWNING_EVENT_COOLDOWN_SEC",
        app.config["ENGINE_DROWNING_EVENT_COOLDOWN_SEC"],
    )
    app.config["MODEL_VERSION"] = os.getenv(
        "MODEL_VERSION", app.config["MODEL_VERSION"]
    )
    model_version_paths_raw = os.getenv("MODEL_VERSION_PATHS_JSON")
    if model_version_paths_raw:
        try:
            parsed_paths = json.loads(model_version_paths_raw)
            if isinstance(parsed_paths, dict):
                app.config["MODEL_VERSION_PATHS"] = parsed_paths
        except Exception:
            pass
    app.config["RECOGNITION_USE_FAKE_MODEL"] = _env_bool(
        "RECOGNITION_USE_FAKE_MODEL", app.config["RECOGNITION_USE_FAKE_MODEL"]
    )
    app.config["CALLBACK_URL"] = os.getenv("CALLBACK_URL", app.config["CALLBACK_URL"])
    app.config["CALLBACK_KEY"] = os.getenv("CALLBACK_KEY", app.config["CALLBACK_KEY"])
    app.config["CALLBACK_SECRET"] = os.getenv(
        "CALLBACK_SECRET", app.config["CALLBACK_SECRET"]
    )
    app.config["CALLBACK_RETRY_TIMES"] = _env_int(
        "CALLBACK_RETRY_TIMES", app.config["CALLBACK_RETRY_TIMES"]
    )
    app.config["CALLBACK_TIMEOUT_SEC"] = _env_float(
        "CALLBACK_TIMEOUT_SEC", app.config["CALLBACK_TIMEOUT_SEC"]
    )
    app.config["CAMERA_MAX_UPLOAD_SIZE_MB"] = _env_int(
        "CAMERA_MAX_UPLOAD_SIZE_MB", app.config["CAMERA_MAX_UPLOAD_SIZE_MB"]
    )
    app.config["CAMERA_TARGET_WIDTH"] = _env_int(
        "CAMERA_TARGET_WIDTH", app.config["CAMERA_TARGET_WIDTH"]
    )
    app.config["CAMERA_INFER_TIMEOUT_MS"] = _env_int(
        "CAMERA_INFER_TIMEOUT_MS", app.config["CAMERA_INFER_TIMEOUT_MS"]
    )
    app.config["RABBITMQ_URL"] = os.getenv("RABBITMQ_URL", app.config["RABBITMQ_URL"])
    app.config["RABBITMQ_EXCHANGE"] = os.getenv(
        "RABBITMQ_EXCHANGE", app.config["RABBITMQ_EXCHANGE"]
    )
    app.config["METRICS_ENABLED"] = _env_bool(
        "METRICS_ENABLED", app.config["METRICS_ENABLED"]
    )
    app.config["METRICS_PORT"] = _env_int("METRICS_PORT", app.config["METRICS_PORT"])
    app.config["OVERLAY_SERVER_SIDE_ENABLED"] = _env_bool(
        "OVERLAY_SERVER_SIDE_ENABLED", app.config["OVERLAY_SERVER_SIDE_ENABLED"]
    )
