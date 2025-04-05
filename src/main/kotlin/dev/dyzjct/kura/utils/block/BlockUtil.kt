package dev.dyzjct.kura.utils.block

import base.utils.entity.EntityUtils.boxCheck
import base.utils.entity.EntityUtils.eyePosition
import base.utils.inventory.slot.allSlots
import base.utils.inventory.slot.hotbarSlots
import base.utils.item.isTool
import base.utils.world.checkAxis
import base.utils.world.getMiningSide
import base.utils.world.getVisibleSides
import dev.dyzjct.kura.event.eventbus.SafeClientEvent
import dev.dyzjct.kura.module.modules.client.AntiCheat
import dev.dyzjct.kura.module.modules.client.CombatSystem
import dev.dyzjct.kura.module.modules.player.PacketMine
import dev.dyzjct.kura.utils.animations.fastCeil
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.block.FireBlock
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.enchantment.Enchantments
import net.minecraft.entity.Entity
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import net.minecraft.world.RaycastContext

object BlockUtil {
    fun SafeClientEvent.getStrictFacing(pos: BlockPos): Direction {
        return getMiningSide(pos) ?: Direction.UP
    }

    fun SafeClientEvent.getNeighbor(pos: BlockPos): EasyBlock? {
        for (side in Direction.entries) {
            val offsetPos = pos.offset(side)
            val oppositeSide = side.opposite

            if (CombatSystem.strictDirection && !getVisibleSides(offsetPos, true).contains(oppositeSide)) continue
            if (world.getBlockState(offsetPos).isReplaceable) continue

            return EasyBlock(offsetPos, oppositeSide ?: Direction.UP)
        }

        return null
    }

    fun SafeClientEvent.getAnchorBlock(pos: BlockPos, strictDirection: Boolean): AnchorBlock? {
        val click = getMiningSide(pos)
        for (side in Direction.entries) {
            val offsetPos = pos.offset(side)
            val oppositeSide = side.opposite
            if (strictDirection && !getVisibleSides(offsetPos, true).contains(oppositeSide)) continue
            if (world.getBlockState(offsetPos).isReplaceable) continue
            click?.let {
                return AnchorBlock(offsetPos, oppositeSide ?: Direction.UP, it)
            }
        }
        return null
    }

    @JvmStatic
    fun SafeClientEvent.calcBreakTime(pos: BlockPos, inventory: Boolean): Float {
        val blockState = world.getBlockState(pos)

        val hardness = blockState.getHardness(mc.world, pos)
        val breakSpeed = getBreakSpeed(blockState, inventory)

        if (breakSpeed == -1.0f) {
            return -1f
        }

        val relativeDamage = breakSpeed / hardness / 30.0f
        val ticks = (0.7f / relativeDamage).fastCeil()
        return ticks * 50f
    }

    @JvmStatic
    fun SafeClientEvent.getBreakSpeed(blockState: BlockState, inventory: Boolean = false): Float {
        var maxSpeed = 1.0f
        for (slot in if (inventory) player.allSlots else player.hotbarSlots) {
            val stack = slot.stack

            if (stack.isEmpty || !stack.item.isTool) {
                continue
            } else {
                var speed = stack.getMiningSpeedMultiplier(blockState)

                if (speed <= 1.0f) {
                    continue
                } else {
                    val efficiency = EnchantmentHelper.getLevel(Enchantments.EFFICIENCY, stack)
                    if (efficiency > 0) {
                        speed += efficiency * efficiency + 1.0f
                    }
                }

                if (speed > maxSpeed) {
                    maxSpeed = speed
                }
            }
        }

        return maxSpeed
    }

    @JvmStatic
    fun SafeClientEvent.getBreakSpeedObi(blockState: BlockState): Float {
        var maxSpeed = 1.0f
        var speed = ItemStack(Items.DIAMOND_PICKAXE).getMiningSpeedMultiplier(blockState)
        speed += 5 * 5 + 1.0f
        if (speed > maxSpeed) {
            maxSpeed = speed
        }
        return maxSpeed
    }

