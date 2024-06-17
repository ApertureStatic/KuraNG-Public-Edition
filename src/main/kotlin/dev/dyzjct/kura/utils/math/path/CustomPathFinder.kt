package dev.dyzjct.kura.utils.math.path

import dev.dyzjct.kura.event.eventbus.SafeClientEvent
import base.utils.block.getBlock
import dev.dyzjct.kura.utils.extension.fastFloor
import net.minecraft.block.*
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.pow

class CustomPathFinder(startVec3: Vec3d, endVec3: Vec3d) {
    private val startVec: Vec3d = floor0(addVector(startVec3, 0.0, 0.0, 0.0))
    private val endVec: Vec3d = floor0(addVector(endVec3, 0.0, 0.0, 0.0))
    private var path = CopyOnWriteArrayList<Vec3d?>()
    private val hubs = CopyOnWriteArrayList<Hub>()
    private val hubsToWork = CopyOnWriteArrayList<Hub>()

    fun floor(v: Vec3d): Vec3d {
        return Vec3d(kotlin.math.floor(v.x), kotlin.math.floor(v.y), kotlin.math.floor(v.z))
    }

    fun path(): CopyOnWriteArrayList<Vec3d?> {
        return path
    }

    @JvmOverloads
    fun SafeClientEvent.compute(loops: Int = 1000, depth: Int = 4) {
        path.clear()
        hubsToWork.clear()
        val initPath = CopyOnWriteArrayList<Vec3d?>()
        initPath.add(startVec)
        hubsToWork.add(Hub(startVec, null, initPath, squareDistanceTo(startVec, endVec), 0.0, 0.0))
        loop@ for (i in 0..loops) {
            hubsToWork.sortWith(CompareHub())
            if (hubsToWork.size == 0) break
            for ((j, hub) in CopyOnWriteArrayList<Hub>(hubsToWork).withIndex()) {
                var loc2: Vec3d
                if (j + 1 > depth) break
                hubsToWork.remove(hub)
                hubs.add(hub)
                val n = flatCardinalDirections.size
                var n2 = 0
                while (n2 < n) {
                    val direction = flatCardinalDirections[n2]
                    val loc: Vec3d = floor0(hub.loc.add(direction))
                    if (checkPositionValidity(loc) && addHub(hub, loc, 0.0)) break@loop
                    ++n2
                }
                val loc1: Vec3d = floor0(addVector(hub.loc, 0.0, 1.0, 0.0))
                if (checkPositionValidity(loc1) && addHub(
                        hub,
                        loc1,
                        0.0
                    ) || checkPositionValidity(floor0(addVector(hub.loc, 0.0, -1.0, 0.0)).also {
                        loc2 = it
                    }) && addHub(hub, loc2, 0.0)
                ) break@loop
            }
        }

        hubs.sortWith(CompareHub())
        path = hubs[0].path
    }

    private fun isHubExisting(loc: Vec3d): Hub? {
        for (hub in hubs) {
            if (hub.loc.getX() != loc.getX() || hub.loc.getY() != loc.getY() || hub.loc
                    .getZ() != loc.getZ()
            ) continue
            return hub
        }
        for (hub in hubsToWork) {
            if (hub.loc.getX() != loc.getX() || hub.loc.getY() != loc.getY() || hub.loc
                    .getZ() != loc.getZ()
            ) continue
            return hub
        }
        return null
    }

    private fun addHub(parent: Hub?, loc: Vec3d, cost: Double): Boolean {
        val existingHub = isHubExisting(loc)
        var totalCost = cost
        parent?.let {
            totalCost += parent.totalCost
            existingHub?.let {
                if (existingHub.cost > cost) {
                    val path: CopyOnWriteArrayList<Vec3d?> = CopyOnWriteArrayList<Vec3d?>(parent.path)
                    path.add(loc)
                    existingHub.loc = loc
                    existingHub.parent = parent
                    existingHub.path = path
                    existingHub.squareDistanceToFromTarget = squareDistanceTo(loc, endVec)
                    existingHub.cost = cost
                    existingHub.totalCost = totalCost
                }
            }
            if (existingHub == null) {
                val minDistanceSquared = 9.0
                if ((loc.getX() == endVec.getX() && loc.getY() == endVec.getY()) && loc.getZ() == endVec.getZ() || squareDistanceTo(
                        loc,
                        endVec
                    ) <= minDistanceSquared
                ) {
                    path.clear()
                    path = parent.path
                    path.add(loc)
                    return true
                }
                val path: CopyOnWriteArrayList<Vec3d?> = CopyOnWriteArrayList<Vec3d?>(parent.path)
                path.add(loc)
                hubsToWork.add(Hub(loc, parent, path, squareDistanceTo(loc, endVec), cost, totalCost))
            }
        }
        return false
    }

