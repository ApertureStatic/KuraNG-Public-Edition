package dev.dyzjct.kura.mixin.input;

import dev.dyzjct.kura.module.ModuleManager;
import base.events.input.MouseClickEvent;
import base.events.input.MouseScrollEvent;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MixinMouse {
    @Inject(at = @At("RETURN"), method = "onMouseScroll(JDD)V")
    private void onOnMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        MouseScrollEvent event = new MouseScrollEvent(vertical);
        event.post();
    }

    @Inject(method = "onMouseButton", at = @At("HEAD"))
    public void onMouseClick(long window, int button, int action, int mods, CallbackInfo ci) {
        new MouseClickEvent(action, button).post();
        if (button == 4 || button == 5) {
            ModuleManager.INSTANCE.onKeyPressed(button, action);
        }
    }
}
