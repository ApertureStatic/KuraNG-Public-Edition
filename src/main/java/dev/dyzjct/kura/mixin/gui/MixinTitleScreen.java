package dev.dyzjct.kura.mixin.gui;

import dev.dyzjct.kura.KuraIdentifier;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.dyzjct.kura.module.modules.client.UiSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashTextRenderer;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(TitleScreen.class)
public abstract class MixinTitleScreen {
    @Mutable
    @Final
    @Shadow
    private final boolean doBackgroundFade;
    @Shadow
    @Nullable
    private SplashTextRenderer splashText;
    @Shadow
    private long backgroundFadeStart;

    protected MixinTitleScreen(boolean doBackgroundFade) {
        this.doBackgroundFade = doBackgroundFade;
    }

    @Inject(method = "init", at = @At("HEAD"))
    private void modifyTitle(CallbackInfo ci) {
        // 實際上不需要使用全局變量，直接獲取即可，修復了賦值一次導致的始終無法切換的bug
        this.splashText = new SplashTextRenderer(UiSetting.getSlashText());
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        int width = MinecraftClient.getInstance().getWindow().getScaledWidth();
        int height = MinecraftClient.getInstance().getWindow().getScaledHeight();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();

        if (this.backgroundFadeStart == 0L && this.doBackgroundFade) {
            this.backgroundFadeStart = Util.getMeasuringTimeMs();
        }
        float f = this.doBackgroundFade ? (float) (Util.getMeasuringTimeMs() - this.backgroundFadeStart) / 1000.0F : 1.0F;

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, f);

        // 同上
        context.drawTexture(new KuraIdentifier("background/"+ UiSetting.splashImg() + ".png"), 0, 0, 0, 0, width, height, width, height);

        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }
}
