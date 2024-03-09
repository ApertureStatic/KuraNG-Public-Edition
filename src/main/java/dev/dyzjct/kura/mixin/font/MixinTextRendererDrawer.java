package dev.dyzjct.kura.mixin.font;

import dev.dyzjct.kura.mixins.ITextRendererDrawer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "net.minecraft.client.font.TextRenderer$Drawer")
public class MixinTextRendererDrawer implements ITextRendererDrawer {
    @Shadow
    float x;
    @Shadow
    float y;

    @Override
    public float getX() {
        return x;
    }

    @Override
    public void setX(Float x) {
        this.x = x;
    }

    @Override
    public float getY() {
        return y;
    }

    @Override
    public void setY(Float y) {
        this.y = y;
    }
}
