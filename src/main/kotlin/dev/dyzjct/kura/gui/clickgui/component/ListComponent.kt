package dev.dyzjct.kura.gui.clickgui.component

import dev.dyzjct.kura.gui.clickgui.render.DrawDelegate
import dev.dyzjct.kura.gui.clickgui.render.DrawScope
import dev.dyzjct.kura.utils.animations.AnimationFlag
import dev.dyzjct.kura.utils.animations.Easing
import net.minecraft.client.gui.DrawContext

abstract class ListComponent(
    val parentComponent: Component,
    protected val selfHeight: Float,
    protected val elementSpacing: Float = 2f,
    duration: Float = 500f
) : Component {
    val elements = mutableListOf<Component>()
    var isOpened = false

    protected val animationFlag = AnimationFlag(Easing.OUT_QUAD, duration)

    override var height: Float = 0f
    protected var totalHeight: Float = 0f
    protected var playingAnimation = false

    override fun renderDelegate(context: DrawContext, mouseX: Float, mouseY: Float) {
        refreshHeight()
        super.renderDelegate(context, mouseX, mouseY)
    }

    private fun refreshHeight() {
        val targetHeight = if (isOpened) totalHeight else selfHeight
        val animatedHeight = animationFlag.getAndUpdate(targetHeight)
        height = animatedHeight

        if (playingAnimation) {
            if (animatedHeight == targetHeight) {
                playingAnimation = false
            }
            parentComponent.rearrange()
        }
    }

    protected fun setOpenState(isOpened: Boolean) {
        playingAnimation = true
        this.isOpened = isOpened
    }

    protected fun forceSetOpenState(isOpened: Boolean) {
        animationFlag.forceUpdate(if (isOpened) totalHeight else selfHeight)
        this.isOpened = isOpened
        height = selfHeight
    }

    protected fun DrawScope.renderChildElements(mouseX: Float, mouseY: Float) {
        if (height != selfHeight) {
            context.enableScissor(x.toInt(), y.toInt(), (x + width).toInt(), (y + height).toInt())
            elements.forEach {
                it.renderDelegate(context, mouseX, mouseY)
            }
            context.disableScissor()
        }
    }

    override fun rearrange() {
        if (height == selfHeight) {
            return
        }

        var offsetY = selfHeight + elementSpacing
        elements.forEach {
            it.x = x
            it.y = y + offsetY
            it.rearrange()
            offsetY += it.height + elementSpacing
        }
        totalHeight = offsetY

        if (!playingAnimation) {
            height = if (isOpened) totalHeight else selfHeight
            animationFlag.forceUpdate(totalHeight)
        }
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
        if (isHoveringOnTitle(mouseX, mouseY) && button == 1) {
            setOpenState(!isOpened)

            if (isOpened) {
                expand()
            } else {
                reduce()
            }
            return true
        }

        if (isOpened) {
            return elements.filter {
                if (it is Visible) {
                    it.isVisible()
                } else {
                    true
                }
            }.any {
                it.mouseClicked(mouseX, mouseY, button)
            }
        }

        return false
    }

    override fun mouseReleased(mouseX: Float, mouseY: Float, button: Int): Boolean {
        if (isOpened) {
            return elements.filter {
                if (it is Visible) {
                    it.isVisible()
                } else {
                    true
                }
            }.any {
                it.mouseReleased(mouseX, mouseY, button)
            }
        }

        return false
    }

    override fun keyTyped(keyCode: Int): Boolean {
        if (isOpened) {
            return elements.filter {
                if (it is Visible) {
                    it.isVisible()
                } else {
                    true
                }
            }.any {
                it.keyTyped(keyCode)
            }
        }

        return false
    }

    protected fun isHoveringOnTitle(mouseX: Float, mouseY: Float): Boolean {
        return mouseX in x..(x + width) && mouseY in y..(y + selfHeight)
    }

    open fun expand() {}

    open fun reduce() {}

    override fun changeDrawDelegate(drawDelegate: DrawDelegate) {
        super.changeDrawDelegate(drawDelegate)
        elements.forEach { it.changeDrawDelegate(drawDelegate) }
    }

    override fun guiClosed() {
        elements.forEach { it.guiClosed() }
    }
}