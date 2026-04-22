from __future__ import annotations

import json
import logging
import threading
import time
from typing import Any

logger = logging.getLogger(__name__)

_RECONNECT_DELAYS = [1.0, 2.0, 5.0, 10.0, 30.0]


class AiWsPushService:
    """
    Python AI 引擎 WebSocket 客户端。
    连接到 Java 后端的 /ws/ai-push 内部端点，
    在检测到目标时主动推送结果，无需等待 Java 来轮询。
    """

    def __init__(self) -> None:
        self._ws = None
        self._ws_lock = threading.Lock()
        self._send_lock = threading.Lock()
        self._stop_event = threading.Event()
        self._url: str = ""
        self._thread: threading.Thread | None = None
        self._retry_index = 0

    def start(self, url: str) -> None:
        self._url = url
        self._stop_event.clear()
        if self._thread and self._thread.is_alive():
            return
        self._thread = threading.Thread(
            target=self._run_loop,
            name="ai-ws-push",
            daemon=True,
        )
        self._thread.start()
        logger.info("AI WS push service started, url=%s", url)

    def stop(self) -> None:
        self._stop_event.set()
        with self._ws_lock:
            if self._ws is not None:
                try:
                    self._ws.close()
                except Exception:
                    pass
                self._ws = None

    def push(self, payload: dict[str, Any]) -> bool:
        with self._ws_lock:
            ws = self._ws
        if ws is None:
            return False
        try:
            ws.send(json.dumps(payload, ensure_ascii=False))
            return True
        except Exception as exc:
            logger.warning("AI WS push failed: %s", exc)
            with self._ws_lock:
                self._ws = None
            return False

    def push_binary(self, data: bytes) -> bool:
        with self._ws_lock:
            ws = self._ws
        if ws is None:
            return False
        try:
            ws.send(data, opcode=2)
            return True
        except Exception as exc:
            logger.warning("AI WS binary push failed: %s", exc)
            with self._ws_lock:
                self._ws = None
            return False
        try:
            import websocket as ws_module

            ws.send(data, opcode=ws_module.ABOP.OPCODE_BINARY)
            return True
        except Exception as exc:
            logger.warning("AI WS binary push failed: %s", exc)
            with self._ws_lock:
                self._ws = None
            return False

    def push_video_frame(self, header_payload: dict[str, Any], jpeg_bytes: bytes) -> bool:
        """原子发送视频帧：先发 JSON header 文本帧，再发 binary JPEG，在同一锁内完成。"""
        with self._send_lock:
            with self._ws_lock:
                ws = self._ws
            if ws is None:
                return False
            try:
                ws.send(json.dumps(header_payload, ensure_ascii=False))
                ws.send(jpeg_bytes, opcode=2)
                return True
            except Exception as exc:
                logger.warning("AI WS video frame push failed: %s", exc)
                with self._ws_lock:
                    self._ws = None
                return False

    def push_binary_header(self, payload: dict[str, Any]) -> bool:
        with self._ws_lock:
            ws = self._ws
        if ws is None:
            return False
        try:
            ws.send(json.dumps(payload, ensure_ascii=False))
            return True
        except Exception as exc:
            logger.warning("AI WS binary header push failed: %s", exc)
            with self._ws_lock:
                self._ws = None
            return False

    def is_connected(self) -> bool:
        with self._ws_lock:
            return self._ws is not None

    def _run_loop(self) -> None:
        self._stop_event.wait(2.0)
        while not self._stop_event.is_set():
            try:
                self._connect_and_block()
                self._retry_index = 0
            except Exception as exc:
                if self._stop_event.is_set():
                    break
                delay = _RECONNECT_DELAYS[
                    min(self._retry_index, len(_RECONNECT_DELAYS) - 1)
                ]
                self._retry_index += 1
                logger.warning(
                    "AI WS push disconnected (%s), reconnecting in %.1fs (attempt %d)",
                    exc,
                    delay,
                    self._retry_index,
                )
                self._stop_event.wait(delay)

    def _connect_and_block(self) -> None:
        try:
            import websocket as ws_module
        except ImportError:
            logger.error(
                "websocket-client not installed; run: pip install websocket-client"
            )
            self._stop_event.wait(30.0)
            return

        logger.info("AI WS push connecting to %s", self._url)
        opened = threading.Event()

        def _on_open_wrap(ws) -> None:
            opened.set()
            self._on_open(ws)

        ws = ws_module.WebSocketApp(
            self._url,
            on_open=_on_open_wrap,
            on_close=self._on_close,
            on_error=self._on_error,
        )
        ws.run_forever(ping_interval=0)
        if not opened.is_set():
            raise ConnectionError(f"ws connection to {self._url} failed (never opened)")

    def _on_open(self, ws) -> None:
        with self._ws_lock:
            self._ws = ws
        self._retry_index = 0
        logger.info("AI WS push connected")

    def _on_close(self, ws, close_status_code, close_msg) -> None:
        with self._ws_lock:
            if self._ws is ws:
                self._ws = None
        logger.info("AI WS push closed, status=%s msg=%s", close_status_code, close_msg)

    def _on_error(self, ws, error) -> None:
        with self._ws_lock:
            if self._ws is ws:
                self._ws = None
        logger.warning("AI WS push error: %s", error)


ai_ws_push_service = AiWsPushService()