    fun SafeClientEvent.checkNearBlocksExtended(blockPos: BlockPos): BlockPosWithFacing? {
        var ret = checkNearBlocks(blockPos)
        if (ret != null) return ret
        ret = checkNearBlocks(blockPos.add(-1, 0, 0))
        if (ret != null) return ret
        ret = checkNearBlocks(blockPos.add(1, 0, 0))
        if (ret != null) return ret
        ret = checkNearBlocks(blockPos.add(0, 0, 1))
        if (ret != null) return ret
        ret = checkNearBlocks(blockPos.add(0, 0, -1))
        if (ret != null) return ret
        ret = checkNearBlocks(blockPos.add(-2, 0, 0))
        if (ret != null) return ret
        ret = checkNearBlocks(blockPos.add(2, 0, 0))
        if (ret != null) return ret
        ret = checkNearBlocks(blockPos.add(0, 0, 2))
        if (ret != null) return ret
        ret = checkNearBlocks(blockPos.add(0, 0, -2))
        if (ret != null) return ret
        ret = checkNearBlocks(blockPos.add(0, -1, 0))
        val blockPos2 = blockPos.down()
        if (ret != null) return ret
        ret = checkNearBlocks(blockPos2.add(1, 0, 0))
        if (ret != null) return ret
        ret = checkNearBlocks(blockPos2.add(-1, 0, 0))
        if (ret != null) return ret
        ret = checkNearBlocks(blockPos2.add(0, 0, 1))
        val blockPos3 = blockPos2.down()
        if (ret != null) return ret
        ret = checkNearBlocks(blockPos3.add(1, 0, 0))
        if (ret != null) return ret
        ret = checkNearBlocks(blockPos3.add(-1, 0, 0))
        if (ret != null) return ret
        ret = checkNearBlocks(blockPos3.add(0, 0, 1))
        return ret ?: checkNearBlocks(blockPos3.add(0, 0, -1))
    }

    private fun SafeClientEvent.checkNearBlocks(blockPos: BlockPos): BlockPosWithFacing? {
        if (world.getBlockState(blockPos.add(0, -1, 0)).isSolid && boxCheck(
                Box(
                    blockPos.add(
                        0,
                        -1,
                        0
                    )
                )
            ) && blockPos.add(0, -1, -1) != PacketMine.blockData?.blockPos
        ) return BlockPosWithFacing(
            blockPos.add(
                0, -1, 0
            ), Direction.UP
        ) else if (world.getBlockState(blockPos.add(-1, 0, 0)).isSolid && boxCheck(
                Box(
                    blockPos.add(
                        -1,
                        0,
                        0
                    )
                )
            ) && blockPos.add(-1, 0, 0) != PacketMine.blockData?.blockPos
        ) return BlockPosWithFacing(
            blockPos.add(
                -1, 0, 0
            ), Direction.EAST
        ) else if (world.getBlockState(
                blockPos.add(
                    1, 0, 0
                )
            ).isSolid && boxCheck(Box(blockPos.add(1, 0, 0))) && blockPos.add(1, 0, 0) != PacketMine.blockData?.blockPos
        ) return BlockPosWithFacing(blockPos.add(1, 0, 0), Direction.WEST) else if (world.getBlockState(
                blockPos.add(0, 0, 1)
            ).isSolid && boxCheck(Box(blockPos.add(0, 0, 1))) && blockPos.add(0, 0, 1) != PacketMine.blockData?.blockPos
        ) return BlockPosWithFacing(blockPos.add(0, 0, 1), Direction.NORTH) else if (world.getBlockState(
                blockPos.add(0, 0, -1)
            ).isSolid && boxCheck(Box(blockPos.add(0, 0, -1))) && blockPos.add(
                0,
                0,
                -1
            ) != PacketMine.blockData?.blockPos
        ) return BlockPosWithFacing(blockPos.add(0, 0, -1), Direction.SOUTH)
        return null
    }