    class CompareHub : Comparator<Hub> {
        override fun compare(o1: Hub, o2: Hub): Int {
            return (o1.squareDistanceToFromTarget + o1.totalCost - (o2.squareDistanceToFromTarget + o2.totalCost)).toInt()
        }
    }

    class Hub(
        var loc: Vec3d,
        var parent: Hub?,
        var path: CopyOnWriteArrayList<Vec3d?>,
        var squareDistanceToFromTarget: Double,
        var cost: Double,
        var totalCost: Double
    )

    companion object {
        private val flatCardinalDirections = arrayOf(
            Vec3d(1.0, 0.0, 0.0),
            Vec3d(-1.0, 0.0, 0.0),
            Vec3d(0.0, 0.0, 1.0),
            Vec3d(0.0, 0.0, -1.0)
        )

        fun SafeClientEvent.checkPositionValidity(loc: Vec3d): Boolean {
            return checkPositionValidity(loc.getX().toInt(), loc.getY().toInt(), loc.getZ().toInt())
        }

        fun SafeClientEvent.checkPositionValidity(loc: BlockPos): Boolean {
            return checkPositionValidity(loc.x, loc.y, loc.z)
        }

        @JvmStatic
        fun SafeClientEvent.checkPositionValidity(x: Int, y: Int, z: Int): Boolean {
            val block1 = BlockPos(x, y, z)
            val block2 = BlockPos(x, y + 1, z)
            val block3 = BlockPos(x, y - 1, z)
            return !isBlockSolid(block1) &&
                    !isBlockSolid(block2) &&
                    isSafeToWalkOn(block3)
        }

        fun SafeClientEvent.canPassThrow(pos: BlockPos): Boolean {
            val block = world.getBlockState(pos).block
            return block is AirBlock || block is PlantBlock || block is VineBlock || block is LadderBlock || block is FluidBlock || block is SignBlock
        }

        private fun SafeClientEvent.isBlockSolid(block: BlockPos?): Boolean {
            block?.let {
                val blockState: BlockState = world.getBlockState(block)
                val b: Block = world.getBlock(block)

                return (((blockState.isSolidBlock(mc.world, block) && blockState.isFullCube(
                    mc.world,
                    block
                ))) || b is SlabBlock || b is StairsBlock || b is CactusBlock || b is ChestBlock || b is EnderChestBlock || b is SkullBlock || b is PaneBlock ||
                        b is FenceBlock || b is WallBlock || b is StainedGlassBlock || b is TintedGlassBlock || b is PistonBlock || b is PistonExtensionBlock || b is PistonHeadBlock || b is StainedGlassBlock ||
                        b is TrapdoorBlock ||
                        b is BambooBlock || b is BellBlock ||
                        b is CakeBlock || b is RedstoneBlock ||
                        b is LeavesBlock
                        )
            }
            return false
        }

        private fun SafeClientEvent.isSafeToWalkOn(block: BlockPos): Boolean {
            val b = world.getBlock(block)
            return (b !is FenceBlock && b !is WallBlock)
        }

        fun addVector(target: Vec3d, x: Double, y: Double, z: Double): Vec3d {
            return Vec3d(target.x + x, target.y + y, target.z + z)
        }

        fun floor0(vec: Vec3d): Vec3d {
            return Vec3d(
                (vec.x).fastFloor().toDouble(),
                (vec.y).fastFloor().toDouble(),
                (vec.z).fastFloor().toDouble()
            )
        }

        fun squareDistanceTo(target: Vec3d, vec: Vec3d): Double {
            return (target.x - vec.x).pow(2.0) + (target.y - vec.y).pow(2.0) + (target.z - vec.z).pow(2.0)
        }

        fun add(target: Vec3d, v: Vec3d): Vec3d {
            return addVector(target, v.getX(), v.getY(), v.getZ())
        }
    }
}
