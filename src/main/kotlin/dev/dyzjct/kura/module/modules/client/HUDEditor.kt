package dev.dyzjct.kura.module.modules.client

import base.utils.Wrapper
import dev.dyzjct.kura.gui.clickgui.ClickGuiScreen
import dev.dyzjct.kura.gui.clickgui.HudEditorScreen
import dev.dyzjct.kura.manager.FileManager.saveAll
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.module.hud.Image
import dev.dyzjct.kura.utils.animations.Easing
import net.minecraft.client.util.InputUtil

object HUDEditor : Module(
    name = "HUDEditor",
    langName = "HUD编辑器",
    category = Category.CLIENT,
    visible = false,
    keyCode = InputUtil.GLFW_KEY_GRAVE_ACCENT,
    safeModule = true
) {

    override fun onEnable() {
        if (mc.currentScreen == ClickGuiScreen) {
            ClickGuiScreen.close()
        }

        if (Wrapper.player != null && mc.currentScreen !is HudEditorScreen) {
            mc.setScreen(HudEditorScreen)
            Image.startTime = System.currentTimeMillis()
        }
    }

    override fun onDisable() {
        if (Easing.IN_QUAD.inc(
                Easing.toDelta(
                    Image.startTime,
                    UiSetting.animationLength
                )
            ) >= 1.0f
        ) Image.startTime = System.currentTimeMillis()
        if (mc.currentScreen is HudEditorScreen) {
            mc.setScreen(null)
        }
        saveAll(CombatSystem.combatMode.value.name)
    }
}
