package base.events.block

import base.system.event.Event
import base.system.event.EventBus
import base.system.event.IEventPosting
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction

class BlockEvent(var pos: BlockPos, var facing: Direction) : Event, IEventPosting by Companion {
    companion object: EventBus()
}