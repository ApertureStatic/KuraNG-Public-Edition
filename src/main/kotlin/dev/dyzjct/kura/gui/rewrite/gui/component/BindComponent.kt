package dev.dyzjct.kura.gui.rewrite.gui.component

import dev.dyzjct.kura.gui.rewrite.gui.render.Alignment
import dev.dyzjct.kura.gui.rewrite.gui.render.DrawDelegate
import dev.dyzjct.kura.gui.rewrite.gui.render.DrawScope
import dev.dyzjct.kura.module.AbstractModule
import dev.dyzjct.kura.module.modules.client.ClickGui
import dev.dyzjct.kura.utils.TimerUtils
import org.lwjgl.glfw.GLFW
import java.awt.Color

class BindComponent(
    private val module: dev.dyzjct.kura.module.AbstractModule,
    override var x: Float,
    override var y: Float,
    override var width: Float,
    override var height: Float,
    override var drawDelegate: DrawDelegate
) : Component, Visible {
    private var waitingForBinding = false
    private var renderTimer = TimerUtils()

    override fun isVisible(): Boolean {
        return true
    }

    override fun DrawScope.render(mouseX: Float, mouseY: Float) {
        val textColor = if (isHovering(mouseX, mouseY)) {
            Color.WHITE.darker()
        } else {
            Color.WHITE
        }

        drawText("Bind", textColor, verticalAlignment = Alignment.CENTER)

        drawText(
            if (!renderTimer.passed(1200)) {
                "HoldEnable: ${if (module.holdToEnable) "Yes" else "No"}"
            } else if (waitingForBinding) {
                "[...]"
            } else {
                module.getBindName()
            }, textColor, horizontalAlignment = Alignment.END, verticalAlignment = Alignment.CENTER
        )
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
        if (!isHovering(mouseX, mouseY)) {
            return false
        }

        when (button) {
            0 -> {
                waitingForBinding = !waitingForBinding
            }

            2 -> {
                if (module.bind != 0) {
                    module.holdToEnable = !module.holdToEnable
                    renderTimer.reset()
                }
            }

            3 -> {
                if (waitingForBinding) {
                    module.bind = GLFW.GLFW_MOUSE_BUTTON_4
                    waitingForBinding = false
                }
            }

            4 -> {
                if (waitingForBinding) {
                    module.bind = GLFW.GLFW_MOUSE_BUTTON_5
                    waitingForBinding = false
                }
            }

            else -> {
                return false
            }
        }

        return true
    }

    override fun keyTyped(keyCode: Int): Boolean {
        if (waitingForBinding) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_SPACE || keyCode == GLFW.GLFW_KEY_DELETE || keyCode == ClickGui.bind) {
                module.bind = 0
            } else {
                module.bind = keyCode
            }
            waitingForBinding = false
            return true
        }

        return false
    }

    override fun guiClosed() {
        waitingForBinding = false
    }
}