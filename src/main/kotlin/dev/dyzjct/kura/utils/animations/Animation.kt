package dev.dyzjct.kura.utils.animations

import dev.dyzjct.kura.utils.TimerUtils

open class Animation {
    private var timerUtil = TimerUtils()
    var duration: Int
    private var endPoint: Float
    private var direction0: Direction

    constructor(ms: Int, endPoint: Float) {
        duration = ms
        this.endPoint = endPoint
        direction0 = Direction.FORWARDS
    }

    constructor(ms: Int, endPoint: Float, direction: Direction) {
        duration = ms
        this.endPoint = endPoint
        this.direction0 = direction
    }

    fun finished(direction: Direction): Boolean {
        return isDone && this.direction0 == direction
    }

    fun reset() {
        timerUtil.reset()
    }

    private val isDone: Boolean
        get() = timerUtil.passedMs(duration.toLong())

    fun getDirection(): Direction {
        return direction0
    }

    fun setDirection(direction: Direction) {
        if (this.direction0 != direction) {
            this.direction0 = direction
            timerUtil.setMs(
                System.currentTimeMillis() - (duration - duration.coerceAtMost(timerUtil.passedTimeMs.toInt()))
            )
        }
    }

    open fun correctOutput(): Boolean {
        return false
    }

    fun getOutput(): Float {
        return if (direction0 === Direction.FORWARDS) {
            if (isDone) endPoint else getEquation(timerUtil.passedTimeMs.toFloat()) * endPoint
        } else {
            if (isDone) return 0f
            if (correctOutput()) {
                val revTime =
                    duration.coerceAtMost(0.coerceAtLeast((duration - timerUtil.passedTimeMs).toInt())).toFloat()
                getEquation(revTime) * endPoint
            } else (1 - getEquation(timerUtil.passedTimeMs.toFloat())) * endPoint
        }
    }

    open fun getEquation(x: Float): Float {
        return 0f
    }

    enum class Direction {
        FORWARDS,
        BACKWARDS
    }
}
