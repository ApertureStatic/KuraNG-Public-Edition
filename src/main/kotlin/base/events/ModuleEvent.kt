package base.events

import base.system.event.Event
import base.system.event.EventBus
import base.system.event.IEventPosting

sealed class ModuleEvent(val module: dev.dyzjct.kura.module.AbstractModule) : Event {
    class Toggle(module: dev.dyzjct.kura.module.AbstractModule) : ModuleEvent(module), IEventPosting by Companion {
        companion object : EventBus()
    }

    class VisibleChange(module: dev.dyzjct.kura.module.AbstractModule) : ModuleEvent(module),
        IEventPosting by Companion {
        companion object : EventBus()
    }
}