package dev.dyzjct.kura.module.modules.player

import dev.dyzjct.kura.Kura
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module

object Timer : Module(name = "Timer", langName = "变速齿轮", category = Category.PLAYER) {
    private val speed = fsetting("Speed", 1.2f, 1f, 10f)

    init {
        onMotion {
            Kura.TICK_TIMER = speed.value
        }
    }

    override fun onDisable() {
        Kura.TICK_TIMER = 1f
    }
}