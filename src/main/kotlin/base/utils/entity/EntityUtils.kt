package base.utils.entity

import dev.dyzjct.kura.manager.EntityManager
import dev.dyzjct.kura.manager.FriendManager
import dev.dyzjct.kura.utils.animations.fastFloor
import dev.dyzjct.kura.utils.animations.sq
import base.system.event.SafeClientEvent
import base.system.util.interfaces.MinecraftWrapper
import base.utils.Wrapper
import base.utils.block.BlockUtil.canSeeEntity
import base.utils.concurrent.threads.runSafe
import net.minecraft.block.*
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.entity.Entity
import net.minecraft.entity.ItemEntity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.entity.mob.EndermanEntity
import net.minecraft.entity.mob.ZombieEntity
import net.minecraft.entity.passive.*
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import team.exception.melon.util.math.distanceSqTo
import team.exception.melon.util.math.toBlockPos

object EntityUtils : MinecraftWrapper {
    val SafeClientEvent.baseMoveSpeed: Double
        get() {
            var n = 0.2873
            if (player.hasStatusEffect(StatusEffects.SPEED)) {
                player.getStatusEffect(StatusEffects.SPEED)?.let {
                    n *= 1.0 + 0.2 * (it.amplifier + 1)
                }
            }
            return n
        }

    val SafeClientEvent.jumpSpeed: Double
        get() {
            var jumpSpeed = 0.3999999463558197
            if (player.hasStatusEffect(StatusEffects.JUMP_BOOST)) {
                player.getStatusEffect(StatusEffects.JUMP_BOOST)?.let {
                    val amplifier = it.amplifier.toDouble()
                    jumpSpeed += (amplifier + 1) * 0.1
                }
            }
            return jumpSpeed
        }

    val SafeClientEvent.viewEntity get() = mc.cameraEntity ?: player
    val Entity.eyePosition get() = Vec3d(this.pos.x, this.pos.y + this.getEyeHeight(this.pose), this.pos.z)
    val Entity.lastTickPos get() = Vec3d(this.lastRenderX, this.lastRenderY, this.lastRenderZ)
    val Entity.flooredPosition get() = BlockPos(this.pos.x.fastFloor(), this.pos.y.fastFloor(), this.pos.z.fastFloor())
    val Entity.betterPosition
        get() = BlockPos(
            this.pos.x.fastFloor(),
            (this.pos.y + 0.25).fastFloor(),
            this.pos.z.fastFloor()
        )
    val Entity.isTamed get() = this is TameableEntity && this.isTamed || this is AbstractHorseEntity && this.isTame
    val Entity.isInOrAboveLiquid
        get() = this.isTouchingWater || this.isInLava || world.containsFluid(
            boundingBox.expand(
                0.0,
                -1.0,
                0.0
            )
        )
    val Entity.preventEntitySpawning get() = this !is ItemEntity
    val PlayerEntity.isFriend get() = FriendManager.isFriend(this.name.toString())
    private fun isNeutralMob(entity: Entity) =
        entity is PigEntity || entity is ZombieEntity || entity is WolfEntity || entity is EndermanEntity || entity is IronGolemEntity

    /**
     * Find the entities interpolated position
     */
    fun getInterpolatedPos(entity: Entity, ticks: Float): Vec3d =
        entity.lastTickPos.add(getInterpolatedAmount(entity, ticks))

    /**
     * Find the entities interpolated amount
     */
    fun getInterpolatedAmount(entity: Entity, ticks: Float): Vec3d =
        entity.getPos().subtract(entity.lastTickPos).multiply(ticks.toDouble())

    fun isMoving(): Boolean {
        return if (Wrapper.player == null) false else (Wrapper.player!!.sidewaysSpeed != 0f || Wrapper.player!!.upwardSpeed != 0f || Wrapper.player!!.forwardSpeed != 0f)
    }

    fun SafeClientEvent.getHealth(): Float {
        val livingBase = player as LivingEntity
        return livingBase.health + livingBase.absorptionAmount
    }

