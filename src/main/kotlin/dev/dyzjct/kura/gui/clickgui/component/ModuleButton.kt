package dev.dyzjct.kura.gui.clickgui.component

import dev.dyzjct.kura.setting.*
import dev.dyzjct.kura.gui.clickgui.Panel
import dev.dyzjct.kura.gui.clickgui.Panel.Companion.toIsVisibleList
import dev.dyzjct.kura.module.AbstractModule
import dev.dyzjct.kura.module.HUDModule
import dev.dyzjct.kura.module.modules.client.ClickGui
import dev.dyzjct.kura.module.modules.client.Colors
import dev.dyzjct.kura.setting.*
import dev.dyzjct.kura.utils.TimerUtils
import dev.dyzjct.kura.utils.animations.Easing
import melon.system.render.graphic.Render2DEngine
import melon.system.render.newfont.FontRenderers
import net.minecraft.client.gui.DrawContext
import java.awt.Color

class ModuleButton(val module: dev.dyzjct.kura.module.AbstractModule, width: Double, height: Double, father: Panel) : Component() {
    private var fade = 0
    private var hovered = false
    private var renderTimer = TimerUtils()
    var settings: MutableList<SettingButton<*>> = ArrayList()
    private val settingHeight: Double
        get() = settings.filter { it.value.isVisible() }.sumOf { it.height }

    var animationHeight = 0.0

    private var startTime = System.currentTimeMillis()
    private val progress: Float
        get() = if (isExtended) {
            Easing.OUT_QUAD.inc(Easing.toDelta(startTime, 400))
        } else {
            Easing.IN_QUAD.dec(Easing.toDelta(startTime, 400))
        }

    init {
        this.width = width
        this.height = height
        this.father = father
        setup()
    }

    private fun setup() {
        for (value in module.settingList) {
            when (value) {
                is BooleanSetting -> settings.add(BooleanButton(value, width, height, father))
                is IntegerSetting -> settings.add(NumberSlider(value, width, height, father))
                is FloatSetting -> settings.add(NumberSlider(value, width, height, father))
                is DoubleSetting -> settings.add(NumberSlider(value, width, height, father))
                is ModeSetting -> settings.add(ModeButton(value as ModeSetting<Enum<*>>, width, height, father))
                is StringSetting -> settings.add(TextButton(value, width, height, father))
                is ColorSetting -> settings.add(ColorPicker(value, father, width, height))
            }
        }
        settings.add(BindButton(module, width, height, father))
    }

