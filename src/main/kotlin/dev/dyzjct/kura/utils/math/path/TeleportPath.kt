package dev.dyzjct.kura.utils.math.path

import dev.dyzjct.kura.utils.math.path.CustomPathFinder.Companion.canPassThrow
import dev.dyzjct.kura.utils.math.path.CustomPathFinder.Companion.checkPositionValidity
import dev.dyzjct.kura.event.eventbus.SafeClientEvent
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.max
import kotlin.math.min

object TeleportPath {
    @JvmOverloads
    fun SafeClientEvent.teleport(from: Vec3d, to: Vec3d, back: Boolean = true, task: Runnable? = null) {
        teleportTo(from, to)
        task?.run()
        if (back) teleportTo(to, from)
    }

    fun SafeClientEvent.teleportTo(to: Vec3d, back: Boolean, task: Runnable?) {
        teleport(player.pos, to, back, task)
    }

    fun SafeClientEvent.teleportTo(to: Vec3d) {
        kotlin.runCatching {
            teleport(player.pos, to, false, null)
        }
    }

    fun SafeClientEvent.teleportTo(from: Vec3d, to: Vec3d) {
        computePath(from, to)?.let { path ->
            for (pathElm in path) {
                connection.sendPacket(
                    PlayerMoveC2SPacket.PositionAndOnGround(
                        pathElm.getX(),
                        pathElm.getY(),
                        pathElm.getZ(),
                        true
                    )
                )
            }
            player.updatePosition(to.x, to.y, to.z)
        }
    }

    private fun SafeClientEvent.computePath(from: Vec3d, to: Vec3d): CopyOnWriteArrayList<Vec3d>? {

        var topFrom = from
        if (!canPassThrow(BlockPos.ofFloored(topFrom))) {
            topFrom = CustomPathFinder.addVector(topFrom, 0.0, 1.0, 0.0)
        }

        val pathfinder = CustomPathFinder(topFrom, to)
        with(pathfinder) { compute() }

        runCatching {

        }
        var lastLoc: Vec3d? = null
        var lastDashLoc: Vec3d? = null
        val path = CopyOnWriteArrayList<Vec3d>()
        val pathFinderPath = pathfinder.path()
        if (pathFinderPath.isEmpty()) return null
        runCatching {
            for ((i, pathElm) in pathFinderPath.withIndex()) {
                if (pathElm == null) continue
                if (i == 0 || i == pathFinderPath.size - 1) {
                    if (lastLoc != null) {
                        path.add(CustomPathFinder.addVector(lastLoc!!, 0.5, 0.0, 0.5))
                    }
                    path.add(CustomPathFinder.addVector(pathElm, 0.5, 0.0, 0.5))
                    lastDashLoc = pathElm
                } else {
                    val lastLoc = lastLoc ?: continue
                    var lastDashLoc = lastDashLoc ?: continue
                    var canContinue = true
                    val dashDistance = 5.0
                    if (CustomPathFinder.squareDistanceTo(pathElm, lastDashLoc) > dashDistance * dashDistance) {
                        canContinue = false
                    } else {
                        val smallX = min(lastDashLoc.getX(), pathElm.getX())
                        val smallY = min(lastDashLoc.getY(), pathElm.getY())
                        val smallZ = min(lastDashLoc.getZ(), pathElm.getZ())
                        val bigX = max(lastDashLoc.getX(), pathElm.getX())
                        val bigY = max(lastDashLoc.getY(), pathElm.getY())
                        val bigZ = max(lastDashLoc.getZ(), pathElm.getZ())
                        cordsLoop@ for (x in smallX.toInt()..bigX.toInt()) {
                            for (y in smallY.toInt()..bigY.toInt()) {
                                for (z in smallZ.toInt()..bigZ.toInt()) {
                                    if (!checkPositionValidity(x, y, z)) {
                                        canContinue = false
                                        break@cordsLoop
                                    }
                                }
                            }
                        }
                    }
                    if (!canContinue) {
                        lastLoc.let {
                            runCatching { path.add(CustomPathFinder.addVector(it, 0.5, 0.0, 0.5)) }
                            lastDashLoc = it
                        }
                    }
                }
                lastLoc = pathElm
            }
        }
        return path
    }
}
