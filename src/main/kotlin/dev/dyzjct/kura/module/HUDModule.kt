package dev.dyzjct.kura.module

import dev.dyzjct.kura.event.events.render.Render2DEvent
import dev.dyzjct.kura.event.eventbus.safeEventListener
import dev.dyzjct.kura.system.render.graphic.Render2DEngine
import dev.dyzjct.kura.gui.clickgui.DrawDelegateSelector
import dev.dyzjct.kura.gui.clickgui.HudEditorScreen
import dev.dyzjct.kura.gui.clickgui.component.Component
import dev.dyzjct.kura.gui.clickgui.render.DrawDelegate
import dev.dyzjct.kura.gui.clickgui.render.DrawScope
import dev.dyzjct.kura.setting.BooleanSetting
import net.minecraft.client.gui.DrawContext
import java.awt.Color

abstract class HUDModule(
    name: String,
    langName: String = "Undefined",
    override var x: Float = 0f,
    override var y: Float = 0f,
    category: Category = Category.HUD,
    description: String = "",
    visible: Boolean = false
) : AbstractModule(), Component {
    override var drawDelegate: DrawDelegate = DrawDelegateSelector.currentDrawDelegate

    override var width: Float = 10f
    override var height: Float = 10f

    private val pinned0 = bsetting("Pinned", true)
    private val pinned by pinned0
    private var relativeX by fsetting("RelativeX", 0f, -1f, 2f).isTrue { false }
    private var relativeY by fsetting("RelativeY", 0f, -1f, 2f).isTrue { false }

    private var lastX = 0f
    private var lastY = 0f

    init {
        moduleName = name
        moduleCName = langName
        moduleCategory = category
        this.description = description
        this.isVisible = visible

        pinned0.onChange<BooleanSetting> { _, input ->
            if (input) {
                synchronized(this) {
                    relativeX = x / mc.window.scaledWidth
                    relativeY = y / mc.window.scaledHeight
                }
            }
        }

        safeEventListener<Render2DEvent> {
            if (mc.currentScreen !is HudEditorScreen)
                return@safeEventListener
            Render2DEngine.drawRect(it.drawContext.matrices, x, y, width, height, Color(0, 0, 0, 50))
        }
    }

    protected var dragging = false
    protected var dragX = 0f
    protected var dragY = 0f

    final override fun DrawScope.render(mouseX: Float, mouseY: Float) {
        if (dragging) {
            try {
                this@HUDModule.x = (mouseX - dragX).coerceIn(0f, mc.window.scaledWidth - width)
                this@HUDModule.y = (mouseY - dragY).coerceIn(0f, mc.window.scaledHeight - height)
            } catch (e: IllegalArgumentException) {
                this@HUDModule.x = 0f
                this@HUDModule.y = 0f
            }

            relativeX = x / mc.window.scaledWidth.toFloat()
            relativeY = y / mc.window.scaledHeight.toFloat()
        }

        if (isEnabled) {
            renderOnGui(mouseX, mouseY)
        }
    }

    fun renderDelegateOnGame(context: DrawContext) {
        if (pinned && !dragging) {
            var calculatedX: Float
            var calculatedY: Float

            try {
                calculatedX =
                    (mc.window.scaledWidth.toFloat() * relativeX).coerceIn(0f, mc.window.scaledWidth - width)
                calculatedY =
                    (mc.window.scaledHeight.toFloat() * relativeY).coerceIn(0f, mc.window.scaledHeight - height)
            } catch (e: IllegalArgumentException) {
                calculatedX = 0f
                calculatedY = 0f
            }


            if (lastX != calculatedX || lastY != calculatedY) {
                lastX = calculatedX
                lastY = calculatedY

                this@HUDModule.x = calculatedX
                this@HUDModule.y = calculatedY

                rearrange()
            }
        }

        DrawScope(x, y, width, height, drawDelegate, context).renderOnGame()
        onRender(context)
    }

    open fun DrawScope.renderOnGame() {}

    open fun DrawScope.renderOnGui(mouseX: Float, mouseY: Float) {}

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
        if (isHovering(mouseX, mouseY) && button == 0) {
            dragging = true
            dragX = mouseX - x
            dragY = mouseY - y
            return true
        }
        return false
    }

    override fun mouseReleased(mouseX: Float, mouseY: Float, button: Int): Boolean {
        if (dragging && button == 0) {
            dragging = false
            return true
        }
        return false
    }
}
