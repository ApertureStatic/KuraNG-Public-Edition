package dev.dyzjct.kura.event.events.player

import dev.dyzjct.kura.event.eventbus.*
import net.minecraft.util.math.Vec3d

class UpdateVelocityEvent(
    val movementInput: Vec3d,
    val speed:Float,
    var velocity :Vec3d = Vec3d(0.0, 0.0, 0.0),
) :Event,ICancellable by Cancellable(), IEventPosting by Companion{
    companion object : EventBus()
}