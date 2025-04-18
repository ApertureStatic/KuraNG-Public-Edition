package dev.dyzjct.kura.module.modules.aura

import dev.dyzjct.kura.event.eventbus.AlwaysListening
import dev.dyzjct.kura.event.eventbus.SafeClientEvent
import dev.dyzjct.kura.event.eventbus.safeEventListener
import dev.dyzjct.kura.event.events.player.PlayerMoveEvent
import dev.dyzjct.kura.mixins.IExplosion
import dev.dyzjct.kura.mixins.IRaycastContext
import dev.dyzjct.kura.mixins.IVec3d
import dev.dyzjct.kura.utils.extension.sq
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.entity.DamageUtil
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.*
import net.minecraft.util.shape.VoxelShapes
import net.minecraft.world.BlockView
import net.minecraft.world.Difficulty
import net.minecraft.world.RaycastContext
import net.minecraft.world.explosion.Explosion
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

object DamageCalculator : AlwaysListening {

    private var vec3d = Vec3d(0.0, 0.0, 0.0)
    lateinit var explosion: Explosion
    lateinit var raycastContext: RaycastContext
    private lateinit var bedRaycast: RaycastContext
    private var needInit = true

    init {
        safeEventListener<PlayerMoveEvent>(114514, true) {
            if (needInit) {
                explosion = Explosion(world, null, 0.0, 0.0, 0.0, 6f, false, Explosion.DestructionType.DESTROY)
                raycastContext =
                    RaycastContext(
                        null,
                        null,
                        RaycastContext.ShapeType.COLLIDER,
                        RaycastContext.FluidHandling.ANY,
                        player
                    )
                bedRaycast =
                    RaycastContext(
                        null,
                        null,
                        RaycastContext.ShapeType.COLLIDER,
                        RaycastContext.FluidHandling.ANY,
                        player
                    )
                needInit = false
            }
        }
    }

    fun init() {}

    // Anchor damage
    fun SafeClientEvent.anchorDamage(
        player: LivingEntity,
        targetPos: Vec3d,
        box: Box,
        anchor: BlockPos
    ): Double {
        if (player is PlayerEntity && player.abilities.creativeMode) return 0.0
        val modDistance = targetPos.distanceTo(anchor.toCenterPos()).sq
        if (modDistance > 10) return 0.0
        val exposure = getExposure(anchor.toCenterPos(), player, box, raycastContext, anchor, true)
        val impact = (1.0 - modDistance / 10.0) * exposure
        var damage = (impact * impact + impact) / 2 * 7 * (5 * 2) + 1

        // Multiply damage by difficulty
        damage = getDamageForDifficulty(damage)

        // Reduce by resistance
        damage = resistanceReduction(player, damage)

        // Reduce by armour
        damage = DamageUtil.getDamageLeft(
            damage.toFloat(),
            player.armor.toFloat(),
            player.getAttributeInstance(EntityAttributes.GENERIC_ARMOR_TOUGHNESS)!!
                .value.toFloat()
        ).toDouble()
        val attribute = player.getAttributeInstance(EntityAttributes.GENERIC_ARMOR_TOUGHNESS)

        // Reduce by enchants
        (explosion as IExplosion)[anchor.toCenterPos(), 5f] = true
        damage = getDamageForDifficulty(damage)
        damage = resistanceReduction(player, damage)
        damage = DamageUtil.getDamageLeft(
            damage.toFloat(), player.armor.toFloat(),
            (attribute?.value?.toFloat() ?: 0) as Float
        )
            .toDouble()
        damage = blastProtReduction(player, damage, explosion)
        return if (damage < 0) 0.0 else damage
    }


    // Bed damage
    fun SafeClientEvent.bedDamage(
        player: LivingEntity,
        targetPos: Vec3d,
        box: Box,
        bed: Vec3d,
        ignore: BlockPos?
    ): Double {
        if (player is PlayerEntity && player.abilities.creativeMode) return 0.0
        val modDistance = targetPos.distanceTo(bed).sq
        if (modDistance > 10) return 0.0
        val exposure = getExposure(bed, player, box, raycastContext, ignore, true)
        val impact = (1.0 - modDistance / 10.0) * exposure
        var damage = (impact * impact + impact) / 2 * 7 * (5 * 2) + 1

        // Multiply damage by difficulty
        damage = getDamageForDifficulty(damage)

        // Reduce by resistance
        damage = resistanceReduction(player, damage)

        // Reduce by armour
        damage = DamageUtil.getDamageLeft(
            damage.toFloat(),
            player.armor.toFloat(),
            player.getAttributeInstance(EntityAttributes.GENERIC_ARMOR_TOUGHNESS)!!
                .value.toFloat()
        ).toDouble()

        // Reduce by enchants
        (explosion as IExplosion)[bed, 5f] = true
        damage = blastProtReduction(player, damage, explosion)
        if (damage < 0) damage = 0.0
        return damage
    }


