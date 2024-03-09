package base.events

import base.system.event.Event
import base.system.event.EventBus
import base.system.event.IEventPosting
import net.minecraft.block.BlockState
import net.minecraft.util.math.BlockPos

sealed class WorldEvent : Event {
    internal object Unload : WorldEvent(), IEventPosting by EventBus()
    internal object Load : WorldEvent(), IEventPosting by EventBus()

    sealed class Entity(val entity: net.minecraft.entity.Entity) : WorldEvent() {
        class Add(entity: net.minecraft.entity.Entity) : Entity(entity), IEventPosting by Companion {
            companion object : EventBus()
        }

        class Remove(entity: net.minecraft.entity.Entity) : Entity(entity), IEventPosting by Companion {
            companion object : EventBus()
        }
    }

    class ServerBlockUpdate(
        val pos: BlockPos,
        val oldState: BlockState,
        val newState: BlockState
    ) : WorldEvent(), IEventPosting by Companion {
        companion object : EventBus()
    }

    class ClientBlockUpdate(
        val pos: BlockPos,
        val oldState: BlockState,
        val newState: BlockState
    ) : WorldEvent(), IEventPosting by Companion {
        companion object : EventBus()
    }

    class RenderUpdate(
        val blockPos: BlockPos
    ) : WorldEvent(), IEventPosting by Companion {
        companion object : EventBus()
    }
}