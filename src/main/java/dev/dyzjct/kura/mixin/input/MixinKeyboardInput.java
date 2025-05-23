package dev.dyzjct.kura.mixin.input;

import dev.dyzjct.kura.event.events.input.KeyboardInputEvent;
import dev.dyzjct.kura.event.events.input.KeyboardTickEvent;
import dev.dyzjct.kura.event.events.input.MovementInputEvent;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public class MixinKeyboardInput extends Input {

    @Inject(method = "tick", at = @At("RETURN"))
    public void tick(boolean slowDownm, float slowDownFactor, CallbackInfo ci) {
        MovementInputEvent event = new MovementInputEvent(movementForward, movementSideways);
        event.post();
        movementForward = event.getForward();
        movementSideways = event.getSideways();
    }

    @Inject(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/input/KeyboardInput;sneaking:Z", shift = At.Shift.BEFORE), cancellable = true)
    private void onSneak(boolean slowDown, float slowDownFactor, CallbackInfo ci) {
        KeyboardInputEvent event = new KeyboardInputEvent();
        event.post();
        if (event.getCancelled()) ci.cancel();
    }

    @Inject(method = "tick", at = @At(value = "TAIL"))
    private void tick$TAIL(CallbackInfo info) {
        KeyboardTickEvent event = new KeyboardTickEvent(movementForward, movementSideways);
        event.post();
        if (event.getCancelled()) {
            this.movementSideways = event.getMovementSideways();
            this.movementForward = event.getMovementForward();
        }
    }
}
