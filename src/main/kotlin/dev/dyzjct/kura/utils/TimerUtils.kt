package dev.dyzjct.kura.utils

open class TimerUtils(val timeUnit: TimeUnit = TimeUnit.MILLISECONDS) {
    private val current = System.currentTimeMillis()
    var time = currentTime
    private val currentTime: Long
        get() = System.currentTimeMillis()
    val passedTimeMs: Long
        get() = System.currentTimeMillis() - time


    private fun convertToNS(time: Long): Long {
        return time * 1000000L
    }

    fun passed(ms: Long): Boolean {
        return System.currentTimeMillis() - time >= ms * timeUnit.multiplier
    }

    fun passed(ms: Double): Boolean {
        return System.currentTimeMillis() - time >= ms * timeUnit.multiplier
    }

    fun setMs(ms: Long) {
        time = System.nanoTime() - convertToNS(ms)
    }

    fun tickAndReset(ms: Long): Boolean {
        if (System.currentTimeMillis() - time >= ms * timeUnit.multiplier || ms * timeUnit.multiplier < 0) {
            reset()
            return true
        }
        return false
    }

    fun tickAndReset(ms: Int): Boolean {
        if (System.currentTimeMillis() - time >= ms * timeUnit.multiplier || ms * timeUnit.multiplier < 0) {
            reset()
            return true
        }
        return false
    }

    fun tickAndReset(ms: Double): Boolean {
        if (System.currentTimeMillis() - time >= ms * timeUnit.multiplier || ms * timeUnit.multiplier < 0) {
            reset()
            return true
        }
        return false
    }

    fun tickAndReset(ms: Float): Boolean {
        if (System.currentTimeMillis() - time >= ms * timeUnit.multiplier || ms * timeUnit.multiplier < 0) {
            reset()
            return true
        }
        return false
    }

    fun reset() {
        time = System.currentTimeMillis()
    }

    fun reset(offset: Int) {
        time = System.currentTimeMillis() + offset
    }

    fun reset(offset: Long) {
        time = System.currentTimeMillis() + offset
    }

    fun passedMs(ms: Long): Boolean {
        return System.currentTimeMillis() - time >= ms * timeUnit.multiplier
    }

    fun passed(ms: Float): Boolean {
        return System.currentTimeMillis() - time >= ms * timeUnit.multiplier
    }

    fun passed(ms: Int): Boolean {
        return System.currentTimeMillis() - time >= ms * timeUnit.multiplier
    }
}

enum class TimeUnit(val multiplier: Long) {
    MILLISECONDS(1L),
    TICKS(50L),
    SECONDS(1000L),
    MINUTES(60000L),
    HOURS(3600000L)
}