from __future__ import annotations

from flask import Blueprint, jsonify

blp = Blueprint("health", __name__)


@blp.get("/health")
def health():
    return jsonify({
        "code": "OK",
        "message": "ok",
        "data": {"service": "video-hub-service"},
    })
