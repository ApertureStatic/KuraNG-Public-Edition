package dev.dyzjct.kura.module.modules.crystal

import dev.dyzjct.kura.event.eventbus.AlwaysListening
import dev.dyzjct.kura.event.eventbus.SafeClientEvent
import dev.dyzjct.kura.utils.block.BlockUtil.canSee
import base.utils.combat.CrystalUtils
import base.utils.math.distanceSqTo
import base.utils.math.toBlockPos
import base.utils.world.noCollision
import dev.dyzjct.kura.manager.CombatManager
import dev.dyzjct.kura.manager.EntityManager
import dev.dyzjct.kura.utils.animations.fastFloor
import dev.dyzjct.kura.utils.animations.sq
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.client.MinecraftClient
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.decoration.EndCrystalEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sqrt

object CrystalHelper : AlwaysListening {
    val mc = MinecraftClient.getInstance()
    val EndCrystalEntity.blockPos: BlockPos
        get() = BlockPos(this.pos.x.fastFloor(), this.pos.y.fastFloor() - 1, this.pos.z.fastFloor())

    val LivingEntity.scaledHealth: Float
        get() = this.health + this.absorptionAmount * (this.health / this.maxHealth)

    val LivingEntity.totalHealth: Float
        get() = this.health + this.absorptionAmount

    @JvmStatic
    val LivingEntity.totalHealthStatic: Float
        get() = this.health + this.absorptionAmount

    fun calcCollidingCrystalDamageOld(
        crystals: List<Pair<EndCrystalEntity, CrystalDamage>>,
        placeBox: Box
    ): Float {
        var max = 0.0f

        for ((crystal, crystalDamage) in crystals) {
            if (!placeBox.intersects(crystal.boundingBox)) continue
            if (crystalDamage.selfDamage > max) {
                max = crystalDamage.selfDamage
            }
        }

        return max
    }

    fun SafeClientEvent.calcCollidingCrystalDamage(
        placeBox: Box
    ): Float {
        var max = 0.0f
        if (!world.entities.none()) {
            for (c in world.entities) {
                if (c == null) continue
                if (c !is EndCrystalEntity) continue
                if (player.distanceSqTo(c.pos) > 6.sq) continue
                val mutableBlockPos = BlockPos.Mutable()
                if (!placeBox.intersects(c.boundingBox)) continue
                val context = CombatManager.contextSelf ?: return 0f
                val crystalX = c.x
                val crystalY = c.y
                val crystalZ = c.z
                val selfDamage = max(
                    context.calcDamage(crystalX, crystalY, crystalZ, false, mutableBlockPos),
                    context.calcDamage(crystalX, crystalY, crystalZ, true, mutableBlockPos)
                ).toDouble()

                if (selfDamage > max) {
                    max = selfDamage.toFloat()
                }
            }
        }
        return max
    }

    @JvmStatic
    fun SafeClientEvent.checkBreakRange(
        entity: EndCrystalEntity,
        breakRange: Float,
        wallCheck: Boolean = false,
        wallRange: Double = 6.0
    ): Boolean {
        return checkBreakRange(
            entity.x,
            entity.y,
            entity.z,
            breakRange,
            wallCheck,
            wallRange
        )
    }

    @JvmStatic
    fun SafeClientEvent.checkBreakRange(
        x: Double,
        y: Double,
        z: Double,
        breakRange: Float,
        wallCheck: Boolean = false,
        wallRange: Double = 6.0
    ): Boolean {
        return player.distanceSqTo(x, y, z) <= breakRange.sq && (canSee(x, y, z) || player.distanceSqTo(
            x,
            y,
            z
        ) <= wallRange.sq || !wallCheck)
    }

    @JvmStatic
    fun SafeClientEvent.isPlaceable(pos: BlockPos, newPlacement: Boolean, mutableBlockPos: BlockPos.Mutable): Boolean {
        if (!canPlaceCrystalOn(pos)) {
            return false
        }
        val posUp = mutableBlockPos.setAndAdd(pos, 0, 1, 0)
        return if (newPlacement) {
            world.isAir(posUp)
        } else {
            isValidMaterial(world.getBlockState(posUp)) && isValidMaterial(
                world.getBlockState(
                    posUp.add(
                        0,
                        1,
                        0
                    )
                )
            )
        }
    }

    @JvmStatic
    fun BlockPos.Mutable.setAndAdd(set: BlockPos, add: BlockPos): BlockPos.Mutable {
        return this.set(set.x + add.x, set.y + add.y, set.z + add.z)
    }

    @JvmStatic
    fun BlockPos.Mutable.setAndAdd(set: BlockPos, x: Int, y: Int, z: Int): BlockPos.Mutable {
        return this.set(set.x + x, set.y + y, set.z + z)
    }

