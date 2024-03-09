package dev.dyzjct.kura.manager

import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import it.unimi.dsi.fastutil.longs.LongSet
import it.unimi.dsi.fastutil.longs.LongSets
import kotlinx.coroutines.launch
import melon.events.RunGameLoopEvent
import melon.events.WorldEvent
import melon.system.event.*
import melon.utils.TickTimer
import melon.utils.concurrent.threads.defaultScope
import melon.utils.concurrent.threads.runSafe
import melon.utils.hole.HoleInfo
import melon.utils.hole.HoleUtils.checkHoleM
import melon.utils.hole.SurroundUtils.betterPosition
import melon.utils.hole.SurroundUtils.flooredPosition
import net.minecraft.entity.Entity
import net.minecraft.util.math.BlockPos
import team.exception.melon.util.math.distanceSqTo
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Predicate

object HoleManager : AlwaysListening {
    private val holeMap0 = ConcurrentHashMap<BlockPos, HoleInfo>()
    private var holeSet: LongSet = LongSets.EMPTY_SET
    val holeMap: Map<BlockPos, HoleInfo> get() = holeMap0
    var holeInfos = emptyList<HoleInfo>(); private set

    private val mainTimer = TickTimer()
    private val updateTimer = TickTimer()
    private val removeTimer = TickTimer()

    private var dirty = false

    private const val RANGE = 16
    private const val RANGE_SQ = 256
    private const val MAX_RANGE_SQ = 1024

    fun onInit() {
        listener<WorldEvent.Unload> {
            holeMap0.clear()
            holeInfos = emptyList()
            dirty = false
        }

        safeEventListener<WorldEvent.RenderUpdate> {
            defaultScope.launch {
                val playerPos = player.flooredPosition
                val mutablePos = BlockPos.Mutable()

                val sequence = sequence {
                    for (x in it.blockPos.x + 2 downTo it.blockPos.x - 1) {
                        for (y in it.blockPos.y + 1 downTo it.blockPos.y - 2) {
                            for (z in it.blockPos.z + 2 downTo it.blockPos.z - 1) {
                                if (playerPos.distanceSqTo(x, y, z) > RANGE_SQ) continue
                                yield(mutablePos.set(x, y, z))
                            }
                        }
                    }
                }

                updatePosSequence(sequence)
            }
        }

        safeEventListener<WorldEvent.ClientBlockUpdate> {
            defaultScope.launch {
                val playerPos = player.flooredPosition
                val mutablePos = BlockPos.Mutable()

                val sequence = sequence {
                    for (x in it.pos.x + 2 downTo it.pos.x - 1) {
                        for (y in it.pos.y + 1 downTo it.pos.y - 2) {
                            for (z in it.pos.z + 2 downTo it.pos.z - 1) {
                                if (playerPos.distanceSqTo(x, y, z) > RANGE_SQ) continue
                                yield(mutablePos.set(x, y, z))
                            }
                        }
                    }
                }

                updatePosSequence(sequence)
            }
        }

        safeConcurrentListener<RunGameLoopEvent.Render> {
            if (mainTimer.tickAndReset(100L)) {
                defaultScope.launch {
                    if (removeTimer.tickAndReset(500L)) {
                        removeInvalidPos()
                    }

                    updatePos(updateTimer.tickAndReset(1000L))
                }
            }

            val flag: Boolean

            synchronized(HoleManager) {
                flag = dirty
                dirty = false
            }

            if (flag) {
                updateHoleInfoList()
            }
        }
    }

    fun getHoleBelow(
        pos: BlockPos,
        yRange: Int
    ) = getHoleBelow(pos, yRange) { true }

    fun getHoleBelow(pos: BlockPos, yRange: Int, predicate: Predicate<HoleInfo>): HoleInfo? {
        for (yOffset in -64..yRange) {
            val offset = pos.down(yOffset)
            val info = getHoleInfo(offset)
            if (info.isHole && predicate.test(info)) return info
        }

        return null
    }

    fun getHoleInfo(entity: Entity) =
        getHoleInfo(entity.betterPosition)

    fun getHoleInfo(pos: BlockPos) =
        holeMap0.computeIfAbsent(pos) {
            runSafe {
                val holeInfo = checkHoleM(it)

                holeInfo
            } ?: HoleInfo.empty(it)
        }

    private fun SafeClientEvent.removeInvalidPos() {
        val playerPos = player.flooredPosition
        var modified = false

        val iterator = holeMap0.keys.iterator()
        while (iterator.hasNext()) {
            val pos = iterator.next()
            if (playerPos.distanceSqTo(pos) > MAX_RANGE_SQ) {
                iterator.remove()
                modified = true
            }
        }

        if (modified) {
            synchronized(HoleManager) {
                dirty = true
            }
        }
    }

    private fun SafeClientEvent.updatePos(force: Boolean) {
        val playerPos = player.flooredPosition
        val checked = LongOpenHashSet()
        val mutablePos = BlockPos.Mutable()

        var modified = false

        for (x in RANGE downTo -RANGE) {
            for (y in RANGE downTo -RANGE) {
                for (z in RANGE downTo -RANGE) {
                    mutablePos.set(playerPos.x + x, playerPos.y + y, playerPos.z + z)
                    if (mutablePos.y !in -64..320) continue
                    if (!force && holeSet.contains(mutablePos.asLong())) continue
                    modified = updatePos(playerPos, checked, mutablePos) || modified
                }
            }
        }

        if (modified) {
            synchronized(HoleManager) {
                dirty = true
            }
        }
    }

    private fun SafeClientEvent.updatePosSequence(sequence: Sequence<BlockPos.Mutable>) {
        val playerPos = player.flooredPosition
        val checked = LongOpenHashSet()

        var modified = false

        sequence.forEach {
            modified = updatePos(playerPos, checked, it) || modified
        }

        if (modified) {
            synchronized(HoleManager) {
                dirty = true
            }
        }
    }

    private fun SafeClientEvent.updatePos(playerPos: BlockPos, checked: LongSet, pos: BlockPos.Mutable): Boolean {
        val long = pos.asLong()
        if (checked.contains(long)) return false
        if (pos.getSquaredDistance(playerPos) > RANGE_SQ) return false

        val holeInfo = checkHoleM(pos)
        return if (!holeInfo.isHole) {
            val prev = holeMap0.put(holeInfo.origin, holeInfo)
            checked.add(long)
            prev == null || prev.isHole
        } else {
            var modified = false

            for (holePos in holeInfo.holePos) {
                val prev = holeMap0.put(holePos, holeInfo)
                checked.add(holePos.asLong())

                modified = modified || (prev == null || !prev.isHole)
            }

            modified
        }
    }

    private fun updateHoleInfoList() {
        holeSet = LongOpenHashSet(holeMap0.size).apply {
            holeMap0.keys.forEach {
                add(it.asLong())
            }
        }

        holeInfos = holeMap0.values.asSequence()
            .filter { it.isHole }
            .distinct()
            .toList()
    }
}