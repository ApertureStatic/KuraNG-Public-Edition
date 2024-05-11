package dev.dyzjct.kura.module.modules.movement

import base.utils.screen.ScreenUtils.notWhiteListScreen
import dev.dyzjct.kura.gui.clickgui.ClickGuiScreen
import dev.dyzjct.kura.gui.clickgui.HudEditorScreen
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.module.modules.client.CombatSystem
import net.minecraft.client.util.InputUtil

object GUIMove : Module(
    name = "GUIMove",
    langName = "背包移动",
    category = Category.MOVEMENT,
    description = "Moving when Gui is open.",
    safeModule = true
) {

    init {
        onMotion {
            val currentScreen = mc.currentScreen ?: return@onMotion

            if (ClickGuiScreen.inSearching && currentScreen is ClickGuiScreen) {
                return@onMotion
            }

            if (currentScreen.notWhiteListScreen()) {
                return@onMotion
            }

            if (currentScreen is ClickGuiScreen || currentScreen is HudEditorScreen || CombatSystem.combatMode.value == CombatSystem.CombatMode.Strong) {
                for (k in arrayOf(
                    mc.options.forwardKey,
                    mc.options.backKey,
                    mc.options.leftKey,
                    mc.options.rightKey,
                    mc.options.jumpKey,
                    mc.options.sprintKey
                )) {
                    k.isPressed = InputUtil.isKeyPressed(
                        mc.window.handle,
                        InputUtil.fromTranslationKey(k.boundKeyTranslationKey).code
                    )
                }
            }
        }
    }
}