from __future__ import annotations

import hashlib
import hmac
import json
import time

import requests
from flask import current_app

from app.metrics.task_metrics import record_alert_published


def _build_signature(secret: str, timestamp: str, body: str) -> str:
    message = f"{timestamp}\n{body}".encode("utf-8")
    digest = hmac.new(secret.encode("utf-8"), message, hashlib.sha256).hexdigest()
    return digest


def post_task_callback(payload: dict) -> bool:
    callback_url = str(current_app.config.get("CALLBACK_URL", "")).strip()
    callback_key = str(current_app.config.get("CALLBACK_KEY", "")).strip()
    callback_secret = str(current_app.config.get("CALLBACK_SECRET", "")).strip()
    retry_times = max(1, int(current_app.config.get("CALLBACK_RETRY_TIMES", 3)))
    timeout_sec = max(1.0, float(current_app.config.get("CALLBACK_TIMEOUT_SEC", 5.0)))

    if not callback_url:
        current_app.logger.warning("Skip callback: CALLBACK_URL is empty")
        return False

    body = json.dumps(payload, ensure_ascii=False, separators=(",", ":"))
    timestamp = str(int(time.time()))
    signature = _build_signature(callback_secret, timestamp, body)
    headers = {
        "Content-Type": "application/json",
        "X-AI-Key": callback_key,
        "X-AI-Timestamp": timestamp,
        "X-AI-Signature": signature,
    }

    for attempt in range(1, retry_times + 1):
        try:
            response = requests.post(
                callback_url,
                data=body.encode("utf-8"),
                headers=headers,
                timeout=timeout_sec,
            )
            if response.ok:
                record_alert_published(channel="http_callback", status="success")
                return True
            current_app.logger.warning(
                "Callback failed, status=%s, attempt=%s/%s",
                response.status_code,
                attempt,
                retry_times,
            )
        except Exception as exc:
            current_app.logger.warning(
                "Callback exception: %s, attempt=%s/%s",
                exc,
                attempt,
                retry_times,
            )

        if attempt < retry_times:
            time.sleep(min(2.0, 0.2 * attempt))

    record_alert_published(channel="http_callback", status="failed")
    return False
