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
    val autoToggle by bsetting("[CA/AA]AutoToggle", false)
    val mainToggle by msetting("MainToggle",MainToggle.Crystal)
    val targetRange by dsetting("TargetRange", 8.0, 0.0, 12.0)
    val placeRange by dsetting("PlaceRange", 6.0, 0.0, 8.0)
    val attackRange by dsetting("AttackRange", 6.0, 0.0, 8.0)
    val interactRange by dsetting("InteractRange", 6.0, 0.0, 8.0)
    val kaRange by dsetting("KARange", 6.0, 0.0, 8.0)

    enum class SpoofMode {
        Normal, Swap, China
    }

    enum class MainToggle {
        Crystal,Anchor
    }
}