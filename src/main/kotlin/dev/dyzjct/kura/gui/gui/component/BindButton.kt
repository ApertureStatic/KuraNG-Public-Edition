package dev.dyzjct.kura.gui.gui.component

import dev.dyzjct.kura.gui.gui.Panel
import dev.dyzjct.kura.module.modules.client.ClickGui
import dev.dyzjct.kura.module.modules.client.Colors
import dev.dyzjct.kura.setting.IntegerSetting
import dev.dyzjct.kura.utils.TimerUtils
import dev.dyzjct.kura.system.render.newfont.FontRenderers
import net.minecraft.client.gui.DrawContext
import org.lwjgl.glfw.GLFW
import java.awt.Color

class BindButton(var module: dev.dyzjct.kura.module.AbstractModule, width: Double, height: Double, father: Panel?) : SettingButton<Int>() {
    private var isListening = false
    private var renderTimer = TimerUtils()

    init {
        this.width = width
        this.height = height
        this.father = father
//        do not use
        value = IntegerSetting("", module, 0, 0, 0)
    }

    override fun render(context: DrawContext, mouseX: Double, mouseY: Double, partialTicks: Float) {
        if (!renderTimer.passed(1200)) {
            FontRenderers.lexend.drawString(
                context.matrices,
                "HoldEnable: ${if (module.holdToEnable) "Yes" else "No"}",
                x + 6,
                y + (height / 2) - (6 / 2f),
                Colors.fontColor.value.rgb,
                true
            )
            return
        }
        if (this.isListening) {
            FontRenderers.lexend.drawString(
                context.matrices, "...", (x + 6.0),
                (y + height / 2 - 6.0 / 2f), Color(-1).rgb
            )
        } else {
            FontRenderers.lexend.drawString(
                context.matrices, "Bind: ${module.getBindName()}",
                (x + 6.0),
                (y + height / 2 - 6 / 2.0), Color(-1).rgb
            )
        }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        super.keyTyped(typedChar, keyCode)
        if (isListening) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_SPACE || keyCode == GLFW.GLFW_KEY_DELETE || keyCode == ClickGui.bind) {
                module.bind = 0
            } else {
                module.bind = keyCode
            }
            isListening = false
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (!isHovered(mouseX, mouseY)) {
            return false
        }

        when (button) {
            0 -> {
                isListening = !isListening
            }

            2 -> {
                module.holdToEnable = !module.holdToEnable
                renderTimer.reset()
            }

            3 -> {
                if (isListening) {
                    module.bind = GLFW.GLFW_MOUSE_BUTTON_4
                    isListening = false
                }
            }

            4 -> {
                if (isListening) {
                    module.bind = GLFW.GLFW_MOUSE_BUTTON_5
                    isListening = false
                }
            }

            else -> {
                return false
            }
        }
        return true
    }
}
