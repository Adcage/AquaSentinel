"""Simulate browser SDP offer to aiortc to test ICE connectivity."""
from __future__ import annotations

import asyncio
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

    server_candidates = [
        line
        for line in server_pc.localDescription.sdp.split("\n")
        if line.startswith("a=candidate:")
    ]
    print(f"Server ICE: {server_pc.iceConnectionState}")
    print(f"Server candidates ({len(server_candidates)}):")
    for c in server_candidates:
        print(f"  {c.strip()}")

    for i in range(10):
        await asyncio.sleep(1)
        ice = server_pc.iceConnectionState
        conn = server_pc.connectionState
        print(f"{i+1}s: server_ice={ice} server_conn={conn}")
        if ice in ("completed", "connected", "failed"):
            break

    stats = await server_pc.getStats()
    pairs = [s for s in stats.values() if s.type == "candidate-pair"]
    print(f"Candidate pairs: {len(pairs)}")
    for p in pairs:
        bs = getattr(p, "bytesSent", None)
        print(f"  state={p.state} nominated={p.nominated} bytesSent={bs}")

    await server_pc.close()


asyncio.run(test())
