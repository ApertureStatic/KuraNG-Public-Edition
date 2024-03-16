package dev.dyzjct.kura.module.hud

import base.notification.NotificationManager
import base.system.render.graphic.Render2DEngine
import base.system.render.newfont.FontRenderers
import dev.dyzjct.kura.module.HUDModule
import net.minecraft.client.gui.DrawContext
import java.awt.Color

object NotificationNew : HUDModule(
    name = "NotificationNew",
    langName = "新的通知界面",
    x = 300f,
    y = 150f
) {
    private var notificationCount by isetting("NotificationCount", 4, 1, 12)
    override fun onRender(context: DrawContext) {
        width = 150f
        height = 35f
        if (NotificationManager.taskList.isEmpty()) return
        runCatching {
            for (i in 0 until NotificationManager.taskList.size.coerceAtMost(notificationCount)) {
                val notification = NotificationManager.taskList[i]
                val animationXOffset = x + width * notification.animation
                val arrangedHeight = if (y < mc.window.scaledHeight / 2)
                    (height + 5) * i.toFloat()
                        .symbolArranged(!notification.reversed, notification.animation) else -(height + 5) * i.toFloat()
                    .symbolArranged(!notification.reversed, notification.animation)
                if (notification.reversed && (notification.animation == 1f)) {
                    NotificationManager.taskList.remove(notification)
                    continue
                }

                Render2DEngine.drawRectBlurredShadow(
                    context.matrices,
                    animationXOffset - 8f,
                    y + arrangedHeight - 8f,
                    width + 16f,
                    height + 16f,
                    15,
                    notification.color
                )

                Render2DEngine.drawRect(
                    context.matrices, animationXOffset, y + arrangedHeight, width, height, notification.color
                )

                FontRenderers.cn.drawString(
                    context.matrices,
                    notification.message,
                    (x + width * notification.animation).symbolArranged(
                        true, width / 7f
                    ), y.symbolArranged(true, height / 2f) + arrangedHeight,
                    Color(255, 255, 255).rgb
                )
            }
        }
    }

    private fun Float.symbolArranged(left: Boolean, numberCalc: Float): Float {
        return if (left) {
            plus(numberCalc)
        } else {
            minus(numberCalc)
        }
    }

    override fun onEnable() {
        NotificationManager.taskList.clear()
    }

    override fun onDisable() {
        NotificationManager.taskList.clear()
    }
}