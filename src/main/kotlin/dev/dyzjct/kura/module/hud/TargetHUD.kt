package dev.dyzjct.kura.module.hud

import base.system.render.graphic.Render2DEngine
import base.system.render.newfont.FontRenderers
import base.utils.combat.getTarget
import base.utils.concurrent.threads.runSafe
import base.utils.math.MathUtils.clamp
import com.mojang.blaze3d.systems.RenderSystem
import dev.dyzjct.kura.KuraIdentifier
import dev.dyzjct.kura.gui.clickgui.render.DrawScope
import dev.dyzjct.kura.module.HUDModule
import dev.dyzjct.kura.module.modules.client.UiSetting
import dev.dyzjct.kura.module.modules.client.UiSetting.Theme
import dev.dyzjct.kura.module.modules.client.UiSetting.theme
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
    private val range by dsetting("TargetRange", 8.0, 1.0, 12.0)
    private val fadeLength by isetting("FadeLength", 200, 0, 1000)
    private var lastTarget: PlayerEntity? = null
    private var isTargetNull = true
    private var started = false
    private var startTime = 0L

    var color  = Color(0,0,0,0)
    var color2  = Color(0,0,0,0)

    init {
        onMotion {
            getTarget(range)?.let { target ->
                lastTarget = target
                isTargetNull = false
            } ?: run {
                isTargetNull = true
            }
        }
    }

    override fun onRender(context: DrawContext) {
        var addWidth = 0f
        lastTarget?.let {
            if (FontRenderers.cn.getStringWidth("Name: ${it.name.string}") + 55f > 135) {
                addWidth = FontRenderers.cn.getStringWidth("Name: ${it.name.string}") + 55f - 135f
            }
        }
        width = 135f + addWidth
        height = 45f
    }


    override fun DrawScope.renderOnGame() {
        when (theme.value) {
            Theme.Rimuru -> {
                color = Color(86, 190, 208, 140)
                color2 = Color(76, 179, 208, 250)
            }
            Theme.Arona -> {
                color = Color(144, 204, 236, 250)
                color2 = Color(213, 236, 252, 140)
            }
            Theme.Mahiro -> {
                color = Color(245, 176, 166, 250)
                color2 = Color(253, 235, 241, 200)
            }
            Theme.Roxy -> {
                color = Color(117, 106, 171, 250)
                color2 = Color(89, 77, 89, 200)
            }
            Theme.Mahiru -> {
                color =   Color(218, 165, 32, 200)
                color2 =   Color(254, 220, 189, 200)
            }
            Theme.Gura -> {
              color =   Color(51, 153, 189, 200)
              color2 =     Color(204, 255, 255, 200)
            }
            Theme.Mikoto -> {
               color   =    Color(109, 68, 55, 250)
               color2   =     Color(255, 255, 255, 150)
            }
            Theme.Miku -> {
               color   =     Color(228, 142, 151, 250)
               color2   =      Color(187, 209, 248, 200)
            }
            else -> {
                color = Color(UiSetting.getThemeSetting().targetcolor.red, UiSetting.getThemeSetting().targetcolor.green, UiSetting.getThemeSetting().targetcolor.blue, 250)
                color2 = Color(UiSetting.getThemeSetting().targethealthcolor.red, UiSetting.getThemeSetting().targethealthcolor.green, UiSetting.getThemeSetting().targethealthcolor.blue, 250)
            }
        }
        if (!isTargetNull) {
            if (started) {

                startTime = System.currentTimeMillis() - if (Easing.OUT_CUBIC.inc(
                        Easing.toDelta(
                            startTime,
                            fadeLength
                        )
                    ) < 1f
                ) (fadeLength - (System.currentTimeMillis() - startTime)) else 0
            }
            started = false
        } else if (!started) {
            startTime = System.currentTimeMillis() - if (Easing.OUT_CUBIC.inc(
                    Easing.toDelta(
                        startTime,
                        fadeLength
                    )
                ) < 1f
            ) (fadeLength - (System.currentTimeMillis() - startTime)) else 0
            started = true
        }

        val animationScale = if (isTargetNull) {
            Easing.IN_CUBIC.dec(Easing.toDelta(startTime, fadeLength))
        } else {
            Easing.OUT_CUBIC.inc(Easing.toDelta(startTime, fadeLength))
        }

        runSafe {
            lastTarget?.let {

                RenderSystem.disableBlend()

                matrixStack.push()

                val healthPercentage =
                    (it.health + it.absorptionAmount) / 36f
                val hurtPercentage = (Render2DEngine.interpolateFloat(
                    clamp(if (it.hurtTime == 0) 0f else it.hurtTime + 1f, 0f, 10f), it.hurtTime.toFloat(),
                    mc.tickDelta.toDouble()
                )) / 8f

                if (animationScale >= 1f) context.matrices.scale(
                    1f - hurtPercentage / 20f,
                    1f - hurtPercentage / 20f,
                    1f
                ) else {
                    context.matrices.scale(animationScale, animationScale, 1.0f)
                }

                if (animationScale == 1f) matrixStack.translate(
                    (x / (1f - hurtPercentage / 20f)) - x,
                    (y / (1f - hurtPercentage / 20f)) - y,
                    0.0f
                ) else matrixStack.translate((x / animationScale) - x, (y / animationScale) - y, 0.0f)

                drawRoundRect(x, y, width, height, color)

                Render2DEngine.drawRectBlurredShadow(
                    context.matrices,
                    x,
                    y,
                    width,
                    height,
                    16,
                   color)


                drawRoundRect(x + 45, y + 25, width * healthPercentage / 1.65f, height / 4, color2)

                FontRenderers.cn.drawString(
                    context.matrices,
                    "Name: ${it.name.string}",
                    x + 45,
                    y + 13,
                    Color.WHITE.rgb
                )

                drawTargetFace(
                    context, it, x.toDouble(), y.toDouble()
                )

                Render2DEngine.drawRect(
                    context.matrices,
                    x + 10,
                    y + 7,
                    32f,
                    32f,
                    Color(255, 0, 0, (150 * hurtPercentage).toInt())
                )
                if (theme.value == Theme.Rimuru) {
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
                }


                matrixStack.pop()

                RenderSystem.enableBlend()

            }
        }
    }


    private fun drawTargetFace(context: DrawContext, target: PlayerEntity, x: Double, y: Double) {
        context.matrices.push()
        context.matrices.translate(x, y, 0.0)
        context.matrices.scale(1f, 1f, 1f)
        context.matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(0f))
        PlayerSkinDrawer.draw(context, (target as AbstractClientPlayerEntity).skinTexture, 10, 7, 32, false, false)
        context.matrices.pop()
    }
}