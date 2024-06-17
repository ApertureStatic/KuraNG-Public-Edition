package dev.dyzjct.kura.mixin.render;

import dev.dyzjct.kura.mixins.IHeldItemRenderer;
import dev.dyzjct.kura.module.modules.render.HandView;
import dev.dyzjct.kura.event.events.render.ItemRenderEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public class MixinHeldItemRenderer implements IHeldItemRenderer {
    @Shadow
    private ItemStack mainHand;
    @Shadow
    private ItemStack offHand;
    @Shadow
    private float equipProgressMainHand;
    @Shadow
    private float equipProgressOffHand;

    @ModifyArg(method = "updateHeldItems", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;clamp(FFF)F", ordinal = 2), index = 0)
    private float modifyEquipProgressMainHand(float value) {
        if (MinecraftClient.getInstance().player == null) return 0f;
        float f = MinecraftClient.getInstance().player.getAttackCooldownProgress(1f);
        float modified = (HandView.INSTANCE.getOldSwing() && HandView.INSTANCE.isEnabled()) ? 1 : f * f * f;

        return (showSwapping(mainHand, MinecraftClient.getInstance().player.getMainHandStack()) ? modified : 0) - equipProgressMainHand;
    }

    @Unique
    private boolean showSwapping(ItemStack stack1, ItemStack stack2) {
        return ItemStack.areEqual(stack1, stack2);
    }

    @Inject(method = "renderFirstPersonItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"))
    private void onRenderItem(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        new ItemRenderEvent(matrices, hand).post();
    }

    @Override
    public float getEquippedProgressMainHand() {
        return equipProgressMainHand;
    }

    @Override
    public void setEquippedProgressMainHand(float var1) {
        equipProgressMainHand = var1;
    }

    @Override
    public float getEquippedProgressOffHand() {
        return equipProgressOffHand;
    }

    @Override
    public void setEquippedProgressOffHand(float var1) {
        equipProgressOffHand = var1;
    }

    @Override
    public void setItemStackMainHand(ItemStack var1) {
        mainHand = var1;
    }

    @Override
    public void setItemStackOffHand(ItemStack var1) {
        offHand = var1;
    }
}
