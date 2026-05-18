package dev.dyzjct.kura.mixin.accessor;

import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ExplosionS2CPacket.class)
public interface ExplosionS2CPacketAccessor {
    @Accessor("playerVelocityX")
    void setPlayerVelocityX(float x);

    @Accessor("playerVelocityY")
    void setPlayerVelocityY(float y);

    @Accessor("playerVelocityZ")
    void setPlayerVelocityZ(float z);
}