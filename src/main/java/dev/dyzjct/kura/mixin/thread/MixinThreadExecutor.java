package dev.dyzjct.kura.mixin.thread;

import net.minecraft.util.thread.ThreadExecutor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ThreadExecutor.class)
public class MixinThreadExecutor {
    @Shadow
    protected void runTasks() {
    }
}
