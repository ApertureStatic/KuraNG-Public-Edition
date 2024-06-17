package dev.dyzjct.kura.event.events.block

import dev.dyzjct.kura.event.eventbus.Event
import dev.dyzjct.kura.event.eventbus.EventBus
import dev.dyzjct.kura.event.eventbus.IEventPosting
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction

class BlockEvent(var pos: BlockPos, var facing: Direction) : Event, IEventPosting by Companion {
    companion object: EventBus()
}