package melon.events.screen

import melon.system.event.Event
import melon.system.event.EventBus
import melon.system.event.IEventPosting
import net.minecraft.client.gui.screen.Screen

sealed class GuiScreenEvent(val screen: Screen) : Event {
    class Display(screen: Screen) : GuiScreenEvent(screen), IEventPosting by Companion {
        companion object : EventBus()
    }

    class Displayed(screen: Screen) : GuiScreenEvent(screen), IEventPosting by Companion {
        companion object : EventBus()
    }

    class Close(screen: Screen) : GuiScreenEvent(screen), IEventPosting by Companion {
        companion object : EventBus()
    }
}