    @JvmStatic
    private val mutableBlockPos = ThreadLocal.withInitial {
        BlockPos.Mutable()
    }

    @JvmStatic
            /** Checks colliding with blocks and given entity */
    fun SafeClientEvent.canPlaceCrystalNew(pos: BlockPos, entity: LivingEntity? = null): Boolean {
        return canPlaceCrystalOn(pos)
                && (entity == null || !getCrystalPlacingBB(pos).intersects(entity.boundingBox))
                && hasValidSpaceForCrystal(pos)
    }

    @JvmStatic
            /** Checks if the block is valid for placing crystal */
    fun SafeClientEvent.canPlaceCrystalOn(pos: BlockPos): Boolean {
        val block = world.getBlockState(pos).block
        return block == Blocks.BEDROCK || block == Blocks.OBSIDIAN
    }

    @JvmStatic
    fun SafeClientEvent.hasValidSpaceForCrystal(pos: BlockPos): Boolean {
        val mutableBlockPos = mutableBlockPos.get()
        return isValidMaterial(world.getBlockState(mutableBlockPos.setAndAdd(pos, 0, 1, 0)))
                && isValidMaterial(world.getBlockState(mutableBlockPos.add(0, 1, 0)))
    }

    @JvmStatic
    fun SafeClientEvent.isValidMaterial(blockState: BlockState): Boolean {
        return !blockState.isLiquid && blockState.isReplaceable
    }

    @JvmStatic
    fun SafeClientEvent.isReplaceable(block: Block): Boolean {
        return block === Blocks.FIRE || block === Blocks.VINE
    }

    @JvmStatic
    fun SafeClientEvent.getVecDistance(a: BlockPos, x: Double, y: Double, z: Double): Double {
        val x1 = a.x - x
        val y1 = a.y - y
        val z1 = a.z - z
        return sqrt(x1 * x1 + y1 * y1 + z1 * z1)
    }

    @JvmStatic
    fun SafeClientEvent.getVecDistance(pos: BlockPos, entity: Entity): Double {
        return getVecDistance(pos, entity.x, entity.y, entity.z)
    }

    inline val Entity.realSpeed get() = hypot(x - prevX, z - prevZ)

    @JvmStatic
    fun SafeClientEvent.shouldForcePlace(entity: LivingEntity, forcePlaceHealth: Float): Boolean {
        return (entity.health + entity.absorptionAmount) <= forcePlaceHealth
    }

    @JvmStatic
    fun SafeClientEvent.normalizeAngle(angleIn: Double): Double {
        var angle = angleIn
        angle %= 360.0
        if (angle >= 180.0) {
            angle -= 360.0
        }
        if (angle < -180.0) {
            angle += 360.0
        }
        return angle
    }

    @JvmStatic
    fun normalizeAngle(angleIn: Float): Float {
        var angle = angleIn
        angle %= 360f
        if (angle >= 180f) {
            angle -= 360f
        }
        if (angle < -180f) {
            angle += 360f
        }
        return angle
    }

    fun placeBoxIntersectsCrystalBox(placePos: BlockPos, crystal: EndCrystalEntity): Boolean {
        return placeBoxIntersectsCrystalBox(placePos, crystal.x, crystal.y, crystal.z)
    }

