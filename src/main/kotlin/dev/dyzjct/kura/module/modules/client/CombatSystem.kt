package dev.dyzjct.kura.module.modules.client

import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module

object CombatSystem : Module(
    name = "CombatSystem",
    langName = "战斗系统",
    category = Category.CLIENT
) {
    val mode = msetting("SpoofMode", SpoofMode.Normal)
    val autoSwitch by bsetting("AutoSwitch", false)
    val eating by bsetting("EatingPause", true)

    enum class SpoofMode {
        Normal, Swap, China
    }
}