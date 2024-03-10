package dev.dyzjct.kura.gui.clickgui.component

import dev.dyzjct.kura.setting.*
import dev.dyzjct.kura.gui.clickgui.render.Alignment
import dev.dyzjct.kura.gui.clickgui.render.DrawDelegate
import dev.dyzjct.kura.gui.clickgui.render.DrawScope
import dev.dyzjct.kura.gui.clickgui.render.Padding
import dev.dyzjct.kura.module.modules.client.UiSetting
import dev.dyzjct.kura.utils.animations.AnimationFlag
import dev.dyzjct.kura.utils.animations.Easing
import java.awt.Color

class ModuleComponent(
    val module: dev.dyzjct.kura.module.AbstractModule,
    panel: Panel,
    override var x: Float,
    override var y: Float,
    override var width: Float,
    override var height: Float,
    override var drawDelegate: DrawDelegate
) : ListComponent(panel, height) {
    private var showVisibleText = false
    private var showVisibleTextStartTime = 0L

    private var inHovering = false
    private var hoveringStartTime = 0L

    private val descriptionTextProgress: Float
        get() = if (inHovering) {
            Easing.OUT_CUBIC.inc(Easing.toDelta(hoveringStartTime + DESCRIPTION_HOVERING_TIME, 400f))
        } else {
            Easing.OUT_CUBIC.dec(Easing.toDelta(hoveringStartTime, 400f))
        }

    private var targetProgress = 0f
    private val sliderProgress = AnimationFlag(Easing.OUT_QUAD, 400f)

    companion object {
        const val DESCRIPTION_HOVERING_TIME = 600L
        const val VISIBLE_TEXT_SHOW_TIME = 1200L

        const val LEFT_PADDING = 6
    }

    init {
        val x = x + LEFT_PADDING
        val width = width - LEFT_PADDING * 2

        @Suppress("UNCHECKED_CAST")
        module.settingList
            .forEach { setting ->
                when (setting) {
                    is BooleanSetting -> elements.add(
                        BooleanComponent(
                            setting,
                            panel,
                            x,
                            y,
                            width,
                            selfHeight,
                            drawDelegate
                        )
                    )

                    is StringSetting -> elements.add(
                        TextFieldComponent(
                            setting,
                            panel,
                            x,
                            y,
                            width,
                            selfHeight,
                            drawDelegate
                        )
                    )

                    is ColorSetting -> elements.add(
                        ColorPickerAdapter(
                            setting,
                            panel,
                            x,
                            y,
                            width,
                            drawDelegate,
                            selfHeight,
                        )
                    )

                    is NumberSetting -> elements.add(
                        NumberSlider(
                            setting,
                            panel,
                            x,
                            y,
                            width,
                            selfHeight,
                            drawDelegate
                        )
                    )

                    is ModeSetting<*> -> elements.add(
                        EnumComponent(
                            setting as ModeSetting<Enum<*>>,
                            panel,
                            x,
                            y,
                            width,
                            selfHeight,
                            drawDelegate
                        )
                    )
                }
            }

        elements.add(BindComponent(module, x, y, width, selfHeight, drawDelegate))
        targetProgress = if (module.isEnabled) 1f else 0f
        forceSetOpenState(false)
        rearrange()
    }

    override fun rearrange() {
        if (height == selfHeight && !isOpened) {
            return
        }

        var offsetY = selfHeight + elementSpacing
        elements
            .filter { it is Visible && it.isVisible() }
            .forEach {
                it.x = x + LEFT_PADDING
                it.y = y + offsetY

                if (isOpened || it is ColorPickerAdapter) {
                    it.rearrange()
                }

                offsetY += it.height + elementSpacing
            }
        totalHeight = offsetY

        if (!playingAnimation) {
            height = if (isOpened) totalHeight else height
            animationFlag.forceUpdate(totalHeight)
        }
    }

    override fun DrawScope.render(mouseX: Float, mouseY: Float) {
        if (height != selfHeight) {
            drawRectBySetting(
                x,
                y + selfHeight,
                width,
                height - selfHeight,
                UiSetting.getThemeSetting().setting,
                Padding(horizontal = LEFT_PADDING.toFloat() - 2f)
            )

//            if (UiSetting.moduleSettingBorder) {
//                drawRect(
//                    x + LEFT_PADDING - 4f,
//                    y + selfHeight,
//                    2f,
//                    height - selfHeight,
//                    UiSetting.primaryColor
//                )
//            }
        }

        targetProgress = if (module.isEnabled) 1f else 0f

        val progress = sliderProgress.getAndUpdate(targetProgress)
        drawRectBySetting(
            x,
            y,
            width * progress,
            selfHeight,
            UiSetting.getThemeSetting().primary,
            Padding(horizontal = 1f)
        )
//        if (!module.isEnabled && isOpened) {
//            drawRectBySetting(x, y, width, selfHeight, UiSetting.settingPanelColor, Padding(horizontal = 1f))
//        }

        if (showVisibleText) {
            drawText(
                "Visible: ${if (module.isVisible) "Yes" else "No"}",
                Color.WHITE,
                horizontalAlignment = Alignment.CENTER,
                verticalAlignment = Alignment.CENTER,
                containerHeight = selfHeight
            )

            if (System.currentTimeMillis() > showVisibleTextStartTime + VISIBLE_TEXT_SHOW_TIME) {
                showVisibleText = false
            }
        } else {
            drawText(
                module.getName(),
                if (isHoveringOnTitle(mouseX, mouseY)) {
                    Color.WHITE.darker()
                } else {
                    Color.WHITE
                },
                horizontalAlignment = Alignment.CENTER,
                verticalAlignment = Alignment.CENTER,
                containerHeight = selfHeight
            )
        }

        if (height != selfHeight) {
            context.enableScissor(x.toInt(), y.toInt(), (x + width).toInt(), (y + height).toInt())
            elements
                .filter { it is Visible && it.isVisible() }
                .forEach {
                    if (it.y + it.height * 0.3 < y + height) {
                        it.renderDelegate(context, mouseX, mouseY)
                    }
                }

            if (!isOpened) {
                elements.forEach {
                    if (it.y + it.height > y + height) {
                        it.guiClosed()
                    }
                }
            }
            context.disableScissor()
        }

        if (isHoveringOnTitle(mouseX, mouseY)) {
            if (!inHovering) {
                inHovering = true
                hoveringStartTime = System.currentTimeMillis()
            }
        } else {
            if (inHovering) {
                inHovering = false
                hoveringStartTime = System.currentTimeMillis()
            }
        }

        if (
            descriptionTextProgress != 0f &&
            (System.currentTimeMillis() < hoveringStartTime + DESCRIPTION_HOVERING_TIME + 400L ||
                    (inHovering && System.currentTimeMillis() > hoveringStartTime + DESCRIPTION_HOVERING_TIME))
        ) {
            val text = module.description.ifEmpty { "Description is empty" }

            val textProgress = descriptionTextProgress
            val secondaryColor = UiSetting.getThemeSetting().secondary

            drawRectBySetting(
                mouseX + 2,
                mouseY - (textHeight * textProgress),
                getTextWidth(text),
                textHeight * textProgress,
                Color(secondaryColor.red, secondaryColor.green, secondaryColor.blue, 255),
                Padding(horizontal = -2f, vertical = -1f)
            )

            val textColor = Color.WHITE
            drawText(
                text,
                mouseX + 2,
                mouseY - textHeight - 2,
                Color(textColor.red, textColor.green, textColor.blue, (255 * textProgress).toInt()),
                verticalAlignment = Alignment.CENTER,
                containerHeight = selfHeight
            )
        }

        if (module is Component) {
            module.renderDelegate(context, mouseX, mouseY)
        }
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
        if (isHoveringOnTitle(mouseX, mouseY)) {
            when (button) {
                0 -> {
                    module.toggle()
                    return true
                }

                2 -> {
                    module.isVisible = !module.isVisible
                    showVisibleText = true
                    showVisibleTextStartTime = System.currentTimeMillis()
                    return true
                }
            }
        }

        val superResult = super.mouseClicked(mouseX, mouseY, button)
        if (module is Component) {
            return module.mouseClicked(mouseX, mouseY, button) || superResult
        }
        return superResult
    }

    override fun mouseReleased(mouseX: Float, mouseY: Float, button: Int): Boolean {
        val superResult = super.mouseReleased(mouseX, mouseY, button)
        if (module is Component) {
            return module.mouseReleased(mouseX, mouseY, button) || superResult
        }
        return superResult
    }

    override fun keyTyped(keyCode: Int): Boolean {
        val superResult = super.keyTyped(keyCode)
        if (module is Component) {
            return module.keyTyped(keyCode) || superResult
        }
        return superResult
    }

    override fun guiClosed() {
        setOpenState(false)
    }
}