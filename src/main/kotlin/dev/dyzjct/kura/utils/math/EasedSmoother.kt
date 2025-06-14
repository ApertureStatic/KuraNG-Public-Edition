package dev.dyzjct.kura.utils.math

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class EasedSmoother(
    private var currentYaw: Float,
    private var durationTicks: Float = 10f, // 持续 tick 数
    private val easing: (Float) -> Float = ::easeOutSine // 可自定义缓动函数
) {
    private var startYaw: Float = currentYaw
    private var targetYaw: Float = currentYaw
    private var elapsedTicks: Int = 0
    private var isSmoothing = false

    fun setTarget(newTarget: Float) {
        startYaw = currentYaw
        targetYaw = normalizeYaw(newTarget)
        elapsedTicks = 0
        isSmoothing = true
    }

    fun update(): Float {
        if (!isSmoothing) return currentYaw

        elapsedTicks++
        val progress = (elapsedTicks.toFloat() / durationTicks).coerceIn(0f, 1f)
        val easedProgress = easing(progress)

        val delta = shortestYawDifference(startYaw, targetYaw)
        currentYaw = normalizeYaw(startYaw + delta * easedProgress)

        if (elapsedTicks >= durationTicks) {
            currentYaw = targetYaw
            isSmoothing = false
        }

        return currentYaw
    }

    fun getCurrentYaw(): Float = currentYaw

    fun setCurrentYaw(value: Float) {
        currentYaw = value
    }

    // 最短角度差
    private fun shortestYawDifference(from: Float, to: Float): Float {
        val diff = (to - from + 540f) % 360f - 180f
        return diff
    }

    // 角度标准化
    private fun normalizeYaw(yaw: Float): Float {
        return ((yaw % 360f) + 360f) % 360f
    }

    companion object {
        fun easeOutSine(x: Float): Float = sin((x * PI / 2).toFloat())
        fun easeInOutSine(x: Float): Float = (-cos(PI * x) + 1).toFloat() / 2f
        fun linear(x: Float): Float = x
    }
}
