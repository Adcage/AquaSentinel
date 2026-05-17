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

    _init_webrtc_port_pool(app)

    from app.api.health import blp as health_blp
    from app.api.video_hub import blp as video_hub_blp
    from app.api.video_hub_webrtc import blp as video_hub_webrtc_blp

    app.register_blueprint(health_blp)
    app.register_blueprint(video_hub_blp)
    app.register_blueprint(video_hub_webrtc_blp)

    from flask_sock import Sock
    sock = Sock(app)
    from app.api.video_hub_push import register_push_routes
    register_push_routes(sock)

    register_error_handlers(app)

    from app.video_hub import redis_stream_sync

    redis_stream_sync.start(app.config.get("VIDEO_HUB_REDIS_URL", ""))

    @app.after_request
    def _add_cors_headers(response):
        response.headers["Access-Control-Allow-Origin"] = "*"
        response.headers["Access-Control-Allow-Methods"] = "GET, POST, DELETE, OPTIONS"
        response.headers["Access-Control-Allow-Headers"] = "Content-Type, Accept, Authorization"
        return response

    return app


def _init_webrtc_port_pool(app: Flask) -> None:
    from app.video_hub.media_port_pool import apply_aioice_port_patch, init_port_pool

    port_range = app.config.get("WEBRTC_MEDIA_PORT_RANGE", "")
    if not port_range:
        return

    try:
        parts = str(port_range).split("-")
        start_port = int(parts[0].strip())
        end_port = int(parts[1].strip())
        if start_port <= 0 or end_port <= 0 or start_port > end_port:
            raise ValueError(f"无效端口范围: {port_range}")
        if end_port - start_port + 1 < 10:
            raise ValueError(f"端口范围太小 ({start_port}-{end_port})，至少需要 10 个端口")
    except (ValueError, IndexError) as exc:
        logging.getLogger(__name__).warning("WEBRTC_MEDIA_PORT_RANGE 配置无效: %s (%s)", port_range, exc)
        return

    init_port_pool(start_port, end_port)
    apply_aioice_port_patch()
