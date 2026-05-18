package dev.dyzjct.kura.mixin.network;

import dev.dyzjct.kura.manager.RotationManager;
import dev.dyzjct.kura.mixin.accessor.ExplosionS2CPacketAccessor;
import dev.dyzjct.kura.module.modules.client.MovementFix;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class MixinClientPlayNetworkHandler {

    @Inject(method = "onExplosion", at = @At("HEAD"))
    private void onExplodeHook(ExplosionS2CPacket packet, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        // 通过包装类或直接投射检查，调用刚才为你预留的自定义开关
        // 此处利用包装层确保在安全上下文中调用
        // 🌟 调用你在 RotationManager 里定义的自定义开关
        if (MovementFix.INSTANCE.isEnabled()) {
            float vX = packet.getPlayerVelocityX();
            float vZ = packet.getPlayerVelocityZ();

            // 如果爆炸本身没有对玩家产生水平位移，则不需要修正
            if (vX == 0f && vZ == 0f) return;

            // 🛠️ 核心：利用旋转矩阵，将绝对维度的爆炸击退向量，强行校准到假视角相对轴上
            float clientYaw = mc.player.getYaw();
            float packetYaw = RotationManager.INSTANCE.getYaw_value();

            // 计算本地真朝向与即将发包的假朝向之间的夹角差值
            double angleDiff = Math.toRadians((clientYaw - packetYaw));
            float cos = (float) Math.cos(angleDiff);
            float sin = (float) Math.sin(angleDiff);

            // 逆矩阵映射转换
            float correctedX = vX * cos - vZ * sin;
            float correctedZ = vZ * cos + vX * sin;

            // 将修正后的击退动量重新塞入数据包，这样接下来的原版物理引擎就会正确计算击退
            ExplosionS2CPacketAccessor accessor = (ExplosionS2CPacketAccessor) packet;
            accessor.setPlayerVelocityX(correctedX);
            accessor.setPlayerVelocityZ(correctedZ);

            // 提示：Y轴（垂直高度击退）由于不受 Yaw 轴水平面旋转的影响，原版数值直接保留即可
        }
    }
}