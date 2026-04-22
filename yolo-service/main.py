import argparse

from app import create_app

app = create_app({"AUTO_CREATE_TABLES": False})


def _parse_args(argv: list[str] | None = None):
    parser = argparse.ArgumentParser()
    parser.add_argument("--dev", action="store_true", help="Enable development mode")
    return parser.parse_args(argv)


def run(argv: list[str] | None = None):
    args = _parse_args(argv)
    app.run(host="0.0.0.0", port=5000, debug=args.dev, use_reloader=args.dev)


if __name__ == "__main__":
    run()
