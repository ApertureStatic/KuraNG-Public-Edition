package dev.dyzjct.kura.mixin.world;

import base.events.WorldEvent;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ChunkLevelType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(World.class)
public abstract class MixinWorld implements WorldAccess, AutoCloseable {
    @Shadow
    @Final
    public boolean isClient;
    @Shadow
    @Final
    private boolean debugWorld;

    @Shadow
    @Final
    public boolean isDebugWorld() {
        return this.debugWorld;
    }

    @Shadow
    public WorldChunk getWorldChunk(BlockPos pos) {
        return null;
    }

    @Shadow
    public void scheduleBlockRerenderIfNeeded(BlockPos pos, BlockState old, BlockState updated) {
    }

    @Shadow
    public abstract void updateListeners(BlockPos var1, BlockState var2, BlockState var3, int var4);

    @Shadow
    public void updateComparators(BlockPos pos, Block block) {
    }

    @Shadow
    public void onBlockChanged(BlockPos pos, BlockState oldBlock, BlockState newBlock) {
    }

    /**
     * @author nigger
     * @reason FUCK SHIT ABSTRACT METHOD
     */
    @Overwrite
    public boolean setBlockState(BlockPos pos, BlockState state, int flags, int maxUpdateDepth) {
        if (this.isOutOfHeightLimit(pos)) {
            return false;
        }
        if (!this.isClient && this.isDebugWorld()) {
            return false;
        }
        WorldChunk worldChunk = this.getWorldChunk(pos);
        Block block = state.getBlock();
        BlockState blockState = worldChunk.setBlockState(pos, state, (flags & Block.MOVED) != 0);
        if (blockState != null) {
            BlockState blockState2 = this.getBlockState(pos);
            if (blockState2 == state) {
                if (blockState != blockState2) {
                    this.scheduleBlockRerenderIfNeeded(pos, blockState, blockState2);
                    WorldEvent.RenderUpdate event = new WorldEvent.RenderUpdate(pos);
                    event.post();
                }
                if ((flags & Block.NOTIFY_LISTENERS) != 0 && (!this.isClient || (flags & Block.NO_REDRAW) == 0) && (this.isClient || worldChunk.getLevelType() != null && worldChunk.getLevelType().isAfter(ChunkLevelType.BLOCK_TICKING))) {
                    this.updateListeners(pos, blockState, state, flags);
                    if ((flags & 3) != 0) {
                        WorldEvent.ClientBlockUpdate event = new WorldEvent.ClientBlockUpdate(pos, blockState, state);
                        event.post();
                    }
                }
                if ((flags & Block.NOTIFY_NEIGHBORS) != 0) {
                    this.updateNeighbors(pos, blockState.getBlock());
                    if (!this.isClient && state.hasComparatorOutput()) {
                        this.updateComparators(pos, block);
                    }
                }
                if ((flags & Block.FORCE_STATE) == 0 && maxUpdateDepth > 0) {
                    int i = flags & ~(Block.NOTIFY_NEIGHBORS | Block.SKIP_DROPS);
                    blockState.prepare(this, pos, i, maxUpdateDepth - 1);
                    state.updateNeighbors(this, pos, i, maxUpdateDepth - 1);
                    state.prepare(this, pos, i, maxUpdateDepth - 1);
                }
                this.onBlockChanged(pos, blockState, blockState2);
            }
            return true;
        }
        return false;
    }
}
