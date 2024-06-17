package dev.dyzjct.kura.event.events.screen

import dev.dyzjct.kura.event.eventbus.Event
import dev.dyzjct.kura.event.eventbus.EventBus
import dev.dyzjct.kura.event.eventbus.IEventPosting
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