package dev.dyzjct.kura.utils.delegate

import dev.dyzjct.kura.utils.TimeUnit
import dev.dyzjct.kura.utils.TimerUtils
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

open class CachedValue<T>(
    protected val updateTime: Long,
    timeUnit: TimeUnit = TimeUnit.MILLISECONDS,
    protected val block: () -> T
) : ReadWriteProperty<Any?, T> {
    protected var value: T? = null
    protected val timer = TimerUtils(timeUnit)

    open fun get(): T {
        val cached = value

        return if (cached == null || timer.tickAndReset(updateTime)) {
            block().also { value = it }
        } else {
            cached
        }
    }

    open fun update() {
        timer.reset(-updateTime * timer.timeUnit.multiplier - 1L)
    }

    fun shouldUpdate() = timer.passed(updateTime)

    final override operator fun getValue(thisRef: Any?, property: KProperty<*>) = get()

    final override operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.value = value
    }
}
