package dev.dyzjct.kura.manager

import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import base.events.ConnectionEvent
import base.events.PacketEvents
import base.events.RunGameLoopEvent
import base.events.WorldEvent
import base.system.event.AlwaysListening
import base.system.event.listener
import base.system.event.safeEventListener
import net.minecraft.block.BlockState
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket
import net.minecraft.util.math.BlockPos
import java.util.function.Function

object WorldManager : AlwaysListening {
    private val lock = Any()
    private val oldBlockStateMap = Object2ObjectOpenHashMap<BlockPos, BlockState>()
    private val newBlockStateMap = Object2ObjectOpenHashMap<BlockPos, BlockState>()
    private val timeoutMap = Object2LongOpenHashMap<BlockPos>()
    private val pendingPos = ObjectArrayList<BlockPos>()
    private val pendingOldState = ObjectArrayList<BlockState>()
    private val pendingNewState = ObjectArrayList<BlockState>()

    fun onInit() {
        listener<ConnectionEvent.Disconnect> {
            synchronized(lock) {
                oldBlockStateMap.clear()
                oldBlockStateMap.trim()
                newBlockStateMap.clear()
                newBlockStateMap.trim()
                timeoutMap.clear()
                timeoutMap.trim()
            }

            pendingPos.clear()
            pendingPos.trim()
            pendingNewState.clear()
            pendingNewState.trim()
        }

        safeEventListener<PacketEvents.Receive> { event ->
            if (event.packet !is BlockUpdateS2CPacket) return@safeEventListener
            synchronized(lock) {
                event.packet.pos?.let { pos ->
                    oldBlockStateMap.computeIfAbsent(pos,
                        Function {
                            world.getBlockState(it)
                        })
                    newBlockStateMap[pos] = event.packet.state
                    timeoutMap[pos] = System.currentTimeMillis() + 5L
                }
            }
        }

        listener<RunGameLoopEvent.Tick> {
            synchronized(lock) {
                val iterator = timeoutMap.object2LongEntrySet().fastIterator()
                val current = System.currentTimeMillis()
                while (iterator.hasNext()) {
                    val entry = iterator.next()
                    if (current > entry.longValue) {
                        val oldState = oldBlockStateMap.remove(entry.key)
                        val newState = newBlockStateMap.remove(entry.key)
                        oldState ?: continue
                        newState ?: continue
                        pendingPos.add(entry.key)
                        pendingOldState.add(oldState)
                        pendingNewState.add(newState)
                        iterator.remove()
                    }
                }
            }

            for (i in pendingPos.indices) {
                val pos = pendingPos[i]
                val oldState = pendingOldState[i]
                val newState = pendingNewState[i]
                WorldEvent.ServerBlockUpdate(pos, oldState, newState).post()
            }

            pendingPos.clear()
            pendingNewState.clear()
        }
    }
}