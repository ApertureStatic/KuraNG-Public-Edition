package dev.dyzjct.kura.module.modules.movement

import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import base.utils.concurrent.threads.runSafe

object AutoWalk: Module(
    name = "AutoWalk",
    langName = "自动行走",
    category = Category.MOVEMENT,
    description = "Automatic walking"
) {

    init {
        onMotion {
            mc.options.forwardKey.isPressed = true
        }
    }

    override fun onDisable() {
        runSafe {
            mc.options.forwardKey.isPressed = false
        }
    }

}