    override fun render(context: DrawContext, mouseX: Double, mouseY: Double, partialTicks: Float) {
        if (module is HUDModule) {
            module.renderDelegate(context, mouseX.toFloat(), mouseY.toFloat())
        }

        animationHeight = settingHeight * progress

        hovered = Render2DEngine.isHovered(
            mouseX, mouseY, x, y, width, height
        )

        val centerX = (x + (width / 2f) / 2f)
        val ix = (x + 5f)
        val iy = (y + height / 2f - 6f / 2f)

        Render2DEngine.drawRoundDoubleColor(
            context.matrices,
            (x + 4),
            (y + height - 16),
            (width - 8),
            height + animationHeight,
            3f,
            Render2DEngine.applyOpacity(Colors.getColor(200), 0.8f),
            Render2DEngine.applyOpacity(Colors.getColor(200), 0.8f)
        )
        var color = if (!Colors.enableGuiColor.value) {
            Colors.enableColor.value.rgb
        } else {
            Color(255, 255, 255).rgb
        }
        val fontColor = Colors.fontColor.value.rgb
        if (isHovered(mouseX, mouseY)) {
            //更新渐变颜色
            fade = if (fade < Colors.fadeColor.value.alpha) fade + 5 else Colors.fadeColor.value.alpha
            if (fade > Colors.fadeColor.value.alpha) {
                fade = Colors.fadeColor.value.alpha
            }
            color = color and 0x7F7F7F shl 1
        } else {
            fade = 0
        }
        val newColor = mix(Color(45, 45, 45, 255), Colors.fadeColor.value, fade / 255f).rgb
        //渲染Modules背景
        if (Colors.enableGuiColor.value && (module.isEnabled || module.alwaysEnable)) {
            if (hovered) {
                Render2DEngine.drawRectBlurredShadow(
                    context.matrices,
                    x.toFloat() + 4f,
                    (y + 1).toFloat(),
                    width.toFloat() - 8,
                    height.toFloat() + 2,
                    32,
                    Color(newColor)
                )
                Render2DEngine.drawRoundDoubleColor(
                    context.matrices,
                    (x + 4),
                    (y - 1),
                    (width - 8),
                    height,
                    3f,
                    Colors.getColor(200),
                    Colors.getColor(0)
                )
            } else {
                Render2DEngine.drawRoundDoubleColor(
                    context.matrices,
                    (x + 4),
                    (y - 1),
                    (width - 8),
                    height,
                    3f,
                    Colors.getColor(200),
                    Colors.getColor(0)
                )
            }
        } else {
            if (hovered) {
                Render2DEngine.drawRound(
                    context.matrices,
                    x.toFloat() + 4f,
                    (y + height - 16).toFloat(),
                    (width - 8).toFloat(),
                    height.toFloat(),
                    3f,
                    Color(newColor).darker()
                )
            }
        }
        if (!renderTimer.passed(1200)) {
            FontRenderers.lexend.drawString(
                context.matrices,
                "Visible: ${if (module.isVisible) "Yes" else "No"}",
                centerX,
                iy,
                Colors.fontColor.value.rgb,
                true
            )
            return
        }

        if (ClickGui.chinese.value && module.getCName() != "Undefined") {
            FontRenderers.default.drawString(
                context.matrices,
                module.moduleCName,
                (ix + 2f),
                (iy + 2f + if (hovered) -1f else 0f),
                if (module.isEnabled || module.alwaysEnable) color else fontColor,
                false
            )
        } else {
            FontRenderers.default.drawString(
                context.matrices,
                module.getName(),
                (ix + 2f),
                (iy + if (hovered) -0.25f else 1f),
                if (module.isEnabled || module.alwaysEnable) color else fontColor,
                false
            )
        }
        Render2DEngine.drawRectBlurredShadow(
            context.matrices,
            (x + 2).toFloat(),
            (y + height + 1).toFloat(),
            (width - 4).toFloat(),
            2f,
            14,
            Color(0, 0, 0, 255)
        )

//        draw settings
        if (isExtended || animationHeight != height) {
            var settingOffset = y + height
            for (component in toIsVisibleList(settings)) {
                if (component is SettingButton<*> && !component.value.isVisible()) continue
                component.solvePos()
                component.y = settingOffset + 2

                val percent = (y + height + animationHeight - settingOffset) / component.height
                if (percent > 0.5f) {
                    component.render(context, mouseX, mouseY, partialTicks)
                }

                if (!isExtended && percent <= 0f) {
                    component.close()
                }

                settingOffset += component.height
            }
        }
    }

    private fun mix(from: Color, to: Color, ratio: Float): Color {
        val rationSelf = 1.0f - ratio
        return Color(
            (from.red * rationSelf + to.red * ratio).toInt(),
            (from.green * rationSelf + to.green * ratio).toInt(),
            (from.blue * rationSelf + to.blue * ratio).toInt(),
            (from.alpha * rationSelf + to.alpha * ratio).toInt()
        )
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (module is HUDModule) {
            module.mouseClicked(mouseX.toFloat(), mouseY.toFloat(), button)
        }

        if (!isHovered(mouseX, mouseY)) {
            return false
        }

        when (button) {
            0 -> {
                if (!module.alwaysEnable) {
                    module.toggle()
                }
            }

            1 -> {
                isExtended = !isExtended
                startTime = System.currentTimeMillis()
            }

            2 -> {
                module.isVisible = !module.isVisible
                renderTimer.reset()
            }

            else -> {
                return false
            }
        }
        return true
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, state: Int) {
        if (module is HUDModule) {
            module.mouseReleased(mouseX.toFloat(), mouseY.toFloat(), state)
        }
        for (setting in settings) {
            setting.mouseReleased(mouseX, mouseY, state)
        }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        super.keyTyped(typedChar, keyCode)
        if (module is HUDModule) {
            module.keyTyped(keyCode)
        }
        for (setting in settings) {
            setting.keyTyped(typedChar, keyCode)
        }
    }
}
