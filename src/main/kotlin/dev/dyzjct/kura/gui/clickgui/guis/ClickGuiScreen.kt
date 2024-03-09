package dev.dyzjct.kura.gui.clickgui.guis

import dev.dyzjct.kura.gui.clickgui.GUIRender
import dev.dyzjct.kura.module.ModuleManager.getModuleByClass
import dev.dyzjct.kura.module.modules.client.ClickGui
import dev.dyzjct.kura.module.modules.client.HUDEditor
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text

open class ClickGuiScreen(title: Text = Text.empty()) : Screen(title) {
    override fun shouldPause(): Boolean {
        return false
    }

    public override fun init() {
        /*
        val client = MinecraftClient.getInstance()
        val gameRenderer = client.gameRenderer
        val accessorShaders = (gameRenderer as AccessorGameRenderer)
        val shaders = accessorShaders.programs

        accessorShaders.postProcessor?.close()
        accessorShaders.invokeLoadPostProcessor(shaders[18])

         */
    }

    override fun close() {
        /*
        val client = MinecraftClient.getInstance()
        val gameRenderer = client.gameRenderer
        val accessorShaders = (gameRenderer as AccessorGameRenderer)
        accessorShaders.postProcessor?.close()

         */
        GUIRender.onClose()
        if (getModuleByClass(ClickGui::class.java).isEnabled) {
            getModuleByClass(ClickGui::class.java).disable()
            HUDEditor.disable()
        }
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, partialTicks: Float) {
        GUIRender.render(context, mouseX, mouseY, partialTicks)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, mouseButton: Int): Boolean {
        GUIRender.onMouseClicked(mouseX, mouseY, mouseButton)
        return super.mouseClicked(mouseX, mouseY, mouseButton)
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        GUIRender.keyTyped('\u0000', keyCode)
        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun charTyped(typedChar: Char, keyCode: Int): Boolean {
        GUIRender.keyTyped(typedChar, keyCode)
        return super.charTyped(typedChar, keyCode)
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, state: Int): Boolean {
        GUIRender.onMouseReleased(mouseX, mouseY, state)
        return super.mouseReleased(mouseX, mouseY, state)
    }
}
