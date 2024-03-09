package dev.dyzjct.kura.mixin.world;

import base.events.WorldEvent;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientWorld.class)
public abstract class MixinClientWorld {
    @Shadow
    public abstract @Nullable Entity getEntityById(int id);

    @Inject(method = "addEntityPrivate", at = @At("RETURN"))
    public void onAddEntity(int id, Entity entity, CallbackInfo ci) {
        WorldEvent.Entity.Add event = new WorldEvent.Entity.Add(entity);
        event.post();
    }

    @Inject(method = "removeEntity", at = @At(value = "HEAD"))
    public void onRemoveEntity(int entityId, Entity.RemovalReason removalReason, CallbackInfo ci) {
        Entity entity = this.getEntityById(entityId);
        if (entity != null) {
            WorldEvent.Entity.Remove event = new WorldEvent.Entity.Remove(entity);
            event.post();
        }
    }
}
