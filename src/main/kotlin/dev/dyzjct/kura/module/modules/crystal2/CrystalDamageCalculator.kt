package dev.dyzjct.kura.module.modules.crystal2

import dev.dyzjct.kura.utils.animations.fastFloor
import base.system.event.AlwaysListening
import base.system.event.SafeClientEvent
import base.utils.combat.CrystalUtils
import base.utils.world.FastRayTraceAction
import base.utils.world.fastRaytrace
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.entity.DamageUtil
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ArmorItem
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import net.minecraft.world.Difficulty
import net.minecraft.world.World
import java.util.*
import kotlin.math.max
import kotlin.math.min

object CrystalDamageCalculator : AlwaysListening {
    val reductionMap: MutableMap<LivingEntity, DamageReduction> = Collections.synchronizedMap(WeakHashMap())

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
                        when (AutoCrystal2.damageMode.value) {
                            AutoCrystal2.DamageMode.Auto -> {
                                if (id == 0) {
                                    // Protection
                                    epf += level
                                } else if (id == 3) {
                                    // Blast protection
                                    epf += level * 2
                                }
                            }

                            AutoCrystal2.DamageMode.PPBP -> {
                                if (itemStack.item is ArmorItem) {
                                    epf += if ((itemStack.item as ArmorItem).type == ArmorItem.Type.LEGGINGS) {
                                        4 * 2
                                    } else {
                                        4
                                    }
                                }
                            }

                            AutoCrystal2.DamageMode.BBBB -> {
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
                    ), AutoCrystal2.crystalPriority
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
        if ((blockState.block != Blocks.AIR || AutoCrystal2.crystalPriority == AutoCrystal2.Priority.Block) && isResistant(
                blockState,
                AutoCrystal2.crystalPriority
            )
        ) {
            FastRayTraceAction.CALC
        } else {
            FastRayTraceAction.SKIP
        }
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
    fun isResistant(blockState: BlockState, prio: AutoCrystal2.Priority = AutoCrystal2.Priority.Crystal) =
        prio == AutoCrystal2.Priority.Block || (!blockState.isLiquid && blockState.block.blastResistance >= 19.7)
}