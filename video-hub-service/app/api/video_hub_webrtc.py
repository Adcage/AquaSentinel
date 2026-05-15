from __future__ import annotations

import logging
import re

from flask import Blueprint, Response, jsonify, request, current_app

from app.common.errors import BusinessError
from app.common.response import success_payload
from app.video_hub import video_hub_registry
from app.video_hub import webrtc_session_manager
from app.video_hub.webrtc_signaling import run_async

logger = logging.getLogger(__name__)

blp = Blueprint("video_hub_webrtc", __name__)


def _filter_sdp_offer(sdp: str) -> str:
    lines = sdp.replace("\r\n", "\n").strip().split("\n")
    video_mids: set[str] = set()
    in_video = False
    for line in lines:
        if line.startswith("m=video "):
            in_video = True
        elif line.startswith("m="):
            in_video = False
        elif line.startswith("a=mid:") and in_video:
            video_mids.add(line[len("a=mid:"):].strip())
    filtered: list[str] = []
    skip = False
    for line in lines:
        if line.startswith("m="):
            skip = not line.startswith("m=video ")
        if skip:
            continue
        if line.startswith("a=group:BUNDLE"):
            parts = line.split()
            mids = [p for p in parts[2:] if p in video_mids]
            if mids:
                filtered.append(f"a=group:BUNDLE {' '.join(mids)}")
            continue
        filtered.append(line)
    return "\r\n".join(filtered) + "\r\n"


def _collect_rejected_sections(sdp: str) -> list[tuple[str, str]]:
    lines = sdp.replace("\r\n", "\n").strip().split("\n")
    rejected: list[tuple[str, str]] = []
    in_non_video = False
    current_mid: str | None = None
    current_m_line: str | None = None
    for line in lines:
        if line.startswith("m="):
            if in_non_video and current_mid is not None and current_m_line is not None:
                rejected.append((current_mid, current_m_line))
            if not line.startswith("m=video "):
                in_non_video = True
                parts = line.split()
                if len(parts) >= 2:
                    parts[1] = "0"
                current_m_line = " ".join(parts)
                current_mid = None
            else:
                in_non_video = False
        elif in_non_video and line.startswith("a=mid:"):
            current_mid = line[len("a=mid:"):].strip()
    if in_non_video and current_mid is not None and current_m_line is not None:
        rejected.append((current_mid, current_m_line))
    return rejected


def _reconstruct_sdp_answer(original_sdp: str, answer_sdp: str) -> str:
    rejected = _collect_rejected_sections(original_sdp)
    if not rejected:
        return answer_sdp
    answer_lines = answer_sdp.replace("\r\n", "\n").strip().split("\n")
    for mid, m_line in rejected:
        answer_lines.append(m_line)
        answer_lines.append(f"a=mid:{mid}")
        answer_lines.append("a=inactive")
    return "\r\n".join(answer_lines) + "\r\n"


def _is_preferred_ipv4(address: str) -> bool:
    if not re.fullmatch(r"\d+\.\d+\.\d+\.\d+", address):
        return False
    if address.startswith("169.254."):
        return False
    if address.startswith("127."):
        return True
    if address.startswith("10."):
        return True
    if address.startswith("192.168."):
        return True
    if address.startswith("172."):
        second = int(address.split(".")[1])
        return 16 <= second <= 31
    return False


def _extract_preferred_offer_ipv4(sdp: str) -> str | None:
    best_address: str | None = None
    best_priority = -1
    for line in _extract_candidate_lines(sdp):
        parts = line.split()
        if len(parts) < 6:
            continue
        if parts[2].lower() != "udp":
            continue
        address = parts[4]
        if not _is_preferred_ipv4(address):
            continue
        try:
            priority = int(parts[3])
        except ValueError:
            continue
        if priority > best_priority:
            best_priority = priority
            best_address = address
    return best_address


def _pin_answer_candidates(answer_sdp: str, preferred_address: str | None, fallback_ip: str | None = None) -> str:
    lines = answer_sdp.replace("\r\n", "\n").strip().split("\n")
    effective_preferred = preferred_address or fallback_ip
    pinned: list[str] = []
    kept_count = 0
    removed_count = 0
    for line in lines:
        if not line.startswith("a=candidate:"):
            pinned.append(line)
            continue
        parts = line.split()
        if len(parts) < 6:
            pinned.append(line)
            continue
        protocol = parts[2].lower()
        address = parts[4]
        if protocol != "udp":
            removed_count += 1
            continue
        if effective_preferred and address == effective_preferred:
            pinned.append(line)
            kept_count += 1
        else:
            removed_count += 1
    logger.info(
        "SDP answer candidates 调整: preferred=%s kept=%d removed=%d",
        effective_preferred,
        kept_count,
        removed_count,
    )
    return "\r\n".join(pinned) + "\r\n"


