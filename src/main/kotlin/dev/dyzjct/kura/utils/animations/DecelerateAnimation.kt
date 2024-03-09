package dev.dyzjct.kura.utils.animations

class DecelerateAnimation : Animation {
    constructor(ms: Int, endPoint: Float) : super(ms, endPoint)
    constructor(ms: Int, endPoint: Float, direction: Direction) : super(ms, endPoint, direction)

    override fun getEquation(x: Float): Float {
        val x1: Float = x / duration
        return 1 - (x1 - 1) * (x1 - 1)
    }
}