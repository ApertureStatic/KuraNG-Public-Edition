package dev.dyzjct.kura.mixin.render;

import dev.dyzjct.kura.module.modules.render.Brightness;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(WorldRenderer.class)
public class MixinWorldRenderer {
    @ModifyVariable(method = "getLightmapCoordinates(Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;)I", at = @At(value = "STORE"), ordinal = 0)
    private static int getLightmapCoordinatesModifySkyLight(int sky) {
        if (Brightness.INSTANCE.isEnabled()) {
            return (Brightness.INSTANCE.getBrightness().getValue());
        }
        return sky;
    }
}
