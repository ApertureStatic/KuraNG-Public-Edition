package dev.dyzjct.kura.module.modules.movement

import base.utils.concurrent.threads.runSafe
import dev.dyzjct.kura.Kura
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.setting.BooleanSetting
import net.minecraft.block.CobwebBlock

object FastWeb : Module(name = "FastWeb", "防蜘蛛网", category = Category.MOVEMENT) {

    val mode = msetting("Mode", Mode.Timer)
    val timer by fsetting("Timer", 7.0f, 0.0f, 10.0f)

    init {
        mode.onChange<BooleanSetting> { _ ->
            if (mode.value == Mode.Vanilla) Kura.TICK_TIMER = 1f
        }
        onMotion {
            if (mode.value == Mode.Timer) {
                if (world.getBlockState(player.blockPos).block is CobwebBlock) {
                    Kura.TICK_TIMER = timer
                }
            }
        }
    }

    override fun onDisable() {
        runSafe {
            if (mode.value == Mode.Timer) Kura.TICK_TIMER = 1f
        }
    }

    enum class Mode {
        Timer, Vanilla
    }
}