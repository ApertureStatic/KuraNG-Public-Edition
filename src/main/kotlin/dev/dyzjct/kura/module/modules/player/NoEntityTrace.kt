package dev.dyzjct.kura.module.modules.player

import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module

object NoEntityTrace : Module(name = "NoEntityTrace", category = Category.PLAYER) {
    var pickaxeOnly = bsetting("PickaxeOnly", true)
    var noSword = bsetting("NoSword", true)
}