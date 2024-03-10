package dev.dyzjct.kura.gui.gui.component

import dev.dyzjct.kura.gui.gui.Panel
import dev.dyzjct.kura.module.modules.client.Colors
import dev.dyzjct.kura.setting.Setting
import dev.dyzjct.kura.utils.animations.Animation
import dev.dyzjct.kura.utils.animations.DecelerateAnimation
import base.system.render.graphic.Render2DEngine
import base.system.render.newfont.FontRenderers
import net.minecraft.client.gui.DrawContext
import net.minecraft.util.math.RotationAxis
import base.KuraIdentifier
import java.awt.Color

class ModeButton(value: Setting<Enum<*>>, width: Double, height: Double, father: Panel?) : SettingButton<Enum<*>>() {
    private val arrow = KuraIdentifier("textures/arrow.png")
    private val rotation = DecelerateAnimation(240, 1f, Animation.Direction.FORWARDS)
    private var open = false
    private var wheight = 0.0

    init {
        this.width = width
        this.height = height
        this.father = father
        this.value = value
    }

    override fun render(context: DrawContext, mouseX: Double, mouseY: Double, partialTicks: Float) {
        wheight = 17.0
        height = if (open) {
            15.0 + asModeValue.getModes().size * 12.0
        } else 17.0
        rotation.setDirection(if (open) Animation.Direction.BACKWARDS else Animation.Direction.FORWARDS)
        val tx = (x + width - 11).toFloat()
        val ty = (y + wheight / 2).toFloat()
        val matrixStack = context.matrices
        val thetaRotation = (-180f * rotation.getOutput())
        matrixStack.push()
        matrixStack.translate(tx, ty, 0f)
        matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(thetaRotation))
        matrixStack.translate(-tx, -ty, 0f)
        Render2DEngine.drawTexture(context, arrow, (x + width - 14).toInt(), (y + (wheight - 6) / 2).toInt(), 6, 6)
        matrixStack.pop()
        FontRenderers.default.drawString(
            matrixStack, value.name,
            (x + 6.0), (y + wheight / 2.0 - 6.0 / 2.0) + 3.0, Color(-1).rgb
        )
        FontRenderers.default.drawString(
            matrixStack,
            this.asModeValue.valueAsString,
            x + width - 16.0 - FontRenderers.default.getStringWidth(this.asModeValue.valueAsString),
            3 + (y + wheight / 2 - 6 / 2f),
            if (!isHovered(mouseX, mouseY)) {
                Color(255, 255, 255).rgb
            } else {
                Color(155, 155, 155).rgb
            }
        )

        if (open) {
            var offsetY = 0.0
            for (i in 0 until this.asModeValue.getModes().size) {
                FontRenderers.default.drawString(
                    matrixStack,
                    this.asModeValue.getModes()[i],
                    x + width / 2.0 - FontRenderers.default.getStringWidth(this.asModeValue.getModes()[i]) / 2.0,
                    (y + wheight + ((12 shr 1) - 6 / 2.0 - 1) + offsetY),
                    if (asModeValue.currentEnumName().contentEquals(asModeValue.getModes()[i])) {
                        Colors.getColor(0).rgb
                    } else {
                        Color(-1).rgb
                    }
                )
                offsetY += 12.0
            }
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (!isHovered(mouseX, mouseY) || !value.isVisible()) return false
        if (Render2DEngine.isHovered(mouseX, mouseY, x, y, width, wheight)) {
            if (button == 0) {
                this.asModeValue.forwardLoop()
            } else {
                open = !open
            }
        }
        if (open) {
            var offsetY = 0.0
            for (i in 0 until this.asModeValue.getModes().size) {
                if (Render2DEngine.isHovered(
                        mouseX,
                        mouseY,
                        x,
                        y + wheight + offsetY,
                        width,
                        12.0
                    ) && button == 0
                ) this.asModeValue.setValueByIndex(i)
                offsetY += 12.0
            }
        }
        return true
    }
}
