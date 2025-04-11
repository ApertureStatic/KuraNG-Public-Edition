package dev.dyzjct.kura.event.events

import dev.dyzjct.kura.event.eventbus.Event
import dev.dyzjct.kura.event.eventbus.EventBus
import dev.dyzjct.kura.event.eventbus.IEventPosting

class MovementPacketsEvent(
    var yaw: Float,
    var pitch: Float
): Event,
    IEventPosting by Companion
{
    companion object : EventBus()
}