import time
import uuid

from flask import g, request


def register_middlewares(app):
    @app.before_request
    def _before_request():
        g.request_id = request.headers.get("X-Request-ID") or str(uuid.uuid4())
        g.request_start_time = time.perf_counter()

    @app.after_request
    def _after_request(response):
        request_start_time = getattr(g, "request_start_time", None)
        duration_ms = 0.0
        if request_start_time is not None:
            duration_ms = (time.perf_counter() - request_start_time) * 1000

        request_id = getattr(g, "request_id", "")
        response.headers["X-Request-ID"] = request_id
        response.headers["X-Process-Time"] = f"{duration_ms:.2f}ms"

        app.logger.info(
            "%s %s -> %s (%.2fms)",
            request.method,
            request.path,
            response.status_code,
            duration_ms,
        )
        return response
