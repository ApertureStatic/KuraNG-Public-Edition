package dev.dyzjct.kura.event.events

import dev.dyzjct.kura.event.eventbus.Event
import dev.dyzjct.kura.event.eventbus.EventBus
import dev.dyzjct.kura.event.eventbus.IEventPosting

sealed class ModuleEvent(val module: dev.dyzjct.kura.module.AbstractModule) : Event {
    class Toggle(module: dev.dyzjct.kura.module.AbstractModule) : ModuleEvent(module), IEventPosting by Companion {
        companion object : EventBus()
    }

    class VisibleChange(module: dev.dyzjct.kura.module.AbstractModule) : ModuleEvent(module),
        IEventPosting by Companion {
        companion object : EventBus()
    }
}