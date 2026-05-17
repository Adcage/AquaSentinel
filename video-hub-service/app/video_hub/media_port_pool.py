from __future__ import annotations

import asyncio
import logging
import threading
from typing import Optional

logger = logging.getLogger(__name__)

_port_pool: Optional["MediaPortPool"] = None
_pool_lock = threading.Lock()


class MediaPortPool:
    def __init__(self, start: int, end: int):
        self._start = start
        self._end = end
        self._available: list[int] = list(range(start, end + 1))
        self._used: set[int] = set()

    def allocate(self) -> Optional[int]:
        if not self._available:
            return None
        port = self._available.pop(0)
        self._used.add(port)
        return port

    def release(self, port: int) -> None:
        if port in self._used:
            self._used.discard(port)
            self._available.append(port)

    @property
    def start(self) -> int:
        return self._start

    @property
    def end(self) -> int:
        return self._end


def init_port_pool(start: int, end: int) -> MediaPortPool:
    global _port_pool
    with _pool_lock:
        _port_pool = MediaPortPool(start, end)
        logger.info("WebRTC 媒体端口池初始化: %d-%d (%d 端口)", start, end, end - start + 1)
        return _port_pool


def get_port_pool() -> Optional[MediaPortPool]:
    return _port_pool


def apply_aioice_port_patch() -> None:
    from aioice import ice

    _original_get_component_candidates = ice.Connection.get_component_candidates

    async def _patched_get_component_candidates(self, component, addresses, timeout=5):
        pool = get_port_pool()
        if pool is None:
            return await _original_get_component_candidates(self, component, addresses, timeout)

        candidates = []
        loop = asyncio.get_event_loop()
        host_protocols = []

        for address in addresses:
            port = pool.allocate()
            if port is None:
                logger.warning("媒体端口池耗尽，跳过地址 %s", address)
                continue
            try:
                transport, protocol = await loop.create_datagram_endpoint(
                    lambda: ice.StunProtocol(self), local_addr=(address, port)
                )
                sock = transport.get_extra_info("socket")
                if sock is not None:
                    import socket as _socket
                    sock.setsockopt(
                        _socket.SOL_SOCKET, _socket.SO_RCVBUF, 262144
                    )
            except OSError as exc:
                pool.release(port)
                logger.info("无法绑定 %s:%d - %s", address, port, exc)
                continue
            host_protocols.append((protocol, port))

            candidate_address = protocol.transport.get_extra_info("sockname")
            protocol.local_candidate = ice.Candidate(
                foundation=ice.candidate_foundation("host", "udp", candidate_address[0]),
                component=component,
                transport="udp",
                priority=ice.candidate_priority(component, "host"),
                host=candidate_address[0],
                port=candidate_address[1],
                type="host",
            )
            if self._transport_policy == ice.TransportPolicy.ALL:
                candidates.append(protocol.local_candidate)

        for protocol, _ in host_protocols:
            self._protocols.append(protocol)

        tasks = []

        if self.stun_server:
            for protocol, _ in host_protocols:
                import ipaddress
                if ipaddress.ip_address(protocol.local_candidate.host).version == 4:
                    tasks.append(
                        asyncio.create_task(
                            ice.server_reflexive_candidate(protocol, self.stun_server)
                        )
                    )

        if self.turn_server:
            tasks.append(
                asyncio.create_task(
                    ice.relayed_candidate(
                        component=component,
                        protocol_factory=lambda: ice.StunProtocol(self),
                        turn_server=self.turn_server,
                        turn_username=self.turn_username,
                        turn_password=self.turn_password,
                        turn_ssl=self.turn_ssl,
                        turn_transport=self.turn_transport,
                    )
                )
            )

        if len(tasks):
            done, pending = await asyncio.wait(tasks, timeout=timeout)
            for task in done:
                if task.exception() is None:
                    candidate, relay_protocol = task.result()
                    candidates.append(candidate)
                    if relay_protocol is not None:
                        self._protocols.append(relay_protocol)
            for task in pending:
                task.cancel()

        return candidates

    ice.Connection.get_component_candidates = _patched_get_component_candidates
    logger.info("已应用 aioice 端口分配补丁")
