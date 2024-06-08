package dev.dyzjct.kura.module.modules.player.disabler


import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module

object Disabler: Module(
    name = "Disabler",
    langName = "禁用包绕过",
    category = Category.PLAYER
) {

    val burrowBypass by bsetting("BurrowBypass", false)
    val noServerMove by bsetting("NoServerMove", false).isTrue { burrowBypass }
    val tickPacket by bsetting("TickPacket", false).isTrue { burrowBypass }
    val posSync by bsetting("PosSync", false).isTrue { burrowBypass }

    override fun onEnable() {
        BurrowBypass.enable()
    }

    override fun onDisable() {
        BurrowBypass.disable()
    }

}