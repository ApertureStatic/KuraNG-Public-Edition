package dev.dyzjct.kura.module.modules.client

import base.system.util.interfaces.DisplayEnum
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.module.modules.client.UiSetting.Theme
import dev.dyzjct.kura.module.modules.client.UiSetting.theme

object LoadingMenu: Module(
    name = "LoadingMenu",
    langName = "加载界面",
    category = Category.CLIENT,
    description = "Loading menu",
    safeModule = true
) {

    val mode = msetting("Mode", LoadingMenuMode.Kura)

    enum class LoadingMenuMode(override val displayName: CharSequence) : DisplayEnum {

        XGP("XGP"),
        GENSHIN_IMPACT("Genshin"),
        Kura("kura"),
        Nullpoint("nullpoint"),
        Ayachinene("0721")
    }

}