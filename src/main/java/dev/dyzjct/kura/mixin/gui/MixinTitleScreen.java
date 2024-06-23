package dev.dyzjct.kura.mixin.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.dyzjct.kura.Kura;
import dev.dyzjct.kura.KuraIdentifier;
import dev.dyzjct.kura.module.modules.client.UiSetting;
import dev.dyzjct.kura.system.render.graphic.Render2DEngine;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.RotatingCubeMapRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.SplashTextRenderer;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(TitleScreen.class)
public class MixinTitleScreen extends Screen {
    @Unique
    private static final Identifier DreamDev = new KuraIdentifier("background/longbg.png");
    @Final
    @Shadow
    private boolean doBackgroundFade;
    @Shadow
    private long backgroundFadeStart;
    @Final
    @Shadow
    private static Identifier PANORAMA_OVERLAY;
    @Final
    @Shadow
    private RotatingCubeMapRenderer backgroundRenderer;


    protected MixinTitleScreen(Text title) {
        super(title);
    }


    /**
     * @author dyzjct
     * @reason fuck u mojang
     */
    @Overwrite
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        if (this.backgroundFadeStart == 0L && this.doBackgroundFade) {
            this.backgroundFadeStart = Util.getMeasuringTimeMs();
        }

        float f = this.doBackgroundFade ? (float) (Util.getMeasuringTimeMs() - this.backgroundFadeStart) / 1000.0F : 1.0F;
        this.backgroundRenderer.render(delta, MathHelper.clamp(f, 0.0F, 1.0F));
        RenderSystem.enableBlend();
        context.setShaderColor(1.0F, 1.0F, 1.0F, this.doBackgroundFade ? (float) MathHelper.ceil(MathHelper.clamp(f, 0.0F, 1.0F)) : 1.0F);
        context.drawTexture(PANORAMA_OVERLAY, 0, 0, this.width, this.height, 0.0F, 0.0F, 16, 128, 16, 128);
        context.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        float g = this.doBackgroundFade ? MathHelper.clamp(f - 1.0F, 0.0F, 1.0F) : 1.0F;

        // why？ cannot load bg image?
        Render2DEngine.INSTANCE.drawRect(context.getMatrices(), 0, 0, width, height, UiSetting.INSTANCE.getSplashColor());
//        RenderSystem.blendEquation(GL14.GL_FUNC_ADD);
//        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
//        RenderSystem.enableBlend();
//        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
//        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
//        context.drawTexture(new KuraIdentifier("background/" + UiSetting.splashImg() + ".png"), 0, 0, width, height, width, height, width, height);
//        context.drawTexture(new KuraIdentifier("background/" + UiSetting.splashImg() + ".png"), 0, 0, 0, 0, this.width, this.height, this.width, this.height);

        int width = MinecraftClient.getInstance().getWindow().getScaledWidth();
        int height = MinecraftClient.getInstance().getWindow().getScaledHeight();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();

        if (this.backgroundFadeStart == 0L && this.doBackgroundFade) {
            this.backgroundFadeStart = Util.getMeasuringTimeMs();
        }
        float L = this.doBackgroundFade ? (float) (Util.getMeasuringTimeMs() - this.backgroundFadeStart) / 1000.0F : 1.0F;

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, L);

        // 同上
        context.drawTexture(DreamDev, -750+mouseX/2, 0, 0, 0, 0, width+960, height, width+960, height);

        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        context.setShaderColor(1.0f, 1.0f, 1.0f, g);
        context.drawTexture(new KuraIdentifier("logo/logo.png"), this.width / 2 - 35, this.height / 4 - 45, 0f, 0f, 80, 80, 80, 80);
        context.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        int i = MathHelper.ceil(g * 255.0F) << 24;
        if ((i & -67108864) != 0) {
            String string = "Minecraft " + SharedConstants.getGameVersion().getName();
            assert this.client != null;
            if (this.client.isDemo()) {
                string = string + " Demo";
            }
            string = string + "/§b" + Kura.MOD_NAME + " " + Kura.VERSION;

            context.drawTextWithShadow(this.textRenderer, string, 2, this.height - 10, 16777215 | i);
        }
    }
}
