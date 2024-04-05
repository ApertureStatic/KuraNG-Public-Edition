package dev.dyzjct.kura.module.modules.render

import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module

object AntiPlayerSwing : Module(name = "AntiPlayerSwing", langName = "防止玩家摆动", category = Category.RENDER) {
    val arm by bsetting("Arm", false)
    val leg by bsetting("Leg", false)
}