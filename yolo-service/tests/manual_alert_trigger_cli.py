from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from app.services.manual_alert_trigger_service import (
    build_trigger_payload,
    parse_command,
    trigger_many,
)


def _build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="手动溺水告警触发工具（持续输入 camera_id）",
    )
    parser.add_argument(
        "--base-url",
        default="http://127.0.0.1:5000",
        help="AI Service 地址，默认 http://127.0.0.1:5000",
    )
    parser.add_argument("--posture", type=float, default=0.99, help="姿态分")
    parser.add_argument("--thermal", type=float, default=0.99, help="温感分")
    parser.add_argument("--duration", type=float, default=4.2, help="持续时长")
    parser.add_argument("--timeout", type=float, default=10.0, help="请求超时秒")
    return parser


def _print_help() -> None:
    print("命令说明:")
    print("  1) 直接输入摄像头ID，例如: 5001")
    print("  2) trigger camera_id [times] [interval_sec]，例如: trigger 5001 5 2")
    print("  3) help 查看帮助")
    print("  4) q/quit/exit 退出")


def main() -> None:
    args = _build_parser().parse_args()
    print("=== 手动溺水告警触发工具 ===")
    print(f"AI Service: {args.base_url}")
    print(
        f"默认评分: posture={args.posture}, thermal={args.thermal}, duration={args.duration}"
    )
    _print_help()

    while True:
        raw = input("\n请输入命令> ")
        try:
            action, data = parse_command(raw)
        except Exception as exc:
            print(f"[ERROR] 命令无效: {exc}")
            continue

        if action == "exit":
            print("已退出")
            return
        if action == "help":
            _print_help()
            continue

        camera_id = int(data["camera_id"])
        times = int(data["times"])
        interval_sec = float(data["interval_sec"])
        payload = build_trigger_payload(
            camera_id=camera_id,
            posture_score=args.posture,
            thermal_score=args.thermal,
            duration_sec=args.duration,
        )
        print(
            f"[INFO] 触发开始 camera_id={camera_id}, times={times}, interval={interval_sec}s"
        )
        results = trigger_many(
            base_url=args.base_url,
            payload=payload,
            times=times,
            interval_sec=interval_sec,
            timeout_sec=args.timeout,
        )
        success_count = 0
        for index, (ok, body) in enumerate(results, start=1):
            if ok:
                success_count += 1
            status = "OK" if ok else "FAIL"
            print(
                f"  [{index}/{times}] {status}: {json.dumps(body, ensure_ascii=False)}"
            )
        print(f"[DONE] 成功 {success_count}/{times}")


if __name__ == "__main__":
    main()
