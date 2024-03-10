package dev.dyzjct.kura.gui.clickgui.animation

import dev.dyzjct.kura.module.modules.client.UiSetting
import dev.dyzjct.kura.utils.animations.Easing

abstract class AbstractAnimationStrategy : AnimationStrategy {
    private var startTime = System.currentTimeMillis()
    private var isReverse = false

    private val progress0: Float
        get() = Easing.IN_QUAD.inc(Easing.toDelta(startTime, UiSetting.animationLength))

    protected val progress: Float
        get() = if (isReverse) {
            1 - progress0
        } else {
            progress0
        }

    override fun onOpen() {
        isReverse = false
        reset()
    }

    override fun onClose() {
        isReverse = true
        reset()
    }

    override fun reset() {
        startTime = System.currentTimeMillis()
    }

    override fun playFinished(): Boolean {
        return startTime + UiSetting.animationLength < System.currentTimeMillis()
    }
}