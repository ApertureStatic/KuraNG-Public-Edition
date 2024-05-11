package dev.dyzjct.kura.module.modules.render

import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module

object CustomFov : Module(
    name = "CustomFov",
    langName = "自定义Fov",
    category = Category.RENDER,
    description = "Custom Minecraft Fov",
    safeModule = true
) {
    var fov = dsetting("Fov", 120.0, 0.0, 160.0)
    var itemFov = bsetting("ItemFov", false)
    var itemFovModifier = dsetting("ItemModifier", 120.0, 0.0, 358.0).isTrue(itemFov)
}