package dev.dyzjct.kura.module.modules.crystal

import dev.dyzjct.kura.module.modules.crystal.AutoCrystal.getPlaceSide
import base.system.event.SafeClientEvent
import base.utils.Wrapper
import base.utils.world.getHitVecOffset
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.entity.EntityType
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.LivingEntity
import net.minecraft.item.ItemStack
import net.minecraft.util.Arm
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import base.utils.math.vector.Vec3f

open class PlaceInfo(
    open val target: LivingEntity,
    open val blockPos: BlockPos,
    open val selfDamage: Float,
    open val targetDamage: Double,
    open val side: Direction,
    open val hitVecOffset: Vec3f,
    open val hitVec: Vec3d,
) {
    class Mutable(
        target: LivingEntity
    ) : PlaceInfo(
        target,
        BlockPos.ORIGIN,
        Float.MAX_VALUE,
        AutoCrystal.forcePlaceDmg.value,
        Direction.UP,
        Vec3f.ZERO,
        Vec3d.ZERO
    ) {
        override var target = target; private set
        override var blockPos = super.blockPos; private set
        override var selfDamage = super.selfDamage; private set
        override var targetDamage = super.targetDamage; private set
        override var side = super.side; private set
        override var hitVecOffset = super.hitVecOffset; private set
        override var hitVec = super.hitVec; private set

        fun update(
            target: LivingEntity? = null,
            blockPos: BlockPos,
            selfDamage: Double,
            targetDamage: Double,
        ) {
            target?.let {
                this.target = it
            }
            this.blockPos = blockPos
            this.selfDamage = selfDamage.toFloat()
            this.targetDamage = targetDamage
        }

        fun calcPlacement(event: SafeClientEvent) {
            event {
                side = getPlaceSide(blockPos)
                hitVecOffset = getHitVecOffset(side)
                hitVec = Vec3d(
                    (blockPos.x + hitVecOffset.x).toDouble(),
                    (blockPos.y + hitVecOffset.y).toDouble(), (blockPos.z + hitVecOffset.z).toDouble()
                )
            }
        }

        fun clear(player: ClientPlayerEntity) {
            update(player, BlockPos.ORIGIN, Double.MAX_VALUE, AutoCrystal.forcePlaceDmg.value)
        }

        fun takeValid(): Mutable? {
            return this.takeIf {
                target != Wrapper.player
                        && selfDamage != Float.MAX_VALUE
                        && targetDamage != AutoCrystal.forcePlaceDmg.value
            }
        }
    }

    companion object {
        @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        @JvmField
        val INVALID = PlaceInfo(object : LivingEntity(EntityType.PLAYER, null) {
            override fun getArmorItems(): MutableIterable<ItemStack> {
                return ArrayList()
            }

            override fun equipStack(slot: EquipmentSlot, stack: ItemStack) {
            }

            override fun getEquippedStack(slot: EquipmentSlot): ItemStack {
                return ItemStack.EMPTY
            }

            override fun getMainArm(): Arm {
                return Arm.RIGHT
            }
        }, BlockPos.ORIGIN, Float.NaN, Double.NaN, Direction.UP, Vec3f.ZERO, Vec3d.ZERO)
    }
}