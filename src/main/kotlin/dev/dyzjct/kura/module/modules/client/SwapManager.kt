package dev.dyzjct.kura.module.modules.client

import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module

object SwapManager : Module(
    name = "HotbarManager",
    langName = "切手管理",
    category = Category.CLIENT
) {
    val mode = msetting("SpoofMode", SpoofMode.Normal)

    enum class SpoofMode {
        Normal, Swap, PickUP
    }
}