package melon.events.input

import melon.system.event.Event
import melon.system.event.EventBus
import melon.system.event.IEventPosting
import melon.utils.MouseUtil

class MouseClickEvent(
    actionCode: Int,
    val buttonCode: Int
) : Event, IEventPosting by Companion {
    val action = if (actionCode == 0) MouseAction.RELEASE else MouseAction.PRESS
    val button = when (buttonCode) {
        0 -> MouseButton.LEFT
        1 -> MouseButton.RIGHT
        2 -> MouseButton.MIDDLE
        else -> MouseButton.UNKNOWN
    }

    enum class MouseAction {
        PRESS,
        RELEASE
    }

    enum class MouseButton {
        RIGHT,
        LEFT,
        MIDDLE,
        UNKNOWN
    }

    companion object : EventBus()
}