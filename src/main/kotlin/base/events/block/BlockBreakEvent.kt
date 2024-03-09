package base.events.block

import base.system.event.Event
import base.system.event.EventBus
import base.system.event.IEventPosting
import net.minecraft.util.math.BlockPos

class BlockBreakEvent(val breakerID: Int, val blockPos: BlockPos, val progress: Int) : Event, IEventPosting by Companion {
    companion object : EventBus()
}