    fun SafeClientEvent.canBreak(pos: BlockPos, air: Boolean): Boolean {
        val blockState = world.getBlockState(pos) ?: return false
        val blackListBlocks = arrayOf(
            Blocks.AIR,
            Blocks.BEDROCK,
            Blocks.END_PORTAL_FRAME,
            Blocks.END_PORTAL,
            Blocks.WATER,
            Blocks.WATER_CAULDRON,
            Blocks.LAVA,
            Blocks.LAVA_CAULDRON,
            Blocks.FIRE
        )
        return when (blockState.block) {
            Blocks.AIR -> air
            is FireBlock -> false
            else -> blockState.getHardness(world, pos) != -1.0f && !blackListBlocks.any { it == pos }
        }
    }

    fun SafeClientEvent.canSee(x: Double, y: Double, z: Double): Boolean {
        return runCatching {
            val playerPos = player.pos.add(0.0, player.standingEyeHeight.toDouble(), 0.0)
            val entityPos = Vec3d(x, y, z)

            val canSeeFeet = world.raycast(
                RaycastContext(
                    playerPos, entityPos, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player
                )
            ).type === HitResult.Type.MISS

            val canSeeEyes = world.raycast(
                RaycastContext(
                    playerPos,
                    entityPos,
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE,
                    mc.player
                )
            ).type === HitResult.Type.MISS

            canSeeFeet || canSeeEyes
        }.getOrDefault(false)
    }

    fun SafeClientEvent.canSeeEntity(entity: Entity): Boolean {
        return runCatching {
            val playerPos = player.pos.add(0.0, player.standingEyeHeight.toDouble(), 0.0)
            val entityPos = entity.pos.add(0.0, entity.standingEyeHeight.toDouble(), 0.0)

            val canSeeFeet = world.raycast(
                RaycastContext(
                    playerPos, entityPos, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player
                )
            ).type === HitResult.Type.MISS

            val canSeeEyes = world.raycast(
                RaycastContext(
                    playerPos,
                    entityPos,
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE,
                    mc.player
                )
            ).type === HitResult.Type.MISS

            canSeeFeet || canSeeEyes
        }.getOrDefault(false)
    }

    fun SafeClientEvent.findDirection(blockPos: BlockPos): Direction? {
        for (direction in Direction.entries) {
            if (isStrictDirection(blockPos, direction)) return direction
        }
        return null
    }

    fun SafeClientEvent.isStrictDirection(pos: BlockPos, side: Direction): Boolean {
        if (player.blockY - pos.y >= 0 && side == Direction.DOWN) return false
        if (AntiCheat.ac != AntiCheat.AntiCheats.NCP) {
            if (side == Direction.UP && pos.y + 1 > player.eyePos.getY()) {
                return false
            }
        } else {
            if (side == Direction.UP && pos.y > player.eyePos.getY()) {
                return false
            }
        }

        val eyePos: Vec3d = player.eyePosition
        val blockCenter = pos.toCenterPos()
        val validAxis = ArrayList<Direction>()
        validAxis.addAll(checkAxis(eyePos.x - blockCenter.x, Direction.WEST, Direction.EAST, false))
        validAxis.addAll(checkAxis(eyePos.y - blockCenter.y, Direction.DOWN, Direction.UP, true))
        validAxis.addAll(checkAxis(eyePos.z - blockCenter.z, Direction.NORTH, Direction.SOUTH, false))
        return validAxis.contains(side)
    }

    data class BlockPosWithFacing(val position: BlockPos, val facing: Direction)

    class EasyBlock(var blockPos: BlockPos, var face: Direction)

    class AnchorBlock(var blockPos: BlockPos, var face: Direction, var clickFace: Direction)
}