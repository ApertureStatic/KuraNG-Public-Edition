package dev.dyzjct.kura.module.hud

import dev.dyzjct.kura.system.render.graphic.Render2DEngine
import dev.dyzjct.kura.system.render.newfont.FontRenderers
import base.utils.chat.ChatUtil
import dev.dyzjct.kura.manager.NotificationManager
import dev.dyzjct.kura.module.HUDModule
import dev.dyzjct.kura.module.modules.client.UiSetting
import dev.dyzjct.kura.module.modules.client.UiSetting.theme
import net.minecraft.client.gui.DrawContext
import java.awt.Color

object NotificationHUD : HUDModule(
    name = "NotificationHUD", langName = "通知界面", x = 300f, y = 150f
) {
    private val mode by msetting("Mode", NoticeMode.Normal)
    private val interval by isetting("Interval", 0, 0, 20)
    val animationLength by isetting("AnimationLength", 15, 10, 100)
    val fadeFraction by fsetting("FractionOfLength", 6f, 1f, 8f)
    private val color by csetting("MainColor", Color(0, 0, 0, 120))
    private val doubleColorSetting by csetting(
        "DoubleColor",
        Color(130, 255, 120)
    ).isTrue { theme == UiSetting.Theme.Custom }
    private val direction by msetting("FadeDirection", Direction.Left)
    private val fontMode by msetting("FontColorMode", FontColorMode.Light)

    private var doubleColor = doubleColorSetting
    override fun onRender(context: DrawContext) {
        width = 200f
        height = 30f
        doubleColor = when (theme) {
            UiSetting.Theme.Custom -> {
                doubleColorSetting
            }

            else -> {
                Color(
                    TargetHUD.getTargetColor().color1.red,
                    TargetHUD.getTargetColor().color1.green,
                    TargetHUD.getTargetColor().color1.blue,
                    color.alpha
                )
            }

        }
        if (NotificationManager.taskList.isEmpty()) return
        try {
            var count = 0f
            for (notification in NotificationManager.taskList) {
                if (notification.reversed && (notification.animation >= 1f)) {
                    count += 1f.symbolArranged(
                        !notification.reversed,
                        notification.animation
                    )
                    NotificationManager.taskList.remove(notification)
                    continue
                }
                val offsetWidth =
                    FontRenderers.cn.getStringWidth(notification.message) + 32
                val offsetX = x + 200 - offsetWidth
                val animationXOffset = offsetX + (offsetWidth + 20) * notification.animation
                val arrangedHeight = if (y < mc.window.scaledHeight / 2)
                    (height + 5 + interval) * count
                        .symbolArranged(
                            !notification.reversed,
                            notification.animation
                        ) else -(height + 5 + interval) * count
                    .symbolArranged(!notification.reversed, notification.animation)
                val doubleFadeEasing = if (direction == Direction.Left) {
                    notification.animationNoReset
                } else (1f - notification.animationNoReset)
                when (mode) {
                    NoticeMode.Normal -> {
                        Render2DEngine.drawRect(
                            context.matrices,
                            animationXOffset,
                            y + arrangedHeight,
                            offsetWidth,
                            height,
                            color
                        )
                        Render2DEngine.drawRect(
                            context.matrices,
                            animationXOffset,
                            y + arrangedHeight + height - (height / 10),
                            offsetWidth * doubleFadeEasing,
                            height / 10,
                            doubleColor
                        )
                        FontRenderers.cn.drawString(
                            context.matrices,
                            notification.message,
                            animationXOffset + 16,
                            y.symbolArranged(true, height / 2f) + arrangedHeight - 1.7f,
                            Color(255, 255, 255).rgb
                        )
                    }

                    NoticeMode.New -> {
                        Render2DEngine.drawRect(
                            context.matrices,
                            animationXOffset,
                            y + arrangedHeight,
                            offsetWidth,
                            height,
                            color
                        )
                        Render2DEngine.drawRect(
                            context.matrices,
                            animationXOffset,
                            y + arrangedHeight,
                            offsetWidth * doubleFadeEasing,
                            height,
                            doubleColor
                        )
                        FontRenderers.cn.drawString(
                            context.matrices,
                            notification.message,
                            animationXOffset + 16,
                            y.symbolArranged(true, height / 2f) + arrangedHeight - 1.7f,
                            Color(255, 255, 255).rgb
                        )
                    }
                }
                count += 1f.symbolArranged(
                    !notification.reversed,
                    notification.animation
                )
            }
        } catch (e: Exception) {
            println(e)
        }
    }

    private fun Float.symbolArranged(left: Boolean, numberCalc: Float): Float {
        return if (left) {
            plus(numberCalc)
        } else {
            minus(numberCalc)
        }
    }

    fun defaultFontColor(): String {
        return when (fontMode) {
            FontColorMode.Light -> ChatUtil.WHITE
            FontColorMode.Dark -> ChatUtil.BLACK
            else -> ChatUtil.WHITE
        }
    }

    override fun onEnable() {
        NotificationManager.taskList.clear()
    }

    override fun onDisable() {
        NotificationManager.taskList.clear()
    }

    enum class NoticeMode {
        Normal, New
    }

    enum class FontColorMode {
        Light, Dark
    }

    @Suppress("UNUSED")
    enum class Direction {
        Left, Right
    }
}