package dev.dyzjct.kura.module.modules.crystal

import base.utils.Wrapper.minecraft
import base.utils.block.getBlock
import base.utils.combat.CrystalUtils
import base.utils.world.FastRayTraceAction
import base.utils.world.fastRaytrace
import dev.dyzjct.kura.event.eventbus.AlwaysListening
import dev.dyzjct.kura.event.eventbus.SafeClientEvent
import dev.dyzjct.kura.mixins.IExplosion
import dev.dyzjct.kura.utils.animations.fastFloor
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.entity.DamageUtil
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ArmorItem
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import net.minecraft.world.Difficulty
import net.minecraft.world.World
import net.minecraft.world.explosion.Explosion
import java.util.*
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

object CrystalDamageCalculator : AlwaysListening {
    val reductionMap: MutableMap<LivingEntity, DamageReduction> = Collections.synchronizedMap(WeakHashMap())

    private var explosion =
        Explosion(minecraft.world, null, 0.0, 0.0, 0.0, 6f, false, Explosion.DestructionType.DESTROY)

    class DamageReduction(var entity: LivingEntity) {
        private val armorValue: Float = entity.armor.toFloat()
        private val toughness =
            entity.getAttributeInstance(EntityAttributes.GENERIC_ARMOR_TOUGHNESS)?.value?.toFloat() ?: 1f
        private val resistance: Float
        private val blastReduction: Float

        init {
            val potionEffect = entity.getStatusEffect(StatusEffects.RESISTANCE)
            resistance = if (potionEffect != null) max(1.0f - (potionEffect.amplifier + 1) * 0.2f, 0.0f) else 1.0f
            blastReduction = 1.0f - min(calcTotalEPF(entity), 20) / 25.0f
        }

        fun calcReductionDamage(damage: Float): Float {
            return DamageUtil.getDamageLeft(damage, armorValue, toughness) *
                    resistance *
                    blastReduction
        }

        companion object {
            private fun calcTotalEPF(entity: LivingEntity): Int {
                var epf = 0
                for (itemStack in entity.armorItems) {
                    val nbtTagList = itemStack.enchantments
                    for (i in 0 until nbtTagList.size) {
                        val nbtTagCompound = nbtTagList.getCompound(i)
                        val id = nbtTagCompound.getInt("id")
                        val level = nbtTagCompound.getShort("lvl").toInt()
                        when (AutoCrystal.damageMode.value) {
                            AutoCrystal.DamageMode.Auto -> {
                                if (id == 0) {
                                    // Protection
                                    epf += level
                                } else if (id == 3) {
                                    // Blast protection
                                    epf += level * 2
                                }
                            }

                            AutoCrystal.DamageMode.PPBP -> {
                                if (itemStack.item is ArmorItem) {
                                    epf += if ((itemStack.item as ArmorItem).type == ArmorItem.Type.LEGGINGS) {
                                        4 * 2
                                    } else {
                                        4
                                    }
                                }
                            }

                            AutoCrystal.DamageMode.BBBB -> {
                                epf += 4 * 2
                            }
                        }
                    }
                }
                return epf
            }
        }
    }

    private const val DOUBLE_SIZE = 12.0f
    private const val DAMAGE_FACTOR = 42.0f

    fun SafeClientEvent.calcDamage(
        entity: LivingEntity,
        entityPos: Vec3d,
        entityBox: Box,
        crystalX: Double,
        crystalY: Double,
        crystalZ: Double,
        mutableBlockPos: BlockPos.Mutable,
        isCrystal: Boolean = false
    ): Float {
        val isPlayer = entity is PlayerEntity
        if (isPlayer && world.difficulty == Difficulty.PEACEFUL) return 0.0f
        var damage: Float

        damage = if (isPlayer
            && crystalY - entityPos.y > 1.5652173822904127
            && if (isCrystal) {
                isResistant(
                    world.getBlockState(
                        mutableBlockPos.set(
                            crystalX.fastFloor(),
                            crystalY.fastFloor() - 1,
                            crystalZ.fastFloor()
                        )
                    )
                )
            } else {
                CrystalUtils.isResistant(
                    world.getBlockState(
                        mutableBlockPos.set(
                            crystalX.fastFloor(),
                            crystalY.fastFloor() - 1,
                            crystalZ.fastFloor()
                        )
                    )
                )
            }
        ) {
            1.0f
        } else {
            calcRawDamage(entityPos, entityBox, crystalX, crystalY, crystalZ, mutableBlockPos)
        }

        if (isPlayer) damage = calcDifficultyDamage(world, damage)
        return calcReductionDamage(entity, damage, Vec3d(crystalX, crystalY, crystalZ))
    }

    private fun SafeClientEvent.calcRawDamage(
        entityPos: Vec3d,
        entityBox: Box,
        posX: Double,
        posY: Double,
        posZ: Double,
        mutableBlockPos: BlockPos.Mutable
    ): Float {
        val scaledDist = entityPos.distanceTo(Vec3d(posX, posY, posZ)).toFloat() / DOUBLE_SIZE
        if (scaledDist > 1.0f) return 0.0f

        val factor = (1.0f - scaledDist) * getExposureAmount(entityBox, posX, posY, posZ, mutableBlockPos)
        return ((factor * factor + factor) * DAMAGE_FACTOR + 1.0f)
    }

    private val function: World.(BlockPos, BlockState) -> FastRayTraceAction = { _, blockState ->
        FastRayTraceAction.SKIP
    }

