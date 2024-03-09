package dev.dyzjct.kura.module.modules.client

import dev.dyzjct.kura.gui.clickgui.guis.HUDEditorScreen
import dev.dyzjct.kura.gui.rewrite.gui.MelonClickGui
import dev.dyzjct.kura.gui.rewrite.gui.MelonHudEditor
import dev.dyzjct.kura.manager.FileManager.saveAll
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.module.hud.Image
import base.utils.Wrapper

object HUDEditor : Module(name = "HUDEditor", langName = "HUD编辑器", category = Category.CLIENT, visible = false) {
    var screen: HUDEditorScreen = HUDEditorScreen()

    override fun onEnable() {
        if (mc.currentScreen == MelonClickGui) {
            MelonClickGui.close()
        }

        if (Wrapper.player != null && mc.currentScreen !is HUDEditorScreen && mc.currentScreen !is MelonHudEditor) {
            if (UiSetting.enableNewUi) {
                mc.setScreen(MelonHudEditor)
            } else {
                mc.setScreen(screen)
            }
            Image.startTime = System.currentTimeMillis()
        }
    }

    override fun onDisable() {
        if (mc.currentScreen is HUDEditorScreen || mc.currentScreen is MelonHudEditor) {
            mc.setScreen(null)
        }
        saveAll()
    }
}
