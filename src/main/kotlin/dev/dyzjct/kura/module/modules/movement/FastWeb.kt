package dev.dyzjct.kura.module.modules.movement

import dev.dyzjct.kura.Kura
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import base.utils.concurrent.threads.runSafe
import net.minecraft.block.CobwebBlock

object FastWeb : Module(name = "FastWeb", "防蜘蛛网", category = Category.MOVEMENT) {

    val mode by msetting("Mode", Mode.Timer)
    val timer by fsetting("Timer", 7.0f, 0.0f, 10.0f)

    init {
        onMotion {
            if (mode == Mode.Timer) {
                if (world.getBlockState(player.blockPos).block is CobwebBlock) {
                    Kura.TICK_TIMER = timer
                } else Kura.TICK_TIMER = 1f
            }
        }
    }

    override fun onDisable() {
        runSafe {
            Kura.TICK_TIMER = 1f
        }
    }

    enum class Mode {
        Timer, Vanilla
    }
}