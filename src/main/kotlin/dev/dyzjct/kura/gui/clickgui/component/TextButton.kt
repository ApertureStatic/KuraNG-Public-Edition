package dev.dyzjct.kura.gui.clickgui.component

import dev.dyzjct.kura.gui.clickgui.Panel
import dev.dyzjct.kura.setting.StringSetting
import base.system.render.graphic.Render2DEngine
import base.system.render.newfont.FontRenderers
import base.utils.concurrent.threads.runSafe
import base.utils.keyboard.KeyboardUtils
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.util.InputUtil
import org.lwjgl.glfw.GLFW

class TextButton(value: StringSetting, width: Double, height: Double, father: Panel?) : SettingButton<String>() {
    private var listening = false
    private var currentString = ""

    init {
        this.width = width
        this.height = height
        this.father = father
        this.value = value
    }

    override fun render(context: DrawContext, mouseX: Double, mouseY: Double, partialTicks: Float) {
        runSafe {
            FontRenderers.default.drawString(
                context.matrices,
                if (listening) currentString + (if (player.age % 20 == 0) "_" else "") else value.value,
                x + 6.0,
                y + height / 2.0,
                -1
            )
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (!value.isVisible() || !isHovered(mouseX, mouseY)) {
            return false
        }
        if (Render2DEngine.isHovered(
                mouseX,
                mouseY,
                x,
                y,
                width,
                height
            ) && button == 0
        ) {
            listening = !listening
        }
        return true
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (listening) {
            when (keyCode) {
                GLFW.GLFW_KEY_ESCAPE -> {
                    return
                }

                GLFW.GLFW_KEY_ENTER -> {
                    value.value = currentString.ifEmpty { value.defaultValue }
                    currentString = ""
                    listening = !listening
                    return
                }

                GLFW.GLFW_KEY_BACKSPACE -> {
                    currentString = NumberSlider.removeLastChar(currentString)
                    return
                }

                GLFW.GLFW_KEY_SPACE -> {
                    currentString += " "
                    return
                }
            }
            if (GLFW.glfwGetKeyName(keyCode, 0) == null) return
            if (!typedChar.toString().contains("\u0000")) {
                this.currentString += typedChar
            } else if (KeyboardUtils.isCtrlDown && KeyboardUtils.isDown(InputUtil.GLFW_KEY_V)) {
                this.currentString += KeyboardUtils.clipboardString
            } else {
                this.currentString += GLFW.glfwGetKeyName(keyCode, 0)?.replace("null", "") ?: ""
            }
        }
    }
}