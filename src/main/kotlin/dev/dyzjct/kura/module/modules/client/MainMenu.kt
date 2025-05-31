package dev.dyzjct.kura.module.modules.client

import dev.dyzjct.kura.KuraIdentifier
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.system.util.interfaces.DisplayEnum
import net.minecraft.util.Identifier

object MainMenu : Module(
    name = "MainMenu",
    langName = "游戏主界面",
    description = "Main menu.",
    category = Category.CLIENT
) {

    val mode by msetting("Mode", MainMenuMode.Kura)
    val background by msetting("BG", KuraBackground.Kura)
    val rimuru by bsetting("Rimuru", false).enumIs(mode, MainMenuMode.Kura)

    fun getBackground(): Identifier {
        return when (background) {
            KuraBackground.Kura -> KuraIdentifier("background/background.png")

            KuraBackground.Shuna -> KuraIdentifier("background/shuna_bg.png")

            else -> KuraIdentifier("background/background.png")
        }
    }

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