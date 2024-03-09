package melon.events.block

import melon.system.event.Event
import melon.system.event.EventBus
import melon.system.event.IEventPosting
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction

class BlockEvent(var pos: BlockPos, var facing: Direction) : Event, IEventPosting by Companion {
    companion object: EventBus()
}