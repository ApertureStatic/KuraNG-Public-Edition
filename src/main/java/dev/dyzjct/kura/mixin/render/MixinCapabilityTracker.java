package dev.dyzjct.kura.mixin.render;

import dev.dyzjct.kura.mixins.ICapabilityTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "com.mojang.blaze3d.platform.GlStateManager$CapabilityTracker")
public abstract class MixinCapabilityTracker implements ICapabilityTracker {
    @Shadow
    private boolean state;

    @Shadow
    public abstract void setState(boolean state);

    @Override
    public boolean melonGet() {
        return state;
    }

    @Override
    public void melonSet(boolean state) {
        setState(state);
    }
}
