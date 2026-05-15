from __future__ import annotations

import argparse
import os

from app import create_app

app = create_app()


def _parse_args(argv: list[str] | None = None):
    parser = argparse.ArgumentParser()
    parser.add_argument("--dev", action="store_true", help="启用开发模式")
    return parser.parse_args(argv)


def run(argv: list[str] | None = None):
    args = _parse_args(argv)
    port = int(os.environ.get("VIDEO_HUB_PORT", "5100"))
    app.run(host="0.0.0.0", port=port, debug=args.dev, use_reloader=args.dev)


if __name__ == "__main__":
    run()
