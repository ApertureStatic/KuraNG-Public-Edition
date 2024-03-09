package dev.dyzjct.kura.mixin.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Pair;
import dev.dyzjct.kura.module.modules.player.NoEntityTrace;
import dev.dyzjct.kura.module.modules.render.Aspect;
import dev.dyzjct.kura.module.modules.render.CustomFov;
import dev.dyzjct.kura.module.modules.render.NoRender;
import dev.dyzjct.kura.module.modules.render.Zoom;
import dev.dyzjct.kura.utils.math.FrameRateCounter;
import melon.events.render.Render3DEvent;
import melon.system.render.graphic.ProjectionUtils;
import melon.system.render.graphic.Render3DEngine;
import melon.system.render.graphic.RenderUtils3D;
import melon.system.render.shader.GlProgram;
import melon.system.render.shader.MSAAFramebuffer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderStage;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.SwordItem;
import net.minecraft.resource.ResourceFactory;
import org.joml.Matrix4f;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;
import java.util.function.Consumer;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {
    @Shadow
    @Final
    MinecraftClient client;
    @Shadow
    private float zoom;

    @Shadow
    private float zoomX;

    @Shadow
    private float zoomY;
    @Shadow
    private float viewDistance;


    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiler/Profiler;pop()V", shift = At.Shift.BEFORE))
    private void postHudRenderHook(float tickDelta, long startTime, boolean tick, CallbackInfo ci) {
        FrameRateCounter.INSTANCE.recordFrame();
    }

    @Inject(method = "getBasicProjectionMatrix", at = @At("TAIL"), cancellable = true)
    public void getBasicProjectionMatrixHook(double fov, CallbackInfoReturnable<Matrix4f> cir) {
        if (Aspect.INSTANCE.isEnabled()) {
            MatrixStack matrixStack = new MatrixStack();
            matrixStack.peek().getPositionMatrix().identity();
            if (zoom != 1.0f) {
                matrixStack.translate(zoomX, -zoomY, 0.0f);
                matrixStack.scale(zoom, zoom, 1.0f);
            }
            matrixStack.peek().getPositionMatrix().mul(new Matrix4f().setPerspective((float) (fov * 0.01745329238474369), Aspect.INSTANCE.getRatio(), 0.05f, viewDistance * 4.0f));
            cir.setReturnValue(matrixStack.peek().getPositionMatrix());
        }
    }

    @Inject(method = "renderWorld", at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/GameRenderer;renderHand:Z", opcode = Opcodes.GETFIELD, ordinal = 0))
    public void render3dHook(float tickDelta, long limitTime, MatrixStack matrix, CallbackInfo ci) {
        client.getProfiler().push("MelonRender3D");
        Render3DEngine.INSTANCE.getLastProjMat().set(RenderSystem.getProjectionMatrix());
        Render3DEngine.INSTANCE.getLastModMat().set(RenderSystem.getModelViewMatrix());
        Render3DEngine.INSTANCE.getLastWorldSpaceMatrix().set(matrix.peek().getPositionMatrix());

        MSAAFramebuffer.Companion.use(() -> {
            ProjectionUtils.INSTANCE.updateMatrix();
            RenderUtils3D.INSTANCE.prepareGL(matrix);
            Render3DEvent event = new Render3DEvent(matrix, tickDelta);
            event.post();
            RenderUtils3D.INSTANCE.releaseGL(matrix);
            GlStateManager._glUseProgram(0);
        });
        RenderSystem.applyModelViewMatrix();
        client.getProfiler().pop();

    }

    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
    private void tiltViewWhenHurtHook(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        if (NoRender.INSTANCE.isEnabled() && NoRender.INSTANCE.getNoHurtCam().getValue()) {
            ci.cancel();
        }
    }

    @Inject(method = "showFloatingItem", at = @At("HEAD"), cancellable = true)
    private void onShowFloatingItem(ItemStack floatingItem, CallbackInfo ci) {
        if (floatingItem.getItem() == Items.TOTEM_OF_UNDYING && NoRender.INSTANCE.isEnabled() && NoRender.INSTANCE.getTotemPops().getValue()) {
            ci.cancel();
        }
    }

    @Inject(method = "loadPrograms", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", ordinal = 0), locals = LocalCapture.CAPTURE_FAILHARD)
    public void loadAllTheShaders(ResourceFactory factory, CallbackInfo ci, List<ShaderStage> stages, List<Pair<ShaderProgram, Consumer<ShaderProgram>>> shadersToLoad) {
        GlProgram.Companion.forEachProgram(loader -> shadersToLoad.add(new Pair<>(loader.getLeft().apply(factory), loader.getRight())));
    }

    @Inject(at = @At("TAIL"), method = "getFov(Lnet/minecraft/client/render/Camera;FZ)D", cancellable = true)
    public void getFov(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Double> cb) {
        if (CustomFov.INSTANCE.isEnabled()) {
            if (cb.getReturnValue() == 70 && !CustomFov.INSTANCE.getItemFov().getValue()) return;
            else if (CustomFov.INSTANCE.getItemFov().getValue() && cb.getReturnValue() == 70) {
                cb.setReturnValue(CustomFov.INSTANCE.getItemFovModifier().getValue());
                return;
            }
            if (Zoom.INSTANCE.isEnabled()) {
                cb.setReturnValue(Zoom.INSTANCE.getFov());
            } else cb.setReturnValue(CustomFov.INSTANCE.getFov().getValue());
        } else if (Zoom.INSTANCE.isEnabled()) {
            if (cb.getReturnValue() == 70) {
                if (CustomFov.INSTANCE.getItemFov().getValue())
                    cb.setReturnValue(CustomFov.INSTANCE.getItemFovModifier().getValue());
                return;
            }
            cb.setReturnValue(Zoom.INSTANCE.getFov());
        }
    }

    @Inject(method = "updateTargetedEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/projectile/ProjectileUtil;raycast(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Box;Ljava/util/function/Predicate;D)Lnet/minecraft/util/hit/EntityHitResult;"), cancellable = true)
    private void onUpdateTargetedEntity(float tickDelta, CallbackInfo info) {
        if (client.player != null) {
            if (NoEntityTrace.INSTANCE.isEnabled() && (client.player.getMainHandStack().getItem() instanceof PickaxeItem || !NoEntityTrace.INSTANCE.getPickaxeOnly().getValue())) {
                if (client.player.getMainHandStack().getItem() instanceof SwordItem && NoEntityTrace.INSTANCE.getNoSword().getValue())
                    return;
                client.getProfiler().pop();
                info.cancel();
            }
        }
    }
}
