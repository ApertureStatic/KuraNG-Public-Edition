package base.utils.combat

import dev.dyzjct.kura.manager.EntityManager
import dev.dyzjct.kura.module.modules.crystal.CrystalHelper.setAndAdd
import dev.dyzjct.kura.utils.animations.fastFloor
import dev.dyzjct.kura.utils.extension.sq
import dev.dyzjct.kura.event.eventbus.SafeClientEvent
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.decoration.EndCrystalEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import java.util.function.Predicate
import kotlin.math.abs

object CrystalUtils {
    val EndCrystalEntity.blockPos: BlockPos
        get() = BlockPos(this.pos.x.fastFloor(), this.pos.y.fastFloor() - 1, this.pos.z.fastFloor())

    fun getSphere(loc: BlockPos, r: Double, h: Double, hollow: Boolean, sphere: Boolean, yOffset: Int): List<BlockPos> {
        val circleblocks: MutableList<BlockPos> = ArrayList()
        val cx = loc.x
        val cy = loc.y
        val cz = loc.z
        val startX = cx - r.toInt()
        val endX = cx + r.toInt()
        val startZ = cz - r.toInt()
        val endZ = cz + r.toInt()
        val startY = if (sphere) cy - r.toInt() else cy
        val endY = if (sphere) cy + r.toInt() else cy + h.toInt()
        val rSquared = r.sq
        val rMinusOneSquared = (r - 1).sq
        for (x in startX..endX) {
            for (z in startZ..endZ) {
                for (y in startY until endY) {
                    val dist = ((cx - x).sq + (cz - z).sq + if (sphere) (cy - y).sq else 0).toDouble()
                    if (!(dist < rSquared && !(hollow && dist < rMinusOneSquared))) continue
                    val l = BlockPos(x, y + yOffset, z)
                    circleblocks.add(l)
                }
            }
        }
        return circleblocks
    }

    fun getSphereVec(loc: Vec3d, r: Double, h: Double, hollow: Boolean, sphere: Boolean, yOffset: Int): List<BlockPos> {
        val circleblocks: MutableList<BlockPos> = ArrayList()
        val cx = loc.x.fastFloor()
        val cy = loc.y.fastFloor()
        val cz = loc.z.fastFloor()
        val startX = cx - r.toInt()
        val endX = cx + r.toInt()
        val startZ = cz - r.toInt()
        val endZ = cz + r.toInt()
        val startY = if (sphere) cy - r.toInt() else cy
        val endY = if (sphere) cy + r.toInt() else cy + h.toInt()
        val rSquared = r.sq
        val rMinusOneSquared = (r - 1).sq
        for (x in startX..endX) {
            for (z in startZ..endZ) {
                for (y in startY until endY) {
                    val dist = ((cx - x).sq + (cz - z).sq + if (sphere) (cy - y).sq else 0).toDouble()
                    if (!(dist < rSquared && !(hollow && dist < rMinusOneSquared))) continue
                    val l = BlockPos(x, y + yOffset, z)
                    circleblocks.add(l)
                }
            }
        }
        return circleblocks
    }

    private val mutableBlockPos = ThreadLocal.withInitial {
        BlockPos.Mutable()
    }

    /** Checks colliding with blocks and given entity */
    fun SafeClientEvent.canPlaceCrystal(pos: BlockPos): Boolean {
        var entity: Entity? = null
        for (target in world.entities) {
            if (target is EndCrystalEntity) continue
            entity = target
        }
        return canPlaceCrystalOn(pos) && (entity == null || !getCrystalPlacingBB(pos).intersects(entity.boundingBox)) && hasValidSpaceForCrystal(
            pos
        )
    }

    fun SafeClientEvent.canPlaceCrystal(pos: BlockPos, entity: LivingEntity? = null): Boolean {
        return canPlaceCrystalOn(pos) && (entity == null || !getCrystalPlacingBB(pos).intersects(entity.boundingBox)) && hasValidSpaceForCrystal(
            pos
        )
    }

    /** Checks if the block is valid for placing crystal */
    fun SafeClientEvent.canPlaceCrystalOn(pos: BlockPos): Boolean {
        val block = world.getBlockState(pos).block
        return block == Blocks.BEDROCK || block == Blocks.OBSIDIAN
    }

    fun SafeClientEvent.hasValidSpaceForCrystal(pos: BlockPos): Boolean {
        val mutableBlockPos = mutableBlockPos.get()
        return isValidMaterial(
            world.getBlockState(
                mutableBlockPos.setAndAdd(
                    pos, 0, 1, 0
                )
            )
        ) && isValidMaterial(world.getBlockState(mutableBlockPos.add(0, 1, 0)))
    }

    fun isValidMaterial(blockState: BlockState): Boolean {
        return !blockState.isLiquid && blockState.isReplaceable
    }

    fun getCrystalPlacingBB(pos: BlockPos): Box {
        return getCrystalPlacingBB(pos.x, pos.y, pos.z)
    }

    fun getCrystalPlacingBB(x: Int, y: Int, z: Int): Box {
        return Box(
            x + 0.001, y + 1.0, z + 0.001, x + 0.999, y + 3.0, z + 0.999
        )
    }

    fun getCrystalPlacingBB(pos: Vec3d): Box {
        return getCrystalPlacingBB(pos.x, pos.y, pos.z)
    }

    fun getCrystalPlacingBB(x: Double, y: Double, z: Double): Box {
        return Box(
            x - 0.499, y, z - 0.499, x + 0.499, y + 2.0, z + 0.499
        )
    }

    fun getCrystalBB(pos: BlockPos): Box {
        return getCrystalBB(pos.x, pos.y, pos.z)
    }

    fun getCrystalBB(x: Int, y: Int, z: Int): Box {
        return Box(
            x - 0.5, y + 1.0, z - 0.5, x + 1.5, y + 3.0, z + 1.5
        )
    }

    fun getCrystalBB(pos: Vec3d): Box {
        return getCrystalBB(pos.x, pos.y, pos.z)
    }

    fun getCrystalBB(x: Double, y: Double, z: Double): Box {
        return Box(
            x - 1.0, y, z - 1.0, x + 1.0, y + 2.0, z + 1.0
        )
    }

    fun crystalPlaceBoxIntersectsCrystalBox(placePos: BlockPos, crystalPos: Vec3d): Boolean {
        return crystalPlaceBoxIntersectsCrystalBox(placePos, crystalPos.x, crystalPos.y, crystalPos.z)
    }

    fun crystalPlaceBoxIntersectsEntityBox(placePos: BlockPos, crystal: Entity): Boolean {
        return crystalPlaceBoxIntersectsCrystalBox(placePos, crystal.x, crystal.y, crystal.z)
    }

    fun crystalPlaceBoxIntersectsCrystalBox(placePos: BlockPos, crystal: EndCrystalEntity): Boolean {
        return crystalPlaceBoxIntersectsCrystalBox(placePos, crystal.x, crystal.y, crystal.z)
    }

    fun crystalPlaceBoxIntersectsCrystalBox(
        placePos: BlockPos, crystalX: Double, crystalY: Double, crystalZ: Double
    ): Boolean {
        return (crystalY.fastFloor() - placePos.y).withIn(0, 2) && (crystalX.fastFloor() - placePos.y).withIn(
            -1, 1
        ) && (crystalZ.fastFloor() - placePos.y).withIn(-1, 1)
    }


    fun placeBoxIntersectsCrystalBox(placePos: BlockPos, crystal: EndCrystalEntity): Boolean {
        return placeBoxIntersectsCrystalBox(placePos, crystal.x, crystal.y, crystal.z)
    }

    fun placeBoxIntersectsCrystalBox(
        placePos: BlockPos, crystalX: Double, crystalY: Double, crystalZ: Double
    ): Boolean {
        return (crystalY.fastFloor() - placePos.y).withIn(0, 1) && (crystalX.fastFloor() - placePos.x).withIn(
            -1, 1
        ) && (crystalZ.fastFloor() - placePos.z).withIn(-1, 1)
    }


    fun crystalIntersects(crystal1: BlockPos, crystal2: BlockPos): Boolean {
        return crystalIntersects(
            crystal1.x, crystal1.y, crystal1.z, crystal2.x, crystal2.y, crystal2.z
        )
    }

    fun crystalIntersects(crystal1: BlockPos, crystal2: Vec3d): Boolean {
        return crystalIntersects(crystal1.x, crystal1.y, crystal1.z, crystal2.x, crystal2.y, crystal2.z)
    }

    fun crystalIntersects(crystal1: Vec3d, crystal2: BlockPos): Boolean {
        return crystalIntersects(crystal2.x, crystal2.y, crystal2.z, crystal1.x, crystal1.y, crystal1.z)
    }

    fun crystalIntersects(
        crystal1X: Int, crystal1Y: Int, crystal1Z: Int, crystal2X: Int, crystal2Y: Int, crystal2Z: Int
    ): Boolean {
        return abs(crystal2Y - crystal1Y) < 2 && abs(crystal2X - crystal1X) < 2 && abs(crystal2Z - crystal1Z) < 2
    }

    fun crystalIntersects(crystal1: EndCrystalEntity, crystal2: BlockPos): Boolean {
        return crystalIntersects(crystal2.x, crystal2.y, crystal2.z, crystal1.x, crystal1.y, crystal1.z)
    }

    fun crystalIntersects(
        crystal1X: Int, crystal1Y: Int, crystal1Z: Int, crystal2X: Double, crystal2Y: Double, crystal2Z: Double
    ): Boolean {
        return abs(crystal2Y - (crystal1Y + 1)) < 2.0 && abs(crystal2X - (crystal1X + 0.5)) < 2.0 && abs(crystal2Z - (crystal1Z + 0.5)) < 2.0
    }

    fun crystalIntersects(crystal1: Vec3d, crystal2: Vec3d): Boolean {
        return crystalIntersects(crystal1.x, crystal1.y, crystal1.z, crystal2.x, crystal2.y, crystal2.z)
    }

    fun crystalIntersects(
        crystal1X: Double, crystal1Y: Double, crystal1Z: Double, crystal2X: Double, crystal2Y: Double, crystal2Z: Double
    ): Boolean {
        return abs(crystal2Y - crystal1Y) < 2.0 && abs(crystal2X - crystal1X) < 2.0 && abs(crystal2Z - crystal1Z) < 2.0
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun Double.withIn(a: Double, b: Double): Boolean {
        return this > a && this < b
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun Int.withIn(a: Int, b: Int): Boolean {
        return this >= a && this <= b
    }

    /** Checks colliding with All Entities */
    fun placeCollideCheck(pos: BlockPos): Boolean {
        val placingBB = getCrystalPlacingBB(pos)
        return EntityManager.entity.asSequence().filter { it.isAlive }.filter { it.boundingBox.intersects(placingBB) }
            .none()
    }

    fun placeCollideCheck(pos: BlockPos, predicate: Predicate<Entity>): Boolean {
        val placingBB = getCrystalPlacingBB(pos)
        return EntityManager.entity.asSequence().filter { it.isAlive }.filter { it.boundingBox.intersects(placingBB) }
            .filterNot { predicate.test(it) }.none()
    }

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS", "DEPRECATION")
    fun isResistant(blockState: BlockState) = !blockState.isLiquid && blockState.block.blastResistance >= 600
}