    // Crystal damage
    fun SafeClientEvent.crystalDamage(
        target: LivingEntity,
        targetPos: Vec3d,
        targetBox: Box,
        crystal: BlockPos
    ): Double {
//        return anchorDamage(target, targetPos, targetBox, crystal)

        if (world.difficulty == Difficulty.PEACEFUL) return 0.0
        if (target is PlayerEntity && target.abilities.creativeMode) return 0.0

        val cry = crystal.toCenterPos()

        val explosion = Explosion(
            world,
            null,
            cry.x,
            cry.y,
            cry.z,
            6f,
            false,
            Explosion.DestructionType.DESTROY
        )
        val maxDist = 12.0
        if (!Box(
                floor(cry.x - maxDist - 1.0),
                floor(cry.y - maxDist - 1.0),
                floor(cry.z - maxDist - 1.0),
                floor(cry.x + maxDist + 1.0),
                floor(cry.y + maxDist + 1.0),
                floor(cry.z + maxDist + 1.0)
            ).intersects(targetBox)
        ) {
            return 0.0
        }
        if (!target.isImmuneToExplosion(explosion) && !target.isInvulnerable) {
            val distExposure = MathHelper.sqrt(target.squaredDistanceTo(cry).toFloat()) / maxDist
            if (distExposure <= 1.0) {
                val xDiff = targetPos.x - cry.x
                val yDiff = targetPos.y - cry.y
                val zDiff = targetPos.x - cry.z
                val diff = MathHelper.sqrt((xDiff * xDiff + yDiff * yDiff + zDiff * zDiff).toFloat()).toDouble()
                if (diff != 0.0) {
                    val exposure = Explosion.getExposure(cry, target).toDouble()
                    val finalExposure = (1.0 - distExposure) * exposure
                    var toDamage =
                        floor((finalExposure * finalExposure + finalExposure) / 2.0 * 7.0 * maxDist + 1.0)
                    if (world.difficulty == Difficulty.EASY) {
                        toDamage = min((toDamage / 2f + 1f), toDamage)
                    } else if (world.difficulty == Difficulty.HARD) {
                        toDamage = toDamage * 3f / 2f
                    }
                    toDamage = DamageUtil.getDamageLeft(
                        toDamage.toFloat(),
                        target.armor.toFloat(),
                        target.getAttributeInstance(EntityAttributes.GENERIC_ARMOR_TOUGHNESS)!!
                            .value.toFloat()
                    ).toDouble()
                    if (target.hasStatusEffect(StatusEffects.RESISTANCE)) {
                        val resistance = 25 - (target.getStatusEffect(StatusEffects.RESISTANCE)!!.amplifier + 1) * 5
                        val resistance1 = toDamage * resistance
                        toDamage = max((resistance1 / 25f), 0.0)
                    }
                    if (toDamage <= 0) {
                        toDamage = 0.0
                    } else {
                        val protectAmount =
                            EnchantmentHelper.getProtectionAmount(target.armorItems, explosion.damageSource)
                        if (protectAmount > 0) {
                            toDamage =
                                DamageUtil.getInflictedDamage(toDamage.toFloat(), protectAmount.toFloat()).toDouble()
                        }
                    }
                    return toDamage
                }
            }
        }
        return 0.0
    }


    // Utils
    private fun SafeClientEvent.getDamageForDifficulty(damage: Double): Double {
        return when (world.difficulty) {
            Difficulty.EASY -> (damage / 2 + 1).coerceAtMost(damage)
            Difficulty.HARD,
            Difficulty.PEACEFUL -> damage * 3 / 2

            else -> damage
        }
    }

    private fun SafeClientEvent.blastProtReduction(player: Entity, damage0: Double, explosion: Explosion): Double {
        var damage = damage0
        var protLevel =
            EnchantmentHelper.getProtectionAmount(player.armorItems, world.damageSources.explosion(explosion))
        if (protLevel > 20) protLevel = 20
        damage *= 1 - protLevel / 25.0
        return if (damage < 0) 0.0 else damage
    }

    fun resistanceReduction(player: LivingEntity, damage0: Double): Double {
        var damage = damage0
        if (player.hasStatusEffect(StatusEffects.RESISTANCE)) {
            player.getStatusEffect(StatusEffects.RESISTANCE)?.let {
                val lvl = it.amplifier + 1
                damage *= 1 - lvl * 0.2
            }
        }
        return if (damage < 0) 0.0 else damage
    }

