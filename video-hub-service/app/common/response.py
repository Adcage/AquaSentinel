from __future__ import annotations

from flask import g


def current_request_id() -> str:
    return str(getattr(g, "request_id", ""))


def success_payload(data=None, message: str = "ok", code: str = "OK") -> dict:
    return {
        "code": code,
        "message": message,
        "data": data if data is not None else {},
        "request_id": current_request_id(),
    }


def error_payload(code: str, message: str, data=None) -> dict:
    return {
        "code": code,
        "message": message,
        "data": data if data is not None else {},
        "request_id": current_request_id(),
    }
