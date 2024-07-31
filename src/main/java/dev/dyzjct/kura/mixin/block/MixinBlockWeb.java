package dev.dyzjct.kura.mixin.block;

import dev.dyzjct.kura.module.modules.movement.FastWeb;
import net.minecraft.block.BlockState;
import net.minecraft.block.CobwebBlock;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CobwebBlock.class)
public class MixinBlockWeb {

    @Inject(method = "onEntityCollision", at = @At("HEAD"), cancellable = true)
    public void onEntityCollidedWithBlock(BlockState state, World world, BlockPos pos, Entity entity, CallbackInfo ci) {
        if (FastWeb.INSTANCE.isEnabled()) {
            if (FastWeb.INSTANCE.getMode().getValue() == FastWeb.Mode.Vanilla) {
                ci.cancel();
            }
        }
    }
}
