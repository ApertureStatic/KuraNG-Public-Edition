package dev.dyzjct.kura.module.hud

import dev.dyzjct.kura.module.HUDModule
import melon.notification.NotificationManager
import melon.system.render.graphic.Render2DEngine
import melon.system.render.newfont.FontRenderers
import net.minecraft.client.gui.DrawContext
import java.awt.Color

object NotificationHUD : HUDModule(name = "NotificationHUD", langName = "通知界面", x = 300f, y = 150f) {
    private var notificationCount by isetting("NotificationCount", 4, 1, 12)
    private var widthSize by isetting("Width", 200, 1, 500)
    private var heightSize by isetting("Height", 35, 1, 100)
    private var fontColor by csetting("FontColor", Color(22, 217, 215, 255))
    private var baseColor by csetting("BaseColor", Color.BLACK)
    private var layerColor by csetting("LayerColor", Color(255, 182, 193, 255))

    override fun onEnable() {
        NotificationManager.taskList.clear()
    }

    override fun onDisable() {
        NotificationManager.taskList.clear()
    }

    override fun onRender(context: DrawContext) {
        width = widthSize.toFloat()
        height = heightSize.toFloat()
        var count = 1f
        for (i in 0 until NotificationManager.taskList.size.coerceAtMost(notificationCount)) {
            val notification = NotificationManager.taskList[i]
            notification.leftSided = x < mc.window.scaledWidth / 2
            if (notification.reversed && ((notification.leftSided && notification.baseAnimation == 0f || !notification.leftSided && notification.baseAnimation == 1f))) {
                NotificationManager.taskList.remove(notification)
                return
            }
            val animationXOffset = x + width * notification.baseAnimation
            val animationLayerXOffset = x + width * notification.layerAnimation
            val arrangedHeight = (height + 5) * count
            //Base Layer
            Render2DEngine.drawRect(
                context.matrices, animationXOffset, y + arrangedHeight, width, height, baseColor
            )
            //Second Layer
            Render2DEngine.drawRect(
                context.matrices,
                animationLayerXOffset.symbolArranged(!notification.leftSided, width / 9),
                y + arrangedHeight,
                width.symbolArranged(notification.leftSided, width / 9),
                height,
                layerColor
            )
            //Text Render
            FontRenderers.cn.drawString(
                context.matrices, notification.message, (x + width * notification.layerAnimation).symbolArranged(
                    !notification.leftSided, width / 7f
                ), y.symbolArranged(!notification.leftSided, height / 2f) + arrangedHeight, fontColor.rgb
            )
            count += 1f.symbolArranged(!notification.reversed, notification.baseAnimation)
        }
    }

    private fun Float.symbolArranged(left: Boolean, numberCalc: Float): Float {
        return if (left) {
            plus(numberCalc)
        } else {
            minus(numberCalc)
        }
    }
}