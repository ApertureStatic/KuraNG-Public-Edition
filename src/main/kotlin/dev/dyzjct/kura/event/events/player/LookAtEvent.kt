package dev.dyzjct.kura.event.events.player

import dev.dyzjct.kura.event.eventbus.Event
import dev.dyzjct.kura.event.eventbus.IEventPosting
import dev.dyzjct.kura.event.eventbus.NamedProfilerEventBus
import net.minecraft.util.math.Vec3d

class LookAtEvent : Event, IEventPosting by NamedProfilerEventBus("kuraLookAtEvent") {
    var target: Vec3d? = null
    var yaw: Float? = null
    var pitch: Float? = null
    var rotation: Boolean = false
    var speed: Float? = null
    var priority = 0f

    fun setTarget(target: Vec3d?, speed: Float, priority: Float) {
        if (priority >= this.priority) {
            this.rotation = false
            this.priority = priority
            this.target = target
            this.speed = speed
        }
    }

    fun setRotation(yaw: Float, pitch: Float, speed: Float, priority: Float) {
        if (priority >= this.priority) {
            this.rotation = true
            this.priority = priority
            this.yaw = yaw
            this.pitch = pitch
            this.speed = speed
        }
    }
}