    private fun SafeClientEvent.getExposure(
        source: Vec3d,
        entity: Entity,
        box: Box,
        raycastContext: RaycastContext,
        ignore: BlockPos?,
        ignoreTerrain: Boolean
    ): Double {
        val d = 1 / ((box.maxX - box.minX) * 2 + 1)
        val e = 1 / ((box.maxY - box.minY) * 2 + 1)
        val f = 1 / ((box.maxZ - box.minZ) * 2 + 1)
        val g = (1 - floor(1 / d) * d) / 2
        val h = (1 - floor(1 / f) * f) / 2
        if (!(d < 0) && !(e < 0) && !(f < 0)) {
            var i = 0
            var j = 0
            var k = 0.0
            while (k <= 1) {
                var l = 0.0
                while (l <= 1) {
                    var m = 0.0
                    while (m <= 1) {
                        val n = MathHelper.lerp(k, box.minX, box.maxX)
                        val o = MathHelper.lerp(l, box.minY, box.maxY)
                        val p = MathHelper.lerp(m, box.minZ, box.maxZ)
                        (vec3d as IVec3d)[n + g, o] = p + h
//                        vec3d = Vec3d(n + g, o, p + h)
                        (raycastContext as IRaycastContext)[vec3d, source, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE] =
                            entity
                        if (mraycast(raycastContext, ignore, ignoreTerrain).type == HitResult.Type.MISS) i++
                        j++
                        m += f
                    }
                    l += e
                }
                k += d
            }
            return i.toDouble() / j
        }
        return 0.0
    }

    fun SafeClientEvent.raycast(context: RaycastContext): BlockHitResult {
        return BlockView.raycast(
            context.start, context.end, context,
            { raycastContext: RaycastContext, blockPos: BlockPos ->
                val blockState = world.getBlockState(blockPos)
                val vec3d = raycastContext.start
                val vec3d2 = raycastContext.end
                val voxelShape = raycastContext.getBlockShape(blockState, world, blockPos)
                val blockHitResult = world.raycastBlock(vec3d, vec3d2, blockPos, voxelShape, blockState)
                val voxelShape2 = VoxelShapes.empty()
                val blockHitResult2 = voxelShape2.raycast(vec3d, vec3d2, blockPos)
                val d =
                    if (blockHitResult == null) Double.MAX_VALUE else raycastContext.start
                        .squaredDistanceTo(blockHitResult.pos)
                val e =
                    if (blockHitResult2 == null) Double.MAX_VALUE else raycastContext.start
                        .squaredDistanceTo(blockHitResult2.pos)
                if (d <= e) blockHitResult else blockHitResult2
            }
        ) { raycastContext: RaycastContext ->
            val vec3d = raycastContext.start.subtract(raycastContext.end)
            BlockHitResult.createMissed(
                raycastContext.end,
                Direction.getFacing(vec3d.x, vec3d.y, vec3d.z),
                BlockPos.ofFloored(raycastContext.end)
            )
        }
    }

    private fun SafeClientEvent.mraycast(
        context: RaycastContext,
        ignore: BlockPos?,
        ignoreTerrain: Boolean
    ): BlockHitResult {
        return BlockView.raycast(
            context.start, context.end, context,
            { raycastContext: RaycastContext, blockPos: BlockPos ->
                var blockState: BlockState
                if (blockPos == ignore) blockState = Blocks.AIR.defaultState else {
                    blockState = world.getBlockState(blockPos)
                    if (blockState.block.blastResistance < 600 && ignoreTerrain) blockState =
                        Blocks.AIR.defaultState
                }
                val vec3d = raycastContext.start
                val vec3d2 = raycastContext.end
                val voxelShape = raycastContext.getBlockShape(blockState, world, blockPos)
                val blockHitResult = world.raycastBlock(vec3d, vec3d2, blockPos, voxelShape, blockState)
                val voxelShape2 = VoxelShapes.empty()
                val blockHitResult2 = voxelShape2.raycast(vec3d, vec3d2, blockPos)
                val d =
                    if (blockHitResult == null) Double.MAX_VALUE else raycastContext.start
                        .squaredDistanceTo(blockHitResult.pos)
                val e =
                    if (blockHitResult2 == null) Double.MAX_VALUE else raycastContext.start
                        .squaredDistanceTo(blockHitResult2.pos)
                if (d <= e) blockHitResult else blockHitResult2
            }
        ) { raycastContext: RaycastContext ->
            val vec3d = raycastContext.start.subtract(raycastContext.end)
            BlockHitResult.createMissed(
                raycastContext.end,
                Direction.getFacing(vec3d.x, vec3d.y, vec3d.z),
                BlockPos.ofFloored(
                    raycastContext.end
                )
            )
        }
    }


}