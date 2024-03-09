package dev.dyzjct.kura.mixin.input;

import dev.dyzjct.kura.module.ModuleManager;
import base.events.chat.CharTypedEvent;
import base.utils.screen.ScreenUtils;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class MixinKeyboard {
    @Mutable
    @Final
    @Shadow
    private MinecraftClient client;

    public MixinKeyboard(MinecraftClient client) {
        this.client = client;
    }

    @Inject(method = "onKey", at = @At(value = "HEAD"), cancellable = true)
    public void onKey(long window, int key, int scancode, int action, int modifiers, CallbackInfo info) {
        if (ScreenUtils.INSTANCE.safeReturn(client.currentScreen)) {
            return;
        }
        if (ModuleManager.INSTANCE.onKeyPressed(key, action)) {
            info.cancel();
        }
        //ChatUtil.INSTANCE.sendMessage(String.valueOf(key));
    }


    @Inject(method = "onChar", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;currentScreen:Lnet/minecraft/client/gui/screen/Screen;"), cancellable = true)
    public void onOneChar(long window, int i, int j, CallbackInfo ci) {
        boolean consumed = false;
        if (Character.charCount(i) == 1) {
            CharTypedEvent event = new CharTypedEvent((char) i);
            event.post();
            consumed = event.getCancelled();
        } else {
            for (char c : Character.toChars(i)) {
                CharTypedEvent event = new CharTypedEvent(c);
                event.post();
                consumed = consumed || event.getCancelled();
            }
        }

        if (consumed)
            ci.cancel();
    }
}
