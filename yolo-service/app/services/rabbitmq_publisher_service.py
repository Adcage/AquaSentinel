from __future__ import annotations

import json
import logging
import threading
import time
from typing import Any

import pika
from pika.adapters.blocking_connection import BlockingChannel

logger = logging.getLogger(__name__)

_RECONNECT_DELAYS = [1.0, 2.0, 5.0, 10.0, 30.0]


class RabbitmqPublisherService:
    """RabbitMQ 报警事件发布服务。

    将检测到的报警事件发布到 RabbitMQ alert.topic Exchange，
    作为持久化保障通道，与 WebSocket 实时推送并行运行。
    """

    def __init__(self) -> None:
        self._connection: BlockingChannel | None = None
        self._channel: BlockingChannel | None = None
        self._lock = threading.Lock()
        self._stop_event = threading.Event()
        self._thread: threading.Thread | None = None
        self._url: str = ""
        self._exchange: str = "alert.topic"
        self._retry_index = 0
        self._connected = False

    def start(self, url: str, exchange: str = "alert.topic") -> None:
        self._url = url
        self._exchange = exchange
        self._stop_event.clear()
        if self._thread and self._thread.is_alive():
            return
        self._thread = threading.Thread(
            target=self._run_loop,
            name="rabbitmq-publisher",
            daemon=True,
        )
        self._thread.start()
        logger.info(
            "RabbitMQ publisher service started, url=%s, exchange=%s", url, exchange
        )

    def stop(self) -> None:
        self._stop_event.set()
        with self._lock:
            try:
                if self._connection and self._connection.is_open:
                    self._connection.close()
            except Exception:
                pass
            self._connection = None
            self._channel = None
            self._connected = False

    def publish_alert(
        self, payload: dict[str, Any], routing_key: str = "alert.record"
    ) -> bool:
        with self._lock:
            if not self._connected or self._channel is None:
                logger.warning(
                    "RabbitMQ not connected, skipping publish for eventUid=%s",
                    payload.get("eventUid", "unknown"),
                )
                return False
            try:
                body = json.dumps(payload, ensure_ascii=False, default=str).encode(
                    "utf-8"
                )
                self._channel.basic_publish(
                    exchange=self._exchange,
                    routing_key=routing_key,
                    body=body,
                    properties=pika.BasicProperties(
                        delivery_mode=2,
                        content_type="application/json",
                        message_id=str(payload.get("eventUid", "")),
                    ),
                )
                logger.debug(
                    "Published alert event to RabbitMQ, eventUid=%s, routingKey=%s",
                    payload.get("eventUid", "unknown"),
                    routing_key,
                )
                return True
            except Exception as exc:
                logger.warning("RabbitMQ publish failed: %s", exc)
                self._connected = False
                self._channel = None
                return False

    def is_connected(self) -> bool:
        with self._lock:
            return self._connected

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
                    "RabbitMQ publisher disconnected (%s), reconnecting in %.1fs (attempt %d)",
                    exc,
                    delay,
                    self._retry_index,
                )
                self._stop_event.wait(delay)

    def _connect_and_block(self) -> None:
        connection_params = pika.URLParameters(self._url)
        connection = pika.BlockingConnection(connection_params)
        channel = connection.channel()

        channel.exchange_declare(
            exchange=self._exchange,
            exchange_type="topic",
            durable=True,
        )

        for rk, queue_name in [
            ("alert.record", "alert.record.queue"),
            ("alert.notification", "alert.notification.queue"),
            ("alert.analytics", "alert.analytics.queue"),
        ]:
            channel.queue_declare(
                queue=queue_name, durable=True, arguments={"x-message-ttl": 86400000}
            )
            channel.queue_bind(
                queue=queue_name, exchange=self._exchange, routing_key=rk
            )

        with self._lock:
            self._connection = connection
            self._channel = channel
            self._connected = True

        logger.info("RabbitMQ publisher connected to %s", self._url)

        try:
            while not self._stop_event.is_set():
                try:
                    connection.process_data_events(time_limit=1)
                except Exception:
                    break
        except Exception:
            pass
        finally:
            with self._lock:
                self._connected = False
                self._channel = None
                try:
                    if connection.is_open:
                        connection.close()
                except Exception:
                    pass


rabbitmq_publisher_service = RabbitmqPublisherService()