    private fun SafeClientEvent.getExposureAmount(
        entityBox: Box,
        posX: Double,
        posY: Double,
        posZ: Double,
        mutableBlockPos: BlockPos.Mutable
    ): Float {
        val width = entityBox.maxX - entityBox.minX
        val height = entityBox.maxY - entityBox.minY

        val gridMultiplierXZ = 1.0 / (width * 2.0 + 1.0)
        val gridMultiplierY = 1.0 / (height * 2.0 + 1.0)

        val gridXZ = width * gridMultiplierXZ
        val gridY = height * gridMultiplierY

        val sizeXZ = (1.0 / gridMultiplierXZ).fastFloor()
        val sizeY = (1.0 / gridMultiplierY).fastFloor()
        val xzOffset = (1.0 - gridMultiplierXZ * (sizeXZ)) / 2.0

        var total = 0
        var count = 0

        for (yIndex in 0..sizeY) {
            for (xIndex in 0..sizeXZ) {
                for (zIndex in 0..sizeXZ) {
                    val x = gridXZ * xIndex + xzOffset + entityBox.minX
                    val y = gridY * yIndex + entityBox.minY
                    val z = gridXZ * zIndex + xzOffset + entityBox.minZ

                    total++
                    if (!world.fastRaytrace(x, y, z, posX, posY, posZ, 20, mutableBlockPos, function)) {
                        count++
                    }
                }
            }
        }

        return count.toFloat() / total.toFloat()
    }

    private fun calcReductionDamage(entity: LivingEntity, damage: Float, vec3d: Vec3d): Float {
        val reduction = reductionMap[entity]
        return reduction?.calcReductionDamage(damage) ?: damage
    }

    private fun calcDifficultyDamage(world: World, damage: Float): Float {
        return when (world.difficulty) {
            Difficulty.PEACEFUL -> 0.0f
            Difficulty.EASY -> min(damage * 0.5f + 1.0f, damage)
            Difficulty.HARD -> damage * 1.5f
            else -> damage
        }
    }

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS", "DEPRECATION")
    fun isResistant(blockState: BlockState) = !blockState.isLiquid && blockState.block.blastResistance >= 19.7

    fun SafeClientEvent.anchorDamageNew(pos: BlockPos, target: PlayerEntity): Float {
        if (world.getBlock(pos) === Blocks.RESPAWN_ANCHOR) {
            val oldState = world.getBlockState(pos)
            world.setBlockState(pos, Blocks.AIR.defaultState)
            val damage = calculateDamage(pos.toCenterPos(), target, 5f)
            world.setBlockState(pos, oldState)
            return damage
        } else {
            return calculateDamage(pos.toCenterPos(), target, 5f)
        }
    }

    fun SafeClientEvent.calculateDamage(
        explosionPos: Vec3d,
        target: PlayerEntity,
        power: Float
    ): Float {
        var endDamage = 0f
        if (world.difficulty === Difficulty.PEACEFUL) return endDamage
        (explosion as IExplosion)[explosionPos, power] = true

        if (!Box(
                MathHelper.floor(explosionPos.x - 11.0).toDouble(),
                MathHelper.floor(explosionPos.y - 11.0).toDouble(),
                MathHelper.floor(explosionPos.z - 11.0).toDouble(),
                MathHelper.floor(explosionPos.x + 13.0).toDouble(),
                MathHelper.floor(explosionPos.y + 13.0).toDouble(),
                MathHelper.floor(explosionPos.z + 13.0).toDouble()
            ).intersects(target.boundingBox)
        ) {
            return endDamage
        }

        if (!target.isImmuneToExplosion(explosion) && !target.isInvulnerable) {
            val distExposure = MathHelper.sqrt(target.squaredDistanceTo(explosionPos).toFloat()) / 12.0
            if (distExposure <= 1.0) {
                val xDiff = target.x - explosionPos.x
                val yDiff = target.y - explosionPos.y
                val zDiff = target.x - explosionPos.z
                val diff = MathHelper.sqrt((xDiff * xDiff + yDiff * yDiff + zDiff * zDiff).toFloat()).toDouble()
                if (diff != 0.0) {
                    val exposure = Explosion.getExposure(explosionPos, target).toDouble()
                    val finalExposure = (1.0 - distExposure) * exposure

                    var toDamage = floor((finalExposure * finalExposure + finalExposure) / 2.0 * 7.0 * 12.0 + 1.0)
                        .toFloat()

                    if (world.difficulty === Difficulty.EASY) {
                        toDamage = min((toDamage / 2f + 1f).toDouble(), toDamage.toDouble()).toFloat()
                    } else if (world.difficulty === Difficulty.HARD) {
                        toDamage = toDamage * 3f / 2f
                    }

                    toDamage = DamageUtil.getDamageLeft(
                        toDamage,
                        target.armor.toFloat(),
                        (target.getAttributeInstance(EntityAttributes.GENERIC_ARMOR_TOUGHNESS)?.value
                            ?: 1.0).toFloat()
                    )

                    if (target.hasStatusEffect(StatusEffects.RESISTANCE)) {
                        val resistance =
                            25 - ((target.getStatusEffect(StatusEffects.RESISTANCE)?.amplifier ?: 0) + 1) * 5
                        val resistance1 = toDamage * resistance
                        toDamage = max((resistance1 / 25f).toDouble(), 0.0).toFloat()
                    }

                    if (toDamage <= 0f) {
                        toDamage = 0f
                    } else {
                        val protAmount = EnchantmentHelper.getProtectionAmount(
                            target.armorItems,
                            (explosion as IExplosion).damageSource
                        )
                        if (protAmount > 0) {
                            toDamage = DamageUtil.getInflictedDamage(toDamage, protAmount.toFloat())
                        }
                    }
                    endDamage = toDamage
                }
            }
        }
        return endDamage
    }
}