def _force_setup_passive(sdp: str) -> str:
    lines = sdp.replace("\r\n", "\n").strip().split("\n")
    result: list[str] = []
    changed = False
    for line in lines:
        if line == "a=setup:active":
            result.append("a=setup:passive")
            changed = True
        else:
            result.append(line)
    if changed:
        logger.info("SDP answer a=setup:active -> passive")
    return "\r\n".join(result) + "\r\n"


def _extract_candidate_lines(sdp: str) -> list[str]:
    return [line for line in sdp.replace("\r\n", "\n").split("\n") if line.startswith("a=candidate:")]


@blp.post("/video-hub/cameras/<int:camera_id>/whip")
def whip_offer(camera_id: int):
    original_sdp = request.get_data(as_text=True)
    if not original_sdp.strip():
        raise BusinessError(
            "SDP offer 不能为空",
            status_code=400,
            code="WEBRTC_SDP_EMPTY",
        )
    logger.info("WHIP offer candidates: %s", _extract_candidate_lines(original_sdp))
    preferred_offer_ipv4 = _extract_preferred_offer_ipv4(original_sdp)
    has_localhost_candidate = any(
        " 127.0.0.1 " in line for line in _extract_candidate_lines(original_sdp)
    )
    if has_localhost_candidate:
        preferred_offer_ipv4 = "127.0.0.1"
    logger.info("WHIP offer 首选 IPv4 candidate: %s (localhost=%s)", preferred_offer_ipv4, has_localhost_candidate)

    preferred_ip = str(request.args.get("preferred_ip") or "").strip()
    if not preferred_ip:
        preferred_ip = current_app.config.get("VIDEO_HUB_PREFERRED_IP", "")
    if preferred_ip:
        logger.info("WHIP 使用配置的 preferred_ip: %s", preferred_ip)

    session = video_hub_registry.get_session(camera_id)
    if session is None:
        source_url = str(request.args.get("source_url") or "").strip()
        if not source_url:
            raise BusinessError(
                f"camera_id={camera_id} 视频会话尚未建立，需提供 source_url",
                status_code=503,
                code="WEBRTC_SESSION_ERROR",
            )
        session = video_hub_registry.ensure_session(camera_id, source_url)
    if session.state == "CIRCUIT_OPEN":
        session.activate_from_circuit_open()

    sdp_offer = original_sdp

    try:
        sdp_answer, session_id = run_async(
            webrtc_session_manager.create_whip_session(camera_id, sdp_offer, session, preferred_ip=preferred_ip or None)
        )
    except ValueError as exc:
        if "not in list" not in str(exc):
            raise BusinessError(str(exc), status_code=503, code="WEBRTC_SESSION_ERROR")
        logger.info("原始 SDP offer 不兼容，降级为过滤模式: %s", exc)
        sdp_offer = _filter_sdp_offer(original_sdp)
        try:
            sdp_answer, session_id = run_async(
                webrtc_session_manager.create_whip_session(camera_id, sdp_offer, session, preferred_ip=preferred_ip or None)
            )
        except ValueError as exc2:
            raise BusinessError(str(exc2), status_code=503, code="WEBRTC_SESSION_ERROR")
        except Exception as exc2:
            raise BusinessError(
                f"WebRTC 信令处理失败: {exc2}",
                status_code=400,
                code="WEBRTC_SIGNALING_ERROR",
            )
    except Exception as exc:
        raise BusinessError(
            f"WebRTC 信令处理失败: {exc}",
            status_code=400,
            code="WEBRTC_SIGNALING_ERROR",
        )

    sdp_answer = _reconstruct_sdp_answer(original_sdp, sdp_answer)
    fallback_ip = preferred_ip if preferred_ip else None
    sdp_answer_raw = sdp_answer
    sdp_answer = _pin_answer_candidates(sdp_answer, preferred_offer_ipv4, fallback_ip)

    logger.info("WHIP SDP answer 原始 (aiortc): %s", sdp_answer_raw.replace("\r\n", "\\r\\n"))
    logger.info("WHIP SDP answer 最终 (发送给浏览器): %s", sdp_answer.replace("\r\n", "\\r\\n"))

    offer_m_count = original_sdp.count("\nm=") + original_sdp.count("\r\nm=")
    answer_m_count = sdp_answer.count("\nm=") + sdp_answer.count("\r\nm=")
    logger.info(
        "WHIP 成功 camera_id=%d session=%s offer_m=%d answer_m=%d",
        camera_id,
        session_id[:8],
        offer_m_count,
        answer_m_count,
    )

    response = Response(sdp_answer, status=201, content_type="application/sdp")
    response.headers["Location"] = f"/video-hub/sessions/{session_id}"
    return response


@blp.delete("/video-hub/sessions/<string:session_id>")
def delete_whip_session(session_id: str):
    run_async(webrtc_session_manager.delete_whip_session(session_id))
    return jsonify(success_payload({"session_id": session_id}))
