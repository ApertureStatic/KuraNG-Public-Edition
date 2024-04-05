package dev.dyzjct.kura.mixin.gui;

import base.KuraIdentifier;
import com.mojang.blaze3d.systems.RenderSystem;
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
    @Unique
    private static final Identifier RIMURU_BACKGROUND_TEXTURE = new KuraIdentifier("background/rimuru_background.png");
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
        this.splashText = new SplashTextRenderer("RimuruSama!!");
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

        context.drawTexture(RIMURU_BACKGROUND_TEXTURE, 0, 0, 0, 0, width, height, width, height);

        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }
}
