package dev.dyzjct.kura.module.modules.player

import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module

object Reach :Module(name = "Reach", langName = "长臂猿", description = "reach", category = Category.PLAYER) {
    val range = fsetting("Range",6.0f,1.0f,6.0f)
}