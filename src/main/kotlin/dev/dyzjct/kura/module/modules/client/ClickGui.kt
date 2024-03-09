package dev.dyzjct.kura.module.modules.client

import dev.dyzjct.kura.gui.clickgui.GUIRender
import dev.dyzjct.kura.gui.clickgui.guis.ClickGuiScreen
import dev.dyzjct.kura.gui.rewrite.gui.MelonClickGui
import dev.dyzjct.kura.gui.rewrite.gui.MelonHudEditor
import dev.dyzjct.kura.manager.FileManager.saveAll
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.module.hud.Image
import melon.utils.concurrent.threads.runSafe
import net.minecraft.client.util.InputUtil

object ClickGui : Module(
    name = "ClickGUI",
    langName = "ClickGUI",
    category = Category.CLIENT,
    keyCode = InputUtil.GLFW_KEY_U,
    visible = true
) {
    var chinese = bsetting("ChineseUI", false)
    var notification by bsetting("Notification", false)
    var chat = bsetting("ToggleChat", true)
    var screen: ClickGuiScreen = ClickGuiScreen()

    override fun onEnable() {
        if (mc.currentScreen == MelonHudEditor) {
            MelonHudEditor.close()
        }

        runSafe {
            if (mc.currentScreen is MelonClickGui || mc.currentScreen is ClickGuiScreen) {
                return
            }

            if (UiSetting.enableNewUi) {
                mc.setScreen(MelonClickGui)
            } else {
                GUIRender.init()
                mc.setScreen(screen)
            }

            Image.startTime = System.currentTimeMillis()
        }
    }

    override fun onDisable() {
        runSafe {
            if (mc.currentScreen is ClickGuiScreen || mc.currentScreen is MelonClickGui) {
                mc.setScreen(null)
            }
            saveAll()
        }
    }
}
