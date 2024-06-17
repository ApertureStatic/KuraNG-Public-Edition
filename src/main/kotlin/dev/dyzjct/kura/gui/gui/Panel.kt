package dev.dyzjct.kura.gui.gui

import dev.dyzjct.kura.system.render.graphic.Render2DEngine
import dev.dyzjct.kura.system.render.newfont.FontRenderers
import dev.dyzjct.kura.system.render.shader.MSAAFramebuffer
import dev.dyzjct.kura.KuraIdentifier
import dev.dyzjct.kura.gui.gui.component.Component
import dev.dyzjct.kura.gui.gui.component.ModuleButton
import dev.dyzjct.kura.gui.gui.component.SettingButton
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.HUDModule
import dev.dyzjct.kura.module.ModuleManager
import dev.dyzjct.kura.module.modules.client.Colors
import dev.dyzjct.kura.utils.animations.Animation
import dev.dyzjct.kura.utils.animations.EaseBackIn
import net.minecraft.client.gui.DrawContext
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.util.*
import java.util.function.Predicate
import java.util.stream.Collectors

class Panel(var category: Category, var x: Double, var y: Double, var width: Double, var height: Double) {
    private val animation = EaseBackIn(270, 1f, 1.03f, Animation.Direction.BACKWARDS)
    private var categoryName = category.name
    private var isHUD = category.isHUD
    private var dragging = false
    private var x2 = 0.0
    private var y2 = 0.0
    private val icon = KuraIdentifier("textures/" + categoryName.lowercase(Locale.getDefault()) + ".png")
    var elements: MutableList<ModuleButton> = ArrayList()
    var extended = true

    companion object {
        fun toIsVisibleList(toChangeList: List<Component>): List<Component> {
            return toChangeList.stream().filter { obj: Component? ->
                if (obj is SettingButton<*>) {
                    return@filter obj.value.isVisible()
                }
                true
            }.collect(Collectors.toList())
        }
    }

    init {
        try {
            for (m in ModuleManager.moduleList) {
                if (m.moduleCategory != this.category) continue
                elements.add(ModuleButton(m, width, height, this))
            }
        } catch (_: Exception) {
        }
    }

    fun drawScreen(context: DrawContext, mouseX: Double, mouseY: Double, partialTicks: Float) = MSAAFramebuffer.use {
        if (dragging) {
            x = x2 + mouseX
            y = y2 + mouseY
        }
        animation.setDirection(if (extended) Animation.Direction.FORWARDS else Animation.Direction.BACKWARDS)

        var startY = y + height + 2
        if (elements.isNotEmpty()) {
            for (button in elements) {
//            for (button in elements) {
                if (!extended || !animation.finished(Animation.Direction.FORWARDS)) continue
                button.solvePos()
                button.y = startY
                button.render(context, mouseX, mouseY, partialTicks)
                startY += button.height + button.animationHeight + 2
                if (button.module !is HUDModule) continue
            }
        }
        val color = if (Colors.overwritePanel.value) {
            Colors.panelColor.value
        } else {
            Color(45, 45, 45, 255)
        }
        categoryName.let {
            GL11.glEnable(GL11.GL_LINE_SMOOTH)
            Render2DEngine.drawRoundD(context.matrices, x + 2, y - 4, width - 4.0, height, 4f, color)
            Render2DEngine.drawTexture(context, icon, (x.toInt() + 7), (y + (height - 18) / 2).toInt(), 12, 12)
            FontRenderers.default.drawCenteredString(
                context.matrices,
                it,
                x + 2 + (width - 4) / 2.0,
                y + height / 2f - 7.0,
                Color(-1).rgb
            )
            GL11.glDisable(GL11.GL_LINE_SMOOTH)
        }
    }


    fun mouseClicked(mouseX: Double, mouseY: Double, mouseButton: Int): Boolean {
        if (mouseButton == 0 && isHovered(mouseX, mouseY).test(this)) {
            x2 = x - mouseX
            y2 = y - mouseY
            dragging = true
            if (!isHUD) {
                Collections.swap(GUIRender.panels, 0, GUIRender.panels.indexOf(this))
            } else {
                Collections.swap(HUDRender.panels, 0, HUDRender.panels.indexOf(this))
            }
            return true
        }
        if (mouseButton == 1 && isHovered(mouseX, mouseY).test(this)) {
            extended = !extended
            return true
        }
        return false
    }

    fun mouseReleased(mouseX: Double, mouseY: Double, state: Int) {
        if (state == 0) {
            dragging = false
        }
        for (component in elements) {
            component.mouseReleased(mouseX, mouseY, state)
        }
    }

    fun keyTyped(typedChar: Char, keyCode: Int) {
        for (component in elements) {
            component.keyTyped(typedChar, keyCode)
        }
    }

    fun onClose() {
        for (component in elements) {
            component.close()
        }
    }

    fun isHovered(mouseX: Double, mouseY: Double): Predicate<Panel> {
        return Predicate { c: Panel ->
            mouseX >= c.x.coerceAtMost(c.x + c.width) && mouseX <= c.x.coerceAtLeast(c.x + c.width) && mouseY >= c.y.coerceAtMost(
                c.y + c.height
            ) && mouseY <= c.y.coerceAtLeast(c.y + c.height)
        }
    }
}
