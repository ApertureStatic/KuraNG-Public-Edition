package dev.dyzjct.kura.mixin.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.VertexSorter;
import com.mojang.datafixers.util.Pair;
import dev.dyzjct.kura.event.events.render.Render3DEvent;
import dev.dyzjct.kura.module.modules.player.NoEntityTrace;
import dev.dyzjct.kura.module.modules.render.Aspect;
import dev.dyzjct.kura.module.modules.render.CustomFov;
import dev.dyzjct.kura.module.modules.render.NoRender;
import dev.dyzjct.kura.module.modules.render.Zoom;
import dev.dyzjct.kura.system.render.graphic.ProjectionUtils;
import dev.dyzjct.kura.system.render.graphic.Render3DEngine;
import dev.dyzjct.kura.system.render.graphic.RenderUtils3D;
import dev.dyzjct.kura.system.render.shader.GlProgram;
import dev.dyzjct.kura.system.render.shader.MSAAFramebuffer;
import dev.dyzjct.kura.utils.math.FrameRateCounter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderStage;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.render.*;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.SwordItem;
import net.minecraft.resource.ResourceFactory;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.world.GameMode;
import org.joml.Matrix4f;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;
import java.util.function.Consumer;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {
    @Mutable
    @Final
    @Shadow
    public final HeldItemRenderer firstPersonRenderer;
    @Mutable
    @Final
    @Shadow
    private final LightmapTextureManager lightmapTextureManager;
    @Mutable
    @Final
    @Shadow
    private final BufferBuilderStorage buffers;
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
    @Shadow
    private boolean renderingPanorama;
    @Shadow
    private float lastFovMultiplier;
    @Shadow
    private float fovMultiplier;

    public MixinGameRenderer(LightmapTextureManager lightmapTextureManager, HeldItemRenderer firstPersonRenderer, BufferBuilderStorage buffers) {
        this.lightmapTextureManager = lightmapTextureManager;
        this.firstPersonRenderer = firstPersonRenderer;
        this.buffers = buffers;
    }


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

    /**
     * @author dyzjct
     * @reason fuck u mojang.
     */
    @Overwrite
    private void renderHand(MatrixStack matrices, Camera camera, float tickDelta) {
        if (!this.renderingPanorama) {
            this.loadProjectionMatrix(getBasicProjectionMatrixKura(getFov(camera, tickDelta, false)));
            matrices.loadIdentity();
            matrices.push();
            this.tiltViewWhenHurt(matrices, tickDelta);
            if (this.client.options.getBobView().getValue()) {
                this.bobView(matrices, tickDelta);
            }

            boolean bl = this.client.getCameraEntity() instanceof LivingEntity && ((LivingEntity) this.client.getCameraEntity()).isSleeping();
            if (this.client.options.getPerspective().isFirstPerson() && !bl && !this.client.options.hudHidden && this.client.interactionManager.getCurrentGameMode() != GameMode.SPECTATOR) {
                this.lightmapTextureManager.enable();
                this.firstPersonRenderer.renderItem(tickDelta, matrices, this.buffers.getEntityVertexConsumers(), this.client.player, this.client.getEntityRenderDispatcher().getLight(this.client.player, tickDelta));
                this.lightmapTextureManager.disable();
            }

            matrices.pop();
            if (this.client.options.getPerspective().isFirstPerson() && !bl) {
                InGameOverlayRenderer.renderOverlays(this.client, matrices);
                this.tiltViewWhenHurt(matrices, tickDelta);
            }

            if (this.client.options.getBobView().getValue()) {
                this.bobView(matrices, tickDelta);
            }
        }
    }

    @Inject(method = "renderWorld", at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/GameRenderer;renderHand:Z", opcode = Opcodes.GETFIELD, ordinal = 0))
    public void render3dHook(float tickDelta, long limitTime, MatrixStack matrix, CallbackInfo ci) {
        client.getProfiler().push("KuraRender3D");
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

    @Unique
    private void bobView(MatrixStack matrices, float tickDelta) {
        if (this.client.getCameraEntity() instanceof PlayerEntity playerEntity) {
            float f = playerEntity.horizontalSpeed - playerEntity.prevHorizontalSpeed;
            float g = -(playerEntity.horizontalSpeed + f * tickDelta);
            float h = MathHelper.lerp(tickDelta, playerEntity.prevStrideDistance, playerEntity.strideDistance);
            matrices.translate(MathHelper.sin(g * 3.1415927F) * h * 0.5F, -Math.abs(MathHelper.cos(g * 3.1415927F) * h), 0.0F);
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(MathHelper.sin(g * 3.1415927F) * h * 3.0F));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(Math.abs(MathHelper.cos(g * 3.1415927F - 0.2F) * h) * 5.0F));
        }
    }

    @Unique
    private double getFov(Camera camera, float tickDelta, boolean changingFov) {
        if (this.renderingPanorama) {
            return 90.0;
        } else {
            double d = 70.0;
            if (changingFov) {
                d = (double) this.client.options.getFov().getValue();
                d *= MathHelper.lerp(tickDelta, this.lastFovMultiplier, this.fovMultiplier);
            }

            if (camera.getFocusedEntity() instanceof LivingEntity && ((LivingEntity) camera.getFocusedEntity()).isDead()) {
                float f = Math.min((float) ((LivingEntity) camera.getFocusedEntity()).deathTime + tickDelta, 20.0F);
                d /= (1.0F - 500.0F / (f + 500.0F)) * 2.0F + 1.0F;
            }

            CameraSubmersionType cameraSubmersionType = camera.getSubmersionType();
            if (cameraSubmersionType == CameraSubmersionType.LAVA || cameraSubmersionType == CameraSubmersionType.WATER) {
                d *= MathHelper.lerp(this.client.options.getFovEffectScale().getValue(), 1.0, 0.8571428656578064);
            }

            return d;
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

    @Unique
    public Matrix4f getBasicProjectionMatrixKura(double fov) {
        MatrixStack matrixStack = new MatrixStack();
        matrixStack.peek().getPositionMatrix().identity();
        if (this.zoom != 1.0F) {
            matrixStack.translate(this.zoomX, -this.zoomY, 0.0F);
            matrixStack.scale(this.zoom, this.zoom, 1.0F);
        }

        matrixStack.peek().getPositionMatrix().mul((new Matrix4f()).setPerspective((float) (fov * 0.01745329238474369), (float) this.client.getWindow().getFramebufferWidth() / (float) this.client.getWindow().getFramebufferHeight(), 0.05F, this.getFarPlaneDistance()));
        return matrixStack.peek().getPositionMatrix();
    }

    @Unique
    private void tiltViewWhenHurt(MatrixStack matrices, float tickDelta) {
        if (NoRender.INSTANCE.isEnabled() && NoRender.INSTANCE.getNoHurtCam().getValue()) {
            return;
        }
        if (this.client.getCameraEntity() instanceof LivingEntity livingEntity) {
            float f = (float) livingEntity.hurtTime - tickDelta;
            float g;
            if (livingEntity.isDead()) {
                g = Math.min((float) livingEntity.deathTime + tickDelta, 20.0F);
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(40.0F - 8000.0F / (g + 200.0F)));
            }

            if (f < 0.0F) {
                return;
            }

            f /= (float) livingEntity.maxHurtTime;
            f = MathHelper.sin(f * f * f * f * 3.1415927F);
            g = livingEntity.getDamageTiltYaw();
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-g));
            float h = (float) ((double) (-f) * 14.0 * this.client.options.getDamageTiltStrength().getValue());
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(h));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(g));
        }

    }

    @Unique
    public void loadProjectionMatrix(Matrix4f projectionMatrix) {
        RenderSystem.setProjectionMatrix(projectionMatrix, VertexSorter.BY_DISTANCE);
    }

    @Unique
    public float getFarPlaneDistance() {
        return this.viewDistance * 4.0F;
    }
}