    fun SafeClientEvent.getJumpEffect(): Int {
        return player.getStatusEffect(StatusEffects.JUMP_BOOST)?.let {
            it.amplifier + 1
        } ?: 0
    }

    fun autoCenter() {
        runSafe {
            val centerPos = player.blockPos
            val y = centerPos.y.toDouble()
            var x = centerPos.x.toDouble()
            var z = centerPos.z.toDouble()
            val plusPlus = Vec3d(x + 0.5, y, z + 0.5)
            val plusMinus = Vec3d(x + 0.5, y, z - 0.5)
            val minusMinus = Vec3d(x - 0.5, y, z - 0.5)
            val minusPlus = Vec3d(x - 0.5, y, z + 0.5)
            if (getDst(plusPlus) < getDst(
                    plusMinus
                ) && getDst(plusPlus) < getDst(
                    minusMinus
                ) && getDst(plusPlus) < getDst(
                    minusPlus
                )
            ) {
                x = centerPos.x + 0.5
                z = centerPos.z + 0.5
                centerPlayer(x, y, z)
            }
            if (getDst(plusMinus) < getDst(
                    plusPlus
                ) && getDst(plusMinus) < getDst(
                    minusMinus
                ) && getDst(plusMinus) < getDst(
                    minusPlus
                )
            ) {
                x = centerPos.x + 0.5
                z = centerPos.z - 0.5
                centerPlayer(x, y, z)
            }
            if (getDst(minusMinus) < getDst(
                    plusPlus
                ) && getDst(minusMinus) < getDst(
                    plusMinus
                ) && getDst(minusMinus) < getDst(
                    minusPlus
                )
            ) {
                x = centerPos.x - 0.5
                z = centerPos.z - 0.5
                centerPlayer(x, y, z)
            }
            if (getDst(minusPlus) < getDst(
                    plusPlus
                ) && getDst(minusPlus) < getDst(
                    plusMinus
                ) && getDst(minusPlus) < getDst(
                    minusMinus
                )
            ) {
                x = centerPos.x - 0.5
                z = centerPos.z + 0.5
                centerPlayer(x, y, z)
            }
        }
    }

    fun SafeClientEvent.isInBurrow(): Boolean {
        return (isBurrowBlock(player.blockPos) || isBurrowBlock(
            player.pos.add(0.3, 0.0, 0.3).toBlockPos()
        ) || isBurrowBlock(player.pos.add(-0.3, 0.0, 0.3).toBlockPos()) || isBurrowBlock(
            player.pos.add(
                -0.3,
                0.0,
                -0.3
            ).toBlockPos()
        ) || isBurrowBlock(player.pos.add(0.3, 0.0, -0.3).toBlockPos()))
    }

    private fun SafeClientEvent.isBurrowBlock(pos: BlockPos): Boolean {
        return (world.getBlockState(pos).block == Blocks.OBSIDIAN || world.getBlockState(pos).block == Blocks.CRYING_OBSIDIAN) && player.boundingBox.intersects(
            Box(pos)
        )
    }

    fun SafeClientEvent.isInWeb(entity: Entity): Boolean {
        return (isWeb(entity, player.blockPos) || isWeb(
            entity,
            entity.pos.add(0.3, 0.0, 0.3).toBlockPos()
        ) || isWeb(entity, entity.pos.add(-0.3, 0.0, 0.3).toBlockPos()) || isWeb(
            entity,
            entity.pos.add(
                -0.3,
                0.0,
                -0.3
            ).toBlockPos()
        ) || isWeb(entity, entity.pos.add(0.3, 0.0, -0.3).toBlockPos()) || isWeb(entity, player.blockPos) || isWeb(
            entity,
            entity.pos.add(0.3, 1.0, 0.3).toBlockPos()
        ) || isWeb(entity, entity.pos.add(-0.3, 1.0, 0.3).toBlockPos()) || isWeb(
            entity,
            entity.pos.add(
                -0.3,
                1.0,
                -0.3
            ).toBlockPos()
        ) || isWeb(entity, entity.pos.add(0.3, 1.0, -0.3).toBlockPos()))
    }

