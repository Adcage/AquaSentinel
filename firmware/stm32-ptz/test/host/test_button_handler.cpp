#include <cassert>

#include "ButtonHandler.h"

int main() {
    ButtonHandler button;

    ButtonEvent event = button.update(false, 0);
    assert(event == ButtonEvent::None);

    button.update(true, 10);
    button.update(true, 50);
    button.update(false, 120);
    event = button.update(false, 160);
    assert(event == ButtonEvent::ShortPress);

    button.reset();
    button.update(true, 1000);
    button.update(true, 1040);
    event = button.update(true, 1900);
    assert(event == ButtonEvent::LongPress);

    event = button.update(true, 2000);
    assert(event == ButtonEvent::None);

    return 0;
}
