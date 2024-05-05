package dev.dyzjct.kura.gui.gui

import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.modules.client.CombatSystem
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text
import java.util.function.Consumer

object GUIRender : Screen(Text.empty()) {
    var panels = ArrayList<Panel>()

    fun onCall() {
        var startX = 5.0
        for (category in Category.entries) {
            if (category.isHUD || category == Category.HIDDEN) continue
            panels.add(Panel(category, startX, 5.0, 110.0, 15.0))
            startX += 115.0
        }
    }

    public override fun init() {
        super.init()
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, partialTicks: Float) {
        context.matrices.push()
        panels.forEach {
            it.drawScreen(context, mouseX.toDouble(), mouseY.toDouble(), partialTicks)
        }
        context.matrices.pop()
    }

    fun onMouseClicked(mouseX: Double, mouseY: Double, mouseButton: Int) {
        for (panel in panels) {
            if (panel.mouseClicked(mouseX, mouseY, mouseButton)) {
                return
            }
            if (!panel.extended) continue
            for (part in panel.elements.filter { CombatSystem.combatMode.value == CombatSystem.CombatMode.Strong || it.module.isSafe}) {
                if (part.mouseClicked(mouseX, mouseY, mouseButton)) {
                    return
                }
                if (!part.isExtended) continue
                for (component in part.settings) {
                    if (!component.value.isVisible() || !component.mouseClicked(mouseX, mouseY, mouseButton)) continue
                    return
                }
            }
        }
    }

    fun keyTyped(typedChar: Char, keyCode: Int) {
        //if (keyCode == 1)  ClickGui.disable()
        panels.forEach(Consumer { panel: Panel -> panel.keyTyped(typedChar, keyCode) })
    }

    fun onMouseReleased(mouseX: Double, mouseY: Double, state: Int) {
        panels.forEach(Consumer { panel: Panel -> panel.mouseReleased(mouseX, mouseY, state) })
    }

    fun mouseDrag(dWheel: Double) {
        if (dWheel < 0) {
            panels.forEach { it.y -= 10 }
        } else if (dWheel > 0) {
            panels.forEach { it.y += 10 }
        }
    }

    fun getPanelByName(name: String): Panel? {
        var getPane: Panel? = null
        for (panel in panels) {
            if (panel.category.name != name) continue
            getPane = panel
        }
        return getPane
    }

    fun onClose() {
        panels.forEach {
            it.onClose()
        }
    }
}
