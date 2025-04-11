package dev.dyzjct.kura.event.events

import dev.dyzjct.kura.event.eventbus.Event
import dev.dyzjct.kura.event.eventbus.EventBus
import dev.dyzjct.kura.event.eventbus.IEventPosting

class RotateEvent(
    var yawVal: Float,
    var pitchVal: Float,
    var modified: Boolean = false
) : Event, IEventPosting by Companion {
    companion object : EventBus()

    fun setYaw(yaw: Float) {
        modified = true
        this.yawVal = yaw
    }

    fun setPitch(pitch: Float) {
        modified = true
    }

    fun setRotation(yaw: Float, pitch: Float) {
        this.yawVal = yaw
        this.pitchVal = pitch
    }
}