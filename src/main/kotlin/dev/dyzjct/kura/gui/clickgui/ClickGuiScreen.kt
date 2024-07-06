package dev.dyzjct.kura.gui.clickgui

import base.utils.chat.ChatUtil
import dev.dyzjct.kura.gui.clickgui.component.Panel
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.ModuleManager
import dev.dyzjct.kura.module.modules.client.ClickGui
import dev.dyzjct.kura.module.modules.client.UiSetting
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import org.lwjgl.glfw.GLFW
import java.awt.Color

object ClickGuiScreen : GuiScreen() {
    var inSearching = false
    private var queryString = ""
    private val mc = MinecraftClient.getInstance()

    init {
        var offsetX = 5f
        for (category in Category.entries) {
            if (category.isHUD || category == Category.HIDDEN) {
                continue
            }

            val modules = ModuleManager.moduleList.filter { it.moduleCategory == category }

            elements.add(
                Panel(
                    modules, category, this, offsetX, 5f, 105f, 15f,
                    DrawDelegateSelector.currentDrawDelegate
                )
            )

            offsetX += 115f
        }

        container.addAll(elements)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)

        if (queryString.isNotEmpty()) {
            val currentScreen = mc.currentScreen
            val drawDelegate = container.drawDelegate
            if (currentScreen != null) {
                val textWidth = drawDelegate.getStringWidth(queryString)
                val x = (currentScreen.width - textWidth) / 2
                val y = (currentScreen.height - drawDelegate.textHeight) / 2

                val secondaryColor = UiSetting.getThemeSetting().secondary
                drawDelegate.drawReact(
                    context.matrices,
                    x - 2,
                    y - 2,
                    textWidth + 4,
                    drawDelegate.textHeight + 4,
                    Color(secondaryColor.red, secondaryColor.green, secondaryColor.blue, 255)
                )

                drawDelegate.drawText(
                    context.matrices, queryString, x, y, Color.WHITE
                )
            }
        }
    }

    override fun keyTyped(keyCode: Int): Boolean {
        val isReceived = super.keyTyped(keyCode)

        if (isReceived) {
            return true
        }

        if (keyCode == 257) inSearching = true

        if (UiSetting.searchDebug) ChatUtil.sendMessage(keyCode.toString())

        if (!inSearching) return false

        if (UiSetting.disableSearch) {
            inSearching = false
            return false
        }

//        ESC -> 256
        if (keyCode == 256) {
            if (queryString.isNotEmpty()) {
                queryString = ""
                updatePanelModule()
                inSearching = false
                return true
            } else {
                inSearching = false
                return false
            }
        }

        when (keyCode) {
            GLFW.GLFW_KEY_BACKSPACE -> {
                queryString =
                    queryString.dropLast(1)
                updatePanelModule()
            }

            else -> {
                val char = GLFW.glfwGetKeyName(keyCode, 0)?.replace("null", "") ?: ""
                queryString += char
                updatePanelModule()
            }
        }

        return true
    }

    override fun close() {
        super.close()
        ClickGui.disable()
    }

    private fun updatePanelModule() {
        elements.mapNotNull { it as? Panel }.forEach {
            it.filterModulesByName { moduleName ->
                moduleName.lowercase().contains(queryString)
            }
        }
    }

    fun updatePanelModuleOnModeChange() {
        elements.mapNotNull { it as? Panel }.forEach {
            it.getModules()
        }
    }
}