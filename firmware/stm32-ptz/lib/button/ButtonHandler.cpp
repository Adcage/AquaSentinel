#include "ButtonHandler.h"

#include "config.h"

void ButtonHandler::begin() {
    reset();
}

ButtonEvent ButtonHandler::update(bool rawPressed, uint32_t nowMs) {
    if (rawPressed != lastRawPressed) {
        lastRawPressed = rawPressed;
        lastBounceMs = nowMs;
    }

    if ((nowMs - lastBounceMs) < ptz_config::BUTTON_DEBOUNCE_MS) {
        return ButtonEvent::None;
    }

    if (stablePressed != rawPressed) {
        stablePressed = rawPressed;
        if (stablePressed) {
            pressStartMs = nowMs;
            longPressFired = false;
        } else if (!longPressFired) {
            return ButtonEvent::ShortPress;
        }
    }

    if (stablePressed && !longPressFired && (nowMs - pressStartMs) >= ptz_config::BUTTON_LONG_PRESS_MS) {
        longPressFired = true;
        return ButtonEvent::LongPress;
    }

    return ButtonEvent::None;
}

void ButtonHandler::reset() {
    stablePressed = false;
    lastRawPressed = false;
    longPressFired = false;
    lastBounceMs = 0;
    pressStartMs = 0;
}
