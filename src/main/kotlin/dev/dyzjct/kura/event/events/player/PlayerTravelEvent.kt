package dev.dyzjct.kura.event.events.player

import dev.dyzjct.kura.event.eventbus.*
import net.minecraft.util.math.Vec3d

class PlayerTravelEvent(var movementInput: Vec3d) : Event, ICancellable by Cancellable(), IEventPosting by Companion {
    companion object : EventBus()
}