    private fun SafeClientEvent.isWeb(entity: Entity, pos: BlockPos): Boolean {
        return world.getBlockState(pos).block is CobwebBlock && entity.boundingBox.intersects(
            Box(pos)
        )
    }

    fun SafeClientEvent.boxCheck(box: Box, entity: Boolean = false): Boolean {
        return world.entities.none {
            (it !is ItemEntity || !entity);it.isAlive;it.boundingBox.intersects(box)
        }
    }

    fun SafeClientEvent.getDst(vec: Vec3d?): Double {
        return player.pos.distanceTo(vec)
    }

    private fun SafeClientEvent.centerPlayer(x: Double, y: Double, z: Double) {
        connection.sendPacket(PlayerMoveC2SPacket.Full(x, y, z, player.yaw, player.pitch, true))
        player.setPosition(x, y, z)
    }

    fun isSafe(entity: Entity, height: Int, floor: Boolean, face: Boolean): Boolean {
        return getUnsafeBlocks(entity, height, floor, face).isEmpty()
    }

    fun getUnsafeBlocks(entity: Entity, height: Int, floor: Boolean, face: Boolean): List<Vec3d?> {
        return getUnsafeBlocksFromVec3d(entity.pos, height, floor, face)
    }

    fun getUnsafeBlocksFromVec3d(pos: Vec3d, height: Int, floor: Boolean, face: Boolean): List<Vec3d?> {
        val vec3ds = ArrayList<Vec3d?>()
        for (vector in getOffsets(height, floor, face)) {
            val targetPos = pos.toBlockPos().add(vector.x.toInt(), vector.y.toInt(), vector.z.toInt())
            val block = mc.world!!.getBlockState(targetPos).block
            if (block !is AirBlock && block !is FluidBlock && block !is TallPlantBlock && block !is FireBlock && block !is DeadBushBlock && block !is SnowBlock) continue
            vec3ds.add(vector)
        }
        return vec3ds
    }

    fun getOffsets(y: Int, floor: Boolean, face: Boolean): Array<Vec3d> {
        val offsets = getOffsetList(y, floor, face)
        return offsets.toTypedArray()
    }

    fun getOffsetList(y: Int, floor: Boolean, face: Boolean): List<Vec3d> {
        val offsets = ArrayList<Vec3d>()
        if (face) {
            offsets.add(Vec3d(-1.0, y.toDouble(), 0.0))
            offsets.add(Vec3d(1.0, y.toDouble(), 0.0))
            offsets.add(Vec3d(0.0, y.toDouble(), -1.0))
            offsets.add(Vec3d(0.0, y.toDouble(), 1.0))
        } else {
            offsets.add(Vec3d(-1.0, y.toDouble(), 0.0))
        }
        if (floor) {
            offsets.add(Vec3d(0.0, (y - 1).toDouble(), 0.0))
        }
        return offsets
    }

    fun SafeClientEvent.isntValid(
        entity: PlayerEntity,
        range: Double,
        wallCheck: Boolean = false,
        wallRange: Double = 6.0
    ): Boolean {
        return entity == player
                || !entity.isAlive
                || FriendManager.isFriend(entity.name.string)
                || player.distanceSqTo(entity.pos) > range.sq || (!canSeeEntity(entity) && player.distanceSqTo(entity.pos) > wallRange.sq && wallCheck)
    }

    fun getClosestEnemy(distance: Double): PlayerEntity? {
        return runSafe {
            var closest: PlayerEntity? = null
            for (player in EntityManager.players) {
                if (isntValid(player, distance)) continue
                if (closest == null) {
                    closest = player
                    continue
                }
                if (player.distanceSqTo(player.pos) >= player.distanceSqTo(
                        closest.pos
                    )
                ) continue
                closest = player
            }
            closest
        }
    }

    inline fun ClientPlayerEntity.spoofSneak(block: () -> Unit) {
        if (!this.isSneaking) {
            runSafe {
                connection.sendPacket(ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY))
                block.invoke()
                connection.sendPacket(ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY))
            }
        } else {
            block.invoke()
        }
    }
}