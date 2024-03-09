package dev.dyzjct.kura.module.modules.client

import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import base.system.util.interfaces.DisplayEnum

object LoadingMenu: Module(
    name = "LoadingMenu",
    langName = "加载界面",
    category = Category.CLIENT,
    description = "Loading menu"
) {

    val mode = msetting("Mode", LoadingMenuMode.GENSHIN_IMPACT)

    enum class LoadingMenuMode(override val displayName: CharSequence): DisplayEnum {
        XGP("XGP"),
        GENSHIN_IMPACT("Genshin")
    }

}