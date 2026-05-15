"""aiortc 客户端直连 video-hub WHIP 端点，验证 WebRTC 视频流获取。"""
from __future__ import annotations

import argparse
import asyncio
import logging
import time

from aiortc import MediaStreamTrack, RTCPeerConnection, RTCSessionDescription, VideoStreamTrack


logging.basicConfig(
    level=logging.DEBUG,
    format="%(asctime)s %(levelname)s [%(name)s] %(message)s",
)
logging.getLogger("aioice").setLevel(logging.DEBUG)
logging.getLogger("aiortc").setLevel(logging.DEBUG)


class FrameCounter(MediaStreamTrack):
    kind = "video"

    def __init__(self, track: MediaStreamTrack):
        super().__init__()
        self._track = track
        self._count = 0
        self._start_time: float | None = None

    async def recv(self):
        frame = await self._track.recv()
        self._count += 1
        if self._start_time is None:
            self._start_time = time.time()
            logging.info("首帧到达! size=%dx%d", frame.width, frame.height)
        if self._count % 30 == 0:
            elapsed = time.time() - self._start_time
            fps = self._count / elapsed if elapsed > 0 else 0
            logging.info(
                "帧统计: count=%d fps=%.1f size=%dx%d",
                self._count, fps, frame.width, frame.height,
            )
        return frame

    @property
    def frame_count(self):
        return self._count


async def run_test(whip_url: str, preferred_ip: str | None = None):
    pc = RTCPeerConnection()

    @pc.on("track")
    def on_track(track):
        logging.info("收到远端 track: kind=%s", track.kind)
        if track.kind == "video":
            counter = FrameCounter(track)
            pc.addTrack(counter)

    @pc.on("connectionstatechange")
    async def on_state():
        logging.info("连接状态: %s", pc.connectionState)

    @pc.on("iceconnectionstatechange")
    async def on_ice_state():
        logging.info("ICE 状态: %s", pc.iceConnectionState)

    pc.addTransceiver("video", direction="recvonly")

    offer = await pc.createOffer()
    await pc.setLocalDescription(offer)

    logging.info("等待 ICE gathering 完成...")
    gather_start = time.time()
    while pc.iceGatheringState != "complete":
        await asyncio.sleep(0.1)
        if time.time() - gather_start > 10:
            logging.warning("ICE gathering 超时")
            break
    logging.info("ICE gathering 完成, 耗时 %.1fs", time.time() - gather_start)

    sdp_offer = pc.localDescription.sdp
    logging.info("offer 候选数: %d", sdp_offer.count("a=candidate:"))

    url = whip_url
    if preferred_ip:
        url += f"?preferred_ip={preferred_ip}"

    logging.info("发送 WHIP 请求到 %s", url)
    import aiohttp
    async with aiohttp.ClientSession() as session:
        async with session.post(
            url,
            data=sdp_offer.encode(),
            headers={"Content-Type": "application/sdp", "Accept": "application/sdp"},
        ) as resp:
            if resp.status != 201:
                body = await resp.text()
                logging.error("WHIP 失败: status=%d body=%s", resp.status, body[:200])
                await pc.close()
                return
            sdp_answer = await resp.text()
            location = resp.headers.get("Location", "")
            logging.info("WHIP 成功: answer_len=%d location=%s", len(sdp_answer), location)

    logging.info("answer 中 a=setup: %s", [l for l in sdp_answer.split("\n") if "a=setup:" in l])
    logging.info("answer 中 a=ice-lite: %s", "a=ice-lite" in sdp_answer)
    logging.info("answer 候选数: %d", sdp_answer.count("a=candidate:"))

    await pc.setRemoteDescription(RTCSessionDescription(sdp_answer, "answer"))
    logging.info("setRemoteDescription(answer) 完成")

    logging.info("等待连接建立...")
    wait_start = time.time()
    while pc.connectionState not in ("connected", "failed", "closed"):
        await asyncio.sleep(0.5)
        elapsed = time.time() - wait_start
        logging.info(
            "等待中: connectionState=%s iceState=%s (%.1fs)",
            pc.connectionState, pc.iceConnectionState, elapsed,
        )
        if elapsed > 30:
            logging.error("连接超时")
            break

    if pc.connectionState == "connected":
        logging.info("连接成功! 等待视频帧...")
        await asyncio.sleep(15)

        stats = await pc.getStats()
        for s in stats.values():
            if s.type == "candidate-pair" and (getattr(s, "selected", False) or getattr(s, "nominated", False)):
                logging.info("选中候选对: state=%s rtt=%s", s.state, getattr(s, "currentRoundTripTime", None))
            if s.type == "inbound-rtp" and getattr(s, "kind", "") == "video":
                logging.info(
                    "入站视频: packetsReceived=%d bytesReceived=%d framesDecoded=%d",
                    getattr(s, "packetsReceived", 0),
                    getattr(s, "bytesReceived", 0),
                    getattr(s, "framesDecoded", 0),
                )
    else:
        logging.error("连接失败: %s", pc.connectionState)
        stats = await pc.getStats()
        for s in stats.values():
            if s.type == "candidate-pair":
                logging.info("候选对: state=%s nominated=%s", s.state, getattr(s, "nominated", None))
            if s.type == "transport":
                logging.info("传输: dtlsState=%s", getattr(s, "dtlsState", None))

    await pc.close()
    logging.info("测试完成")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--url", default="http://127.0.0.1:5100/video-hub/cameras/5021/whip")
    parser.add_argument("--preferred-ip", default="192.168.0.181")
    parser.add_argument("--source-url", default="http://192.168.137.5/stream")
    args = parser.parse_args()

    url = args.url
    if args.source_url:
        url += f"?source_url={args.source_url}"

    asyncio.run(run_test(url, args.preferred_ip))
