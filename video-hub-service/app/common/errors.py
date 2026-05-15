from __future__ import annotations

import json

from flask import jsonify
from werkzeug.exceptions import HTTPException

from app.common.response import error_payload


class BusinessError(Exception):
    def __init__(
        self, message: str, status_code: int = 400, code: str = "BUSINESS_ERROR"
    ):
        super().__init__(message)
        self.message = message
        self.status_code = status_code
        self.code = code


def _json_safe(value):
    if value is None:
        return None
    if isinstance(value, (str, int, float, bool)):
        return value
    if isinstance(value, dict):
        return {str(k): _json_safe(v) for k, v in value.items()}
    if isinstance(value, (list, tuple, set)):
        return [_json_safe(item) for item in value]
    try:
        json.dumps(value)
        return value
    except Exception:
        return str(value)


def register_error_handlers(app):
    @app.errorhandler(BusinessError)
    def handle_business_error(exc: BusinessError):
        payload = error_payload(exc.code, exc.message)
        return jsonify(payload), exc.status_code

    @app.errorhandler(HTTPException)
    def handle_http_exception(exc: HTTPException):
        status_code = exc.code or 500
        if status_code in (400, 422):
            code = "PARAM_ERROR"
        else:
            code = "HTTP_ERROR"

        detail = _json_safe(getattr(exc, "data", None))
        payload = error_payload(code, exc.description or "request error", data=detail)
        return jsonify(payload), status_code

    @app.errorhandler(Exception)
    def handle_unexpected_error(exc: Exception):
        app.logger.exception("Unhandled exception: %s", exc)
        payload = error_payload("SYSTEM_ERROR", "internal server error")
        return jsonify(payload), 500
