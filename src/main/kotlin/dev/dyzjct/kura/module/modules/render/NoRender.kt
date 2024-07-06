package dev.dyzjct.kura.module.modules.render

import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module

object NoRender : Module(
    name = "NoRender",
    langName = "移除渲染",
    description = "Ignore Some Effects",
    category = Category.RENDER
) {
    var noHurtCam = bsetting("NoHurtCam", true)
    val blockLayer = bsetting("BlockLayer", true)
    var totemPops = bsetting("Totem", false)
    var explosions = bsetting("Explosions", true)
    var noArmor = bsetting("noArmor",true)
    var fog = bsetting("Fog", true)
}