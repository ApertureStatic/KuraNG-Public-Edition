package base.events.player

import base.system.event.*
import net.minecraft.util.math.Vec3d

class PlayerTravelEvent(var movementInput: Vec3d) : Event, ICancellable by Cancellable(), IEventPosting by Companion {
    companion object : EventBus()
}