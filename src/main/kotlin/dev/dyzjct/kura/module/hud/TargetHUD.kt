package dev.dyzjct.kura.module.hud

import base.KuraIdentifier
import base.system.render.newfont.FontRenderers
import base.utils.combat.getTarget
import base.utils.concurrent.threads.runSafe
import com.mojang.blaze3d.systems.RenderSystem
import dev.dyzjct.kura.gui.clickgui.render.DrawScope
import dev.dyzjct.kura.module.HUDModule
import dev.dyzjct.kura.module.modules.crystal.CrystalHelper.scaledHealth
import dev.dyzjct.kura.utils.animations.Easing
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.PlayerSkinDrawer
import net.minecraft.client.network.AbstractClientPlayerEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.RotationAxis
import java.awt.Color

object TargetHUD : HUDModule(
    name = "TargetHUD",
    langName = "目标显示",
    255f,
    255f
) {
    private val range by dsetting("targetRange", 8.0, 1.0, 12.0)
    private val fadeLength by isetting("FadeLength", 200, 0, 1000)
    private var lastTarget: PlayerEntity? = null
    private var isTargetNull = true
    private var started = false
    private var startTime = 0L

    init {
        onMotion {
            getTarget(range)?.let { target ->
                lastTarget = target
                isTargetNull = false
            } ?: run {
                isTargetNull = true
            }
        }

        onLoop {
            if (!isTargetNull) {
                if (started) {
                    startTime = System.currentTimeMillis()
                }
                started = false
            } else if (!started) {
                startTime = System.currentTimeMillis()
                started = true
            }
        }
    }

    override fun onRender(context: DrawContext) {
        width = 135f
        height = 45f
    }

    override fun DrawScope.renderOnGame() {
        runSafe {
            lastTarget?.let {
                val animationScale = if (isTargetNull) {
                    Easing.IN_CUBIC.dec(Easing.toDelta(startTime, fadeLength))
                } else {
                    Easing.OUT_CUBIC.inc(Easing.toDelta(startTime, fadeLength))
                }

                if (animationScale == 0.0f) return

                val healthPercentage =
                    (100f - ((it.scaledHealth * 2.7f))) / 100f

                drawRoundRect(x, y, width * animationScale, height * animationScale, Color(76, 179, 208, 150))

                drawTargetFace(
                    context, it, animationScale, x.toDouble(), y.toDouble()
                )

                matrixStack.push()

                context.matrices.scale(animationScale, animationScale, 1.0f)
                matrixStack.translate((x / animationScale) - x, (y / animationScale) - y, 0.0f)

                RenderSystem.disableBlend()
                context.drawTexture(
                    KuraIdentifier("textures/slm.png"),
                    x.toInt() - 6,
                    y.toInt() - 12,
                    0F,
                    0F,
                    28,
                    28,
                    28,
                    28
                )
                RenderSystem.enableBlend()

                drawRect(x + 45, y + 25, width * healthPercentage / 1.5f, height / 4, Color.RED)

                FontRenderers.cn.drawString(
                    context.matrices,
                    "Name: ${it.name.string}",
                    x + 45,
                    y + 13,
                    Color.WHITE.rgb
                )

                matrixStack.pop()
            }
        }
    }

    private fun drawTargetFace(context: DrawContext, target: PlayerEntity, scale: Float, x: Double, y: Double) {
        context.matrices.push()
        context.matrices.translate(x, y, 0.0)
        context.matrices.scale(scale, scale, 1f)
        context.matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(0f))
        PlayerSkinDrawer.draw(context, (target as AbstractClientPlayerEntity).skinTexture, 10, 7, 32, false, false)
        context.matrices.pop()
    }
}