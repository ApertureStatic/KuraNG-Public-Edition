package dev.dyzjct.kura.module.modules.client

import base.utils.concurrent.threads.runSafe
import dev.dyzjct.kura.gui.clickgui.ClickGuiScreen
import dev.dyzjct.kura.gui.clickgui.HudEditorScreen
import dev.dyzjct.kura.manager.FileManager.saveAll
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.module.hud.Image
import dev.dyzjct.kura.utils.animations.Easing
import net.minecraft.client.util.InputUtil

object ClickGui : Module(
    name = "ClickGUI",
    langName = "ClickGUI",
    category = Category.CLIENT,
    keyCode = InputUtil.GLFW_KEY_RIGHT_SHIFT,
    safeModule = true,
    visible = true
) {
    var chinese = bsetting("ChineseUI", false)
    var chat = bsetting("ToggleChat", true)

    override fun onEnable() {
        if (mc.currentScreen == HudEditorScreen) {
            HudEditorScreen.close()
        }

        runSafe {
            if (mc.currentScreen is ClickGuiScreen) {
                return
            }

            mc.setScreen(ClickGuiScreen)

            Image.startTime = System.currentTimeMillis()
        }
    }

    override fun onDisable() {
        runSafe {
            if (Easing.IN_QUAD.inc(
                    Easing.toDelta(
                        Image.startTime,
                        UiSetting.animationLength
                    )
                ) >= 1.0f
            ) Image.startTime = System.currentTimeMillis()
            if (mc.currentScreen is ClickGuiScreen) {
                mc.setScreen(null)
            }
            saveAll(CombatSystem.combatMode.name)
        }
    }
}
