package dev.dyzjct.kura.gui.clickgui

import com.mojang.blaze3d.platform.GlStateManager
import dev.dyzjct.kura.event.eventbus.Listener
import dev.dyzjct.kura.event.events.render.Render2DEvent
import dev.dyzjct.kura.graphics.shaders.impl.ParticleShader
import dev.dyzjct.kura.gui.clickgui.animation.AnimationStrategy
import dev.dyzjct.kura.gui.clickgui.animation.NonAnimationStrategy
import dev.dyzjct.kura.gui.clickgui.component.Component
import dev.dyzjct.kura.gui.clickgui.component.ComponentContainer
import dev.dyzjct.kura.module.AbstractModule
import dev.dyzjct.kura.module.modules.client.UiSetting
import dev.dyzjct.kura.system.render.graphic.Render2DEngine
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text
import java.awt.Color

open class GuiScreen : Screen(Text.empty()) {
    private var buffer: Component? = null
    private var playingCloseAnimation = false

    val container = ComponentContainer()
    val elements = mutableListOf<Component>()

    var animationStrategy: AnimationStrategy = NonAnimationStrategy
        set(value) {
            field.onUnbind(container)
            field = value
            field.onBind(container)
        }

    private var idontknowwhatnameineedtospecify = false

    init {
        Render2DEvent.eventBus.subscribe(Listener(this, (0..1000).random(), 0) {
            val currentScreen = MinecraftClient.getInstance().currentScreen

            if (playingCloseAnimation && currentScreen != this) {
                if (!animationStrategy.playFinished()) {
                    animationStrategy.onRender((it as Render2DEvent).drawContext, 0f, 0f, container)
                } else {
                    playingCloseAnimation = false
                }
            }
        })
    }

    fun resetUiComponentPositions() {
        var offsetX = 5f
        elements.forEach {
            it.x = offsetX
            it.y = 10f
            it.rearrange()

            offsetX += it.width + 5f
        }
    }

    override fun shouldPause(): Boolean {
        return false
    }

    override fun onDisplayed() {
        super.onDisplayed()
        animationStrategy.onOpen()
        playingCloseAnimation = false
    }

    override fun close() {
        super.close()
        idontknowwhatnameineedtospecify = false
        animationStrategy.onClose()
        playingCloseAnimation = true
    }

    private fun updatePanelIndex() {
        buffer?.let {
            if (elements.isNotEmpty()) {
                val first = elements.last()
                val index = elements.indexOf(it)

                if (index != -1) {
                    elements[elements.size - 1] = it
                    elements[index] = first
                }
            }
            buffer = null
        }
    }

    fun moveToFirstRender(component: Component) {
        if (elements[0] != component) {
            buffer = component
        }
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        updatePanelIndex()
        //kotlin.runCatching { WindowBlurShader.render(width.toDouble(), height.toDouble()) }

        GlStateManager._enableBlend()
        if (UiSetting.getThemeSetting().particle) ParticleShader.render()
        animationStrategy.onRender(context, mouseX.toFloat(), mouseY.toFloat(), container)
        if (UiSetting.getThemeSetting().syt) {
            if (UiSetting.getThemeSetting().sytMode == UiSetting.SytMode.Top) {
                Render2DEngine.drawRectGradient(
                    context.matrices, 0.0f, 0.0f,
                    AbstractModule.mc.window.scaledWidth.toFloat(), AbstractModule.mc.window.scaledHeight.toFloat(),
                    UiSetting.getThemeSetting().sytColor, Color(0, 0, 0, 0),
                    UiSetting.getThemeSetting().sytColor, Color(0, 0, 0, 0)
                )
            } else {
                Render2DEngine.drawRectGradient(
                    context.matrices, 0.0f, 0.0f,
                    AbstractModule.mc.window.scaledWidth.toFloat(), AbstractModule.mc.window.scaledHeight.toFloat(),
                    Color(0, 0, 0, 0), UiSetting.getThemeSetting().sytColor,
                    Color(0, 0, 0, 0), UiSetting.getThemeSetting().sytColor
                )
            }
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        return container.mouseClicked(mouseX.toFloat(), mouseY.toFloat(), button)
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        return container.mouseReleased(mouseX.toFloat(), mouseY.toFloat(), button)
    }

    open fun keyTyped(keyCode: Int): Boolean {
        return container.keyTyped(keyCode)
    }

    final override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        val isReceived = keyTyped(keyCode)

        return if (isReceived) {
            true
        } else {
            super.keyPressed(keyCode, scanCode, modifiers)
        }
    }

    override fun mouseScrolled(
        mouseX: Double,
        mouseY: Double,
        horizontalAmount: Double,
        verticalAmount: Double
    ): Boolean {
        if (verticalAmount < 0) {
            elements.forEach {
                it.y -= 10
                it.rearrange()
            }
        } else if (verticalAmount > 0) {
            elements.forEach {
                it.y += 10
                it.rearrange()
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }
}