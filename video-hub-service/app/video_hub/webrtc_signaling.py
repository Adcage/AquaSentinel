from __future__ import annotations

import asyncio
import threading

_loop: asyncio.AbstractEventLoop | None = None
_lock = threading.Lock()


def get_webrtc_event_loop() -> asyncio.AbstractEventLoop:
    global _loop
    with _lock:
        if _loop is not None and not _loop.is_closed():
            return _loop
        _loop = asyncio.new_event_loop()
        thread = threading.Thread(target=_loop.run_forever, daemon=True)
        thread.start()
        return _loop


def run_async(coro):
    loop = get_webrtc_event_loop()
    future = asyncio.run_coroutine_threadsafe(coro, loop)
    return future.result()
