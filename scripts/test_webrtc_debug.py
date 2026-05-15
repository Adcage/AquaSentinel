"""Debug aiortc ICE candidate pair formation with browser-like SDP."""
from __future__ import annotations

import asyncio
import logging

logging.basicConfig(level=logging.DEBUG)
logging.getLogger("aioice").setLevel(logging.DEBUG)
logging.getLogger("aiortc").setLevel(logging.DEBUG)

from aiortc import RTCPeerConnection, RTCSessionDescription, MediaStreamTrack


class DummyTrack(MediaStreamTrack):
    kind = "video"

    async def recv(self):
        await asyncio.sleep(999)


BROWSER_SDP = (
    "v=0\r\n"
    "o=- 123456789 2 IN IP4 127.0.0.1\r\n"
    "s=-\r\n"
    "t=0 0\r\n"
    "a=group:BUNDLE 0\r\n"
    "a=extmap-allow-mixed\r\n"
    "a=msid-semantic:WMS\r\n"
    "m=video 9 UDP/TLS/RTP/SAVPF 96\r\n"
    "c=IN IP4 0.0.0.0\r\n"
    "a=rtcp:9 IN IP4 0.0.0.0\r\n"
    "a=ice-ufrag:abcd\r\n"
    "a=ice-pwd:abcdefghijklmnop1234567890\r\n"
    "a=ice-options:trickle\r\n"
    "a=fingerprint:sha-256 00:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD:EE:FF\r\n"
    "a=setup:actpass\r\n"
    "a=mid:0\r\n"
    "a=sendrecv\r\n"
    "a=rtcp-mux\r\n"
    "a=rtpmap:96 VP8/90000\r\n"
    "a=candidate:1125298608 1 udp 2121998079 192.168.0.181 65437 typ host generation 0 network-id 3\r\n"
    "a=candidate:3152129463 1 udp 2121932543 192.168.137.1 65438 typ host generation 0 network-id 6 network-cost 10\r\n"
    "a=end-of-candidates\r\n"
)


async def test():
    server_pc = RTCPeerConnection()
    track = DummyTrack()
    server_pc.addTrack(track)

    await server_pc.setRemoteDescription(RTCSessionDescription(BROWSER_SDP, "offer"))
    answer = await server_pc.createAnswer()
    await server_pc.setLocalDescription(answer)

    await asyncio.sleep(5)

    await server_pc.close()


asyncio.run(test())
