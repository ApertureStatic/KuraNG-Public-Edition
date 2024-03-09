package base.events.input

import base.system.event.Event
import base.system.event.EventBus
import base.system.event.IEventPosting

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