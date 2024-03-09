package dev.dyzjct.kura.utils.animations

import kotlin.math.pow

open class EaseBackIn : Animation {
    private val easeAmount: Float

    constructor(ms: Int, endPoint: Float, easeAmount: Float) : super(ms, endPoint) {
        this.easeAmount = easeAmount
    }

    constructor(ms: Int, endPoint: Float, easeAmount: Float, direction: Direction) : super(
        ms,
        endPoint,
        direction
    ) {
        this.easeAmount = easeAmount
    }

    override fun correctOutput(): Boolean {
        return true
    }

    override fun getEquation(x: Float): Float {
        val x1 = x / duration
        val shrink = easeAmount + 1
        return 0f.coerceAtLeast(1 + shrink * (x1 - 1).pow(3f) + easeAmount * (x1 - 1).pow(2f))
    }
}