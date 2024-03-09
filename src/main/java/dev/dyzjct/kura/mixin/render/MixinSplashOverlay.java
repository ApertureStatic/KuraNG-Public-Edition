package dev.dyzjct.kura.mixin.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.dyzjct.kura.module.modules.client.LoadingMenu;
import melon.utils.Wrapper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.resource.ResourceReload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import team.exception.melon.MelonIdentifier;

import java.util.Optional;
import java.util.function.Consumer;

@Mixin(SplashOverlay.class)
public abstract class MixinSplashOverlay {
    @Unique
    private static final Identifier XGP = new MelonIdentifier("textures/xgp.png");

    @Unique
    private static final Identifier GENSHIN_IMPACT = new MelonIdentifier("textures/genshin.png");

    private static final MinecraftClient mc = Wrapper.getMinecraft();
    @Final
    @Shadow
    private boolean reloading;
    @Shadow
    private float progress;
    @Shadow
    private long reloadCompleteTime = -1L;
    @Shadow
    private long reloadStartTime = -1L;
    @Final
    @Shadow
    private ResourceReload reload;
    @Final
    @Shadow
    private Consumer<Optional<Throwable>> exceptionHandler;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (LoadingMenu.INSTANCE.isDisabled()) return;
        ci.cancel();
        renderCustom(context);
    }

    @Unique
    public void renderCustom(DrawContext context) {
        int width = mc.getWindow().getScaledWidth();
        int height = mc.getWindow().getScaledHeight();
        long l = Util.getMeasuringTimeMs();
        if (reloading && reloadStartTime == -1L) {
            reloadStartTime = l;
        }

        float f = reloadCompleteTime > -1L ? (float) (l - reloadCompleteTime) / 1000.0F : -1.0F;
        float g = reloadStartTime > -1L ? (float) (l - reloadStartTime) / 500.0F : -1.0F;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.1f);
        context.drawTexture(switch ((LoadingMenu.LoadingMenuMode) LoadingMenu.INSTANCE.getMode().getValue()) {
            case XGP -> XGP;
            case GENSHIN_IMPACT -> GENSHIN_IMPACT;
        }, 0, 0, 0, 0, width, height, width, height);

        float t = this.reload.getProgress();
        this.progress = MathHelper.clamp(this.progress * 0.95F + t * 0.050000012F, 0.0F, 1.0F);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();

        if (f >= 2.0F) {
            mc.setOverlay(null);
        }

        if (reloadCompleteTime == -1L && reload.isComplete() && (!reloading || g >= 2.0F)) {
            try {
                reload.throwException();
                exceptionHandler.accept(Optional.empty());
            } catch (Throwable var23) {
                exceptionHandler.accept(Optional.of(var23));
            }

            reloadCompleteTime = Util.getMeasuringTimeMs();
            if (mc.currentScreen != null) {
                mc.currentScreen.init(mc, width, height);
            }
        }
    }
}