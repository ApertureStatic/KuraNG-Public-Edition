package dev.dyzjct.kura.utils.block

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import base.utils.Wrapper
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.client.world.ClientWorld
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.world.World

val shulkerList: Set<Block> = hashSetOf(
    Blocks.WHITE_SHULKER_BOX,
    Blocks.ORANGE_SHULKER_BOX,
    Blocks.MAGENTA_SHULKER_BOX,
    Blocks.LIGHT_BLUE_SHULKER_BOX,
    Blocks.YELLOW_SHULKER_BOX,
    Blocks.LIME_SHULKER_BOX,
    Blocks.PINK_SHULKER_BOX,
    Blocks.GRAY_SHULKER_BOX,
    Blocks.CYAN_SHULKER_BOX,
    Blocks.PURPLE_SHULKER_BOX,
    Blocks.BLUE_SHULKER_BOX,
    Blocks.BROWN_SHULKER_BOX,
    Blocks.GREEN_SHULKER_BOX,
    Blocks.RED_SHULKER_BOX,
    Blocks.BLACK_SHULKER_BOX
)

val blockBlacklist: Set<Block> = hashSetOf(
    Blocks.ENDER_CHEST,
    Blocks.CHEST,
    Blocks.TRAPPED_CHEST,
    Blocks.CRAFTING_TABLE,
    Blocks.ANVIL,
    Blocks.BREWING_STAND,
    Blocks.HOPPER,
    Blocks.DROPPER,
    Blocks.DISPENSER,
    Blocks.ENCHANTING_TABLE
).apply {
    addAll(shulkerList)
}

private val hashMap = Object2IntOpenHashMap<Block>().apply { defaultReturnValue(-1) }

inline val BlockState.isBlacklisted: Boolean
    get() = blockBlacklist.contains(this.block)

inline val BlockState.isLiquidBlock: Boolean
    get() = this.isLiquid

inline val BlockState.isWater: Boolean
    get() = this.block == Blocks.WATER

inline val BlockState.isFullBox: Boolean
    get() = Wrapper.world?.let {
        if (!getCollisionShape(it, BlockPos.ORIGIN).isEmpty) {
            this.getOutlineShape(it, BlockPos.ORIGIN).boundingBox
        } else {
            false
        }
    } == Box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)

inline fun World.getBlockState(x: Int, y: Int, z: Int): BlockState {
    return if (y !in 0..255) {
        Blocks.AIR.defaultState
    } else {
        val chunk = getChunk(x shr 4, z shr 4)
        return if (chunk.isEmpty) Blocks.AIR.defaultState else chunk.getBlockState(BlockPos(x, y, z))
    }
}

inline fun World.isAir(x: Int, y: Int, z: Int): Boolean {
    return getBlockState(x, y, z).block == Blocks.AIR
}

inline fun World.isAir(pos: BlockPos): Boolean {
    return getBlockState(pos).block == Blocks.AIR
}

inline fun World.getBlock(pos: BlockPos): Block =
    this.getBlockState(pos).block

inline fun ClientWorld.getSelectedBox(pos: BlockPos): Box =
    this.getBlockState(pos).getOutlineShape(this, pos).boundingBox

inline fun ClientWorld.getCollisionBox(pos: BlockPos): Box =
    this.getBlockState(pos).getOutlineShape(this, pos).boundingBox

inline val BlockState.isReplaceable: Boolean
    get() = this.isReplaceable

inline fun World.notBlockExpandY(entityPlayer: PlayerEntity, blockType: Block, expandY: Int): Boolean {
    return getBlockState(entityPlayer.blockPos.add(0, expandY, 0)).block != blockType
}

inline fun World.notBlock(entityPlayer: PlayerEntity, blockType: Block): Boolean {
    return getBlockState(entityPlayer.blockPos).block != blockType
}

inline fun World.notBlock(pos: BlockPos, blockType: Block): Boolean {
    return getBlockState(pos).block != blockType
}