    fun placeBoxIntersectsCrystalBox(
        placePos: BlockPos,
        crystalX: Double,
        crystalY: Double,
        crystalZ: Double
    ): Boolean {
        return (crystalY.fastFloor() - placePos.y).withIn(0, 1)
                && (crystalX.fastFloor() - placePos.x).withIn(-1, 1)
                && (crystalZ.fastFloor() - placePos.z).withIn(-1, 1)
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun Int.withIn(a: Int, b: Int): Boolean {
        return this in a..b
    }

    @JvmStatic
    fun placeBoxIntersectsCrystalBox(placePos: BlockPos, crystalPos: BlockPos): Boolean {
        return crystalPos.y - placePos.y in 0..2
                && abs(crystalPos.x - placePos.x) < 2
                && abs(crystalPos.z - placePos.z) < 2
    }

    @JvmStatic
    fun placeBoxIntersectsCrystalBox(placePos: Vec3d, crystalPos: BlockPos): Boolean {
        return crystalPos.y - placePos.y in 0.0..2.0
                && abs(crystalPos.x - placePos.x) < 2.0
                && abs(crystalPos.z - placePos.z) < 2.0
    }

    @JvmStatic
    fun placeBoxIntersectsCrystalBox(
        placeX: Double,
        placeY: Double,
        placeZ: Double,
        crystalPos: BlockPos
    ): Boolean {
        return crystalPos.y - placeY in 0.0..2.0
                && abs(crystalPos.x - placeX) < 2.0
                && abs(crystalPos.z - placeZ) < 2.0
    }

    @JvmStatic
    fun placeBoxIntersectsCrystalBox(
        placeX: Double,
        placeY: Double,
        placeZ: Double,
        crystalX: Double,
        crystalY: Double,
        crystalZ: Double
    ): Boolean {
        return crystalY - placeY in 0.0..2.0
                && abs(crystalX - placeX) < 2.0
                && abs(crystalZ - placeZ) < 2.0
    }

    @JvmStatic
    fun SafeClientEvent.getCrystalPlacingBB(pos: BlockPos): Box {
        return getCrystalPlacingBB(pos.x, pos.y, pos.z)
    }

    @JvmStatic
    fun getCrystalPlacingBB(x: Int, y: Int, z: Int): Box {
        return Box(
            x + 0.001, y + 1.0, z + 0.001,
            x + 0.999, y + 3.0, z + 0.999
        )
    }

    @JvmStatic
    fun SafeClientEvent.getCrystalPlacingBB(pos: Vec3d): Box {
        return getCrystalPlacingBB(pos.x, pos.y, pos.z)
    }

    @JvmStatic
    fun SafeClientEvent.getCrystalPlacingBB(x: Double, y: Double, z: Double): Box {
        return Box(
            x - 0.499, y, z - 0.499,
            x + 0.499, y + 2.0, z + 0.499
        )
    }

    @JvmStatic
    fun getCrystalBB(pos: BlockPos): Box {
        return getCrystalBB(pos.x, pos.y, pos.z)
    }

    @JvmStatic
    fun getCrystalBB(x: Int, y: Int, z: Int): Box {
        return Box(
            x - 0.5, y + 1.0, z - 0.5,
            x + 1.5, y + 3.0, z + 1.5
        )
    }

    fun checkPlaceCollision(placeInfo: BlockPos): Boolean {
        return EntityManager.entity.asSequence()
            .filterIsInstance<EndCrystalEntity>()
            .filter { it.isAlive }
            .filter { CrystalUtils.crystalPlaceBoxIntersectsCrystalBox(placeInfo, it) }
            .none()
    }

    fun SafeClientEvent.getPredictedTarget(target: Entity, ticks: Int, ignoreBox: Boolean = false): Vec3d {
        val motionX = (target.x - target.lastRenderX).coerceIn(-0.6, 0.6)
        val motionY = (target.y - target.lastRenderY).coerceIn(-0.5, 0.5)
        val motionZ = (target.z - target.lastRenderZ).coerceIn(-0.6, 0.6)
        val entityBox = target.boundingBox
        var targetBox = entityBox
        for (tick in 0..ticks) {
            targetBox = if (!ignoreBox) {
                canMove(targetBox, motionX, motionY, motionZ)
                    ?: canMove(targetBox, motionX, 0.0, motionZ)
                    ?: canMove(targetBox, 0.0, motionY, 0.0)
                    ?: break
            } else {
                targetBox.offset(motionX, motionY, motionZ)
            }
        }
        val offsetX = targetBox.minX - entityBox.minX
        val offsetY = targetBox.minY - entityBox.minY
        val offsetZ = targetBox.minZ - entityBox.minZ
        return if (ticks > 0) {
            Vec3d(offsetX, offsetY, offsetZ)
        } else {
            target.pos
            //Vec3d(motionX, motionY, motionZ)
        }
    }

    fun SafeClientEvent.getPredictedPos(target: Entity, ticks: Int, ignoreBox: Boolean = false): Vec3d {
        val motionX = (target.x - target.lastRenderX).coerceIn(-0.6, 0.6)
        val motionY = (target.y - target.lastRenderY).coerceIn(-0.5, 0.5)
        val motionZ = (target.z - target.lastRenderZ).coerceIn(-0.6, 0.6)
        val entityBox = target.boundingBox
        var targetBox = entityBox
        for (tick in 0..ticks) {
            targetBox = if (!ignoreBox) {
                canMove(targetBox, motionX, motionY, motionZ)
                    ?: canMove(targetBox, motionX, 0.0, motionZ)
                    ?: canMove(targetBox, 0.0, motionY, 0.0)
                    ?: break
            } else {
                targetBox.offset(motionX, motionY, motionZ)
            }
        }
        val offsetX = targetBox.minX - entityBox.minX
        val offsetY = targetBox.minY - entityBox.minY
        val offsetZ = targetBox.minZ - entityBox.minZ
        val motion = Vec3d(offsetX, offsetY, offsetZ)
        val pos = target.pos
        return (ticks > 0).let { pos.add(motion) } ?: pos
    }

    fun SafeClientEvent.canMove(box: Box, x: Double, y: Double, z: Double): Box? {
        synchronized(this) {
            runCatching {
                return box.offset(x, y, z).takeIf { world.noCollision(it.center.toBlockPos()) }
            }
            return null
        }
    }
}