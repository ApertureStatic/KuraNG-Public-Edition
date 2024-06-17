package dev.dyzjct.kura.event.events.block

import dev.dyzjct.kura.event.eventbus.Event
import dev.dyzjct.kura.event.eventbus.EventBus
import dev.dyzjct.kura.event.eventbus.IEventPosting
import net.minecraft.util.math.BlockPos

class BlockBreakEvent(val breakerID: Int, val blockPos: BlockPos, val progress: Int) : Event, IEventPosting by Companion {
    companion object : EventBus()
}