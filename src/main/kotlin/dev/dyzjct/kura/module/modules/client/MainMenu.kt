package dev.dyzjct.kura.module.modules.client

import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.system.util.interfaces.DisplayEnum

object MainMenu : Module(
    name = "MainMenu",
    langName = "游戏主界面",
    description = "Main menu.",
    category = Category.CLIENT
) {

    val mode = msetting("Mode", MainMenuMode.Kura)
    val background = msetting("BG",KuraBackground.Kura)
    val rimuru by bsetting("Rimuru", false).enumIs(mode, MainMenuMode.Kura)

    @Suppress("UNUSED")
    enum class MainMenuMode(override val displayName: CharSequence) : DisplayEnum {
        Kura("kura"),
        Opan("Opan"),
    }

    @Suppress("UNUSED")
    enum class KuraBackground(override val displayName: CharSequence) : DisplayEnum {
        Kura("kura"),
        Shuna("Shuna"),
    }
}