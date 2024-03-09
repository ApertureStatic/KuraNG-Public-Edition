package melon.events

import dev.dyzjct.kura.module.AbstractModule
import melon.system.event.Event
import melon.system.event.EventBus
import melon.system.event.IEventPosting

sealed class ModuleEvent(val module: dev.dyzjct.kura.module.AbstractModule) : Event {
    class Toggle(module: dev.dyzjct.kura.module.AbstractModule) : ModuleEvent(module), IEventPosting by Companion {
        companion object : EventBus()
    }

    class VisibleChange(module: dev.dyzjct.kura.module.AbstractModule) : ModuleEvent(module),
        IEventPosting by Companion {
        companion object : EventBus()
    }
}