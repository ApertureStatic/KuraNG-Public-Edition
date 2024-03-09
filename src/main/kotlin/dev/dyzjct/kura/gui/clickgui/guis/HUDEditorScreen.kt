package dev.dyzjct.kura.gui.clickgui.guis

import dev.dyzjct.kura.gui.clickgui.HUDRender
import dev.dyzjct.kura.module.ModuleManager.getModuleByClass
import dev.dyzjct.kura.module.modules.client.ClickGui
import dev.dyzjct.kura.module.modules.client.HUDEditor
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text

class HUDEditorScreen : Screen(Text.empty()) {
    override fun shouldPause(): Boolean {
        return false
    }

    override fun close() {
        if (getModuleByClass(HUDEditor::class.java).isEnabled) {
            getModuleByClass(HUDEditor::class.java).disable()
            ClickGui.disable()
        }
        HUDRender.onClose()
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, partialTicks: Float) {
        HUDRender.render(context, mouseX, mouseY, partialTicks)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        HUDRender.mouseClicked(mouseX, mouseY, button)
        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        HUDRender.mouseReleased(mouseX, mouseY, button)
        return super.mouseReleased(mouseX, mouseY, button)
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        HUDRender.keyTyped('\u0000', keyCode)
        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun charTyped(typedChar: Char, keyCode: Int): Boolean {
        HUDRender.keyTyped(typedChar, keyCode)
        return super.charTyped(typedChar, keyCode)
    }
}
