from __future__ import annotations

import logging

from flask import Flask

from app.common.errors import register_error_handlers
from app.core.config import BaseConfig


def create_app(config_overrides: dict | None = None) -> Flask:
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s %(levelname)s [%(name)s] %(message)s",
    )
    logging.getLogger("aioice").setLevel(logging.INFO)
    logging.getLogger("aiortc").setLevel(logging.INFO)

    app = Flask(__name__)
    app.config.from_object(BaseConfig)

    if config_overrides:
        app.config.update(config_overrides)

    from app.api.health import blp as health_blp
    from app.api.video_hub import blp as video_hub_blp
    from app.api.video_hub_webrtc import blp as video_hub_webrtc_blp

    app.register_blueprint(health_blp)
    app.register_blueprint(video_hub_blp)
    app.register_blueprint(video_hub_webrtc_blp)

    register_error_handlers(app)

    @app.after_request
    def _add_cors_headers(response):
        response.headers["Access-Control-Allow-Origin"] = "*"
        response.headers["Access-Control-Allow-Methods"] = "GET, POST, DELETE, OPTIONS"
        response.headers["Access-Control-Allow-Headers"] = "Content-Type, Accept, Authorization"
        return response

    return app
