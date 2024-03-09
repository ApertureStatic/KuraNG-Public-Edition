package dev.dyzjct.kura.mixin.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.dyzjct.kura.module.ModuleManager;
import dev.dyzjct.kura.module.modules.client.GameAnimation;
import base.events.render.Render2DEvent;
import base.system.render.shader.MSAAFramebuffer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.option.AttackIndicator;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class MixinInGameHud {
    @Final
    @Shadow
    private static final Identifier ICONS = new Identifier("textures/gui/icons.png");
    @Final
    @Shadow
    private static Identifier WIDGETS_TEXTURE = new Identifier("textures/gui/widgets.png");
    @Shadow
    private int scaledWidth;
    @Shadow
    private int scaledHeight;
    @Shadow
    @Final
    private MinecraftClient client;

    @Shadow
    public abstract void clear();

    @Shadow
    private void renderHotbarItem(DrawContext context, int x, int y, float f, PlayerEntity player, ItemStack stack, int seed) {
    }

    @Shadow
    private PlayerEntity getCameraPlayer() {
        if (!(this.client.getCameraEntity() instanceof PlayerEntity)) {
            return null;
        }
        return (PlayerEntity) this.client.getCameraEntity();
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void onRender(DrawContext context, float tickDelta, CallbackInfo ci) {
        client.getProfiler().push("MelonRender2D");

        MSAAFramebuffer.Companion.use(() -> {
            Render2DEvent event = new Render2DEvent(context, scaledWidth, scaledHeight, tickDelta);
            event.post();
            ModuleManager.INSTANCE.onRenderHUD(context);
        });

        client.getProfiler().pop();
    }

    @Inject(at = @At(value = "HEAD"), method = "renderHotbar", cancellable = true)
    public void renderHotbarCustom(float tickDelta, DrawContext context, CallbackInfo ci) {
        float f;
        int o;
        int n;
        int m;
        if (GameAnimation.INSTANCE.isEnabled()) {
            if (GameAnimation.INSTANCE.getHotbar().getValue()) {
                ci.cancel();
                PlayerEntity player = getCameraPlayer();
                if (player != null) {
                    Arm primaryHand = player.getMainArm().getOpposite();
                    int i = scaledWidth / 2;
                    float x = GameAnimation.INSTANCE.updateHotbar();
                    context.getMatrices().push();
                    context.getMatrices().translate(0.0f, 0.0f, -90.0f);
                    context.drawTexture(WIDGETS_TEXTURE, i - 91, this.scaledHeight - 22, 0, 0, 182, 22);
                    context.drawTexture(WIDGETS_TEXTURE, (int) (i - 91 - 1 + x), this.scaledHeight - 22 - 1, 0, 22, 24, 22);

                    ItemStack offHandStack = player.getOffHandStack();
                    if (!offHandStack.isEmpty()) {
                        if (primaryHand == Arm.LEFT) {
                            context.drawTexture(WIDGETS_TEXTURE, i - 91 - 29, this.scaledHeight - 23, 24, 22, 29, 24);
                        } else {
                            context.drawTexture(WIDGETS_TEXTURE, i + 91, this.scaledHeight - 23, 53, 22, 29, 24);
                        }
                    }
                    context.getMatrices().pop();
                    int l = 1;
                    for (m = 0; m < 9; ++m) {
                        n = i - 90 + m * 20 + 2;
                        o = this.scaledHeight - 16 - 3;
                        this.renderHotbarItem(context, n, o, tickDelta, player, player.getInventory().main.get(m), l++);
                    }
                    if (!offHandStack.isEmpty()) {
                        m = this.scaledHeight - 16 - 3;
                        if (primaryHand == Arm.LEFT) {
                            this.renderHotbarItem(context, i - 91 - 26, m, tickDelta, player, offHandStack, l++);
                        } else {
                            this.renderHotbarItem(context, i + 91 + 10, m, tickDelta, player, offHandStack, l++);
                        }
                    }
                    RenderSystem.enableBlend();
                    if (this.client.options.getAttackIndicator().getValue() == AttackIndicator.HOTBAR && client.player != null) {
                        if ((f = this.client.player.getAttackCooldownProgress(0.0f)) < 1.0f) {
                            n = this.scaledHeight - 20;
                            o = i + 91 + 6;
                            if (primaryHand == Arm.RIGHT) {
                                o = i - 91 - 22;
                            }
                            int p = (int) (f * 19.0f);
                            context.drawTexture(ICONS, o, n, 0, 94, 18, 18);
                            context.drawTexture(ICONS, o, n + 18 - p, 18, 112 - p, 18, p);
                        }
                    }
                    RenderSystem.disableBlend();
                }
            }
        }
    }
}