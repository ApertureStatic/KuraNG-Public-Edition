package dev.dyzjct.kura.gui.gui.component

import dev.dyzjct.kura.gui.gui.Panel
import dev.dyzjct.kura.setting.DoubleSetting
import dev.dyzjct.kura.setting.FloatSetting
import dev.dyzjct.kura.setting.IntegerSetting
import dev.dyzjct.kura.setting.Setting
import dev.dyzjct.kura.utils.animations.MathUtils
import dev.dyzjct.kura.system.render.graphic.Render2DEngine
import dev.dyzjct.kura.system.render.newfont.FontRenderers
import net.minecraft.client.gui.DrawContext
import net.minecraft.util.math.MathHelper
import org.lwjgl.glfw.GLFW
import java.awt.Color

class NumberSlider<T: Number>(value: Setting<T>, width: Double, height: Double, father: Panel?) : SettingButton<T>() {
    private var animation = 0f
    private var stranimation = 0.0
    private var dragging = false
    private var listening = false
    private var stringNumber = ""

    private var min = 0f
    private var max = 0f

    init {
        this.width = width
        this.height = height
        this.father = father
        this.value = value
        when (value) {
            is IntegerSetting -> {
                min = (value as IntegerSetting).min.toFloat()
                max = (value as IntegerSetting).max.toFloat()
            }

            is DoubleSetting -> {
                min = (value as DoubleSetting).min.toFloat()
                max = (value as DoubleSetting).max.toFloat()
            }

            is FloatSetting -> {
                min = (value as FloatSetting).min
                max = (value as FloatSetting).max
            }
        }
    }


    override fun render(context: DrawContext, mouseX: Double, mouseY: Double, partialTicks: Float) {
        height = 18.0
        val currentPos = (((value.value as Number).toFloat() - min) / (max - min)).toDouble()
        stranimation += ((value.value as Number).toFloat() * 100 / 100 - stranimation) / 2.0
        animation = Render2DEngine.scrollAnimate(animation, currentPos.toFloat(), .5f)
        val matrixStack = context.matrices
        var stringValue = ""
        when (value) {
            is FloatSetting -> {
                stringValue = MathUtils.round(value.value as Float, 2).toString()
            }

            is DoubleSetting -> {
                stringValue = MathUtils.round(value.value as Double, 2).toString()
            }

            is IntegerSetting -> {
                stringValue = MathUtils.round((value.value as Int).toFloat(), 2).toString()
            }
        }
        FontRenderers.default.drawString(matrixStack, value.name, (x + 6.0), (y + 4.0), Color(-1).rgb)

        if (!listening) {
            FontRenderers.default.drawString(
                matrixStack,
                stringValue,
                (x + width - 6.0 - FontRenderers.default.getStringWidth(stringValue)),
                y + 5.0,
                Color(-1).rgb
            )
        } else {
            if (stringNumber == "") {
                FontRenderers.default.drawString(
                    matrixStack,
                    "...",
                    (x + width - 6.0 - FontRenderers.default.getStringWidth(stringValue)),
                    y + 5.0,
                    Color(-1).rgb
                )
            } else {
                FontRenderers.default.drawString(
                    matrixStack,
                    stringNumber,
                    (x + width - 6.0 - FontRenderers.default.getStringWidth(stringValue)),
                    y + 5.0,
                    Color(-1).rgb
                )
            }
        }

        Render2DEngine.drawRound(
            matrixStack,
            (x + 6).toFloat(), (y + height - 6).toFloat(), (width - 12).toFloat(), 1f, 0.5f, Color(-0xf1f1f2)
        )
        Render2DEngine.drawRound(
            matrixStack,
            (x + 6).toFloat(),
            (y + height - 6).toFloat(),
            ((width - 12) * animation).toFloat(),
            1f,
            0.5f,
            Color(-0x1e1e1f)
        )
        Render2DEngine.drawRound(
            matrixStack,
            (x + 6 + (width - 16) * animation).toFloat(), (y + height - 7.5f).toFloat(), 4f, 4f, 1.5f, Color(-0x1e1e1f)
        )

        animation = MathUtils.clamp(animation, 0f, 1f)

        if (dragging) setValue(mouseX, x + 7.0, width - 14.0)
    }

    private fun setValue(mouseX: Double, x: Double, width: Double) {
        val diff: Double
        val percentBar: Double
        val valueSetting: Double
        when (value) {
            is IntegerSetting -> {
                val settingValue = value as IntegerSetting
                diff = (settingValue.max.toFloat() - settingValue.min.toFloat()).toDouble()
                percentBar = MathHelper.clamp((mouseX.toFloat() - x) / width, 0.0, 1.0)
                valueSetting = settingValue.min.toFloat() + percentBar * diff
                settingValue.value = valueSetting.toInt()
            }

            is DoubleSetting -> {
                val settingValue = value as DoubleSetting
                diff = (settingValue.max.toFloat() - settingValue.min.toFloat()).toDouble()
                percentBar = MathHelper.clamp((mouseX.toFloat() - x) / width, 0.0, 1.0)
                valueSetting = settingValue.min.toFloat() + percentBar * diff
                settingValue.value = valueSetting
            }

            is FloatSetting -> {
                val settingValue = value as FloatSetting
                diff = (settingValue.max - settingValue.min).toDouble()
                percentBar = MathHelper.clamp((mouseX.toFloat() - x) / width, 0.0, 1.0)
                valueSetting = settingValue.min + percentBar * diff
                settingValue.value = valueSetting.toFloat()
            }
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (!value.isVisible() || !isHovered(mouseX, mouseY)) {
            return false
        }
        if (button == 0 && Render2DEngine.isHovered(
                mouseX,
                mouseY,
                x,
                y,
                width,
                height
            )
        ) {
            dragging = true
        } else if (Render2DEngine.isHovered(
                mouseX,
                mouseY,
                x,
                y,
                width,
                height
            )
        ) {
            stringNumber = ""
            listening = true
        }
        return false
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, state: Int) {
        dragging = false
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (listening) {
            when (keyCode) {
                GLFW.GLFW_KEY_ESCAPE -> {
                    listening = false
                    stringNumber = ""
                    return
                }

                GLFW.GLFW_KEY_ENTER -> {
                    try {
                        this.searchNumber()
                    } catch (e: Exception) {
                        stringNumber = ""
                        listening = false
                    }
                    return
                }

                GLFW.GLFW_KEY_BACKSPACE -> {
                    this.stringNumber = removeLastChar(this.stringNumber)
                    return
                }
            }
            this.stringNumber += GLFW.glfwGetKeyName(keyCode, 0)?.replace("null", "") ?: ""
        }
    }

    private fun searchNumber() {
        when (value) {
            is FloatSetting -> {
                (value as FloatSetting).value = stringNumber.toFloat()
                stringNumber = ""
                listening = false
            }

            is DoubleSetting -> {
                (value as DoubleSetting).value = stringNumber.toDouble()
                stringNumber = ""
                listening = false
            }

            is IntegerSetting -> {
                (value as IntegerSetting).value = stringNumber.toInt()
                stringNumber = ""
                listening = false
            }
        }
    }

    companion object {
        fun removeLastChar(str: String?): String {
            var output = ""
            if (!str.isNullOrEmpty()) {
                output = str.substring(0, str.length - 1)
            }
            return output
        }
    }

    override fun close() {
        animation = 0f
    }
}