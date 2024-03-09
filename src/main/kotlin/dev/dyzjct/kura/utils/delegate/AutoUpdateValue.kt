package dev.dyzjct.kura.utils.delegate

import dev.dyzjct.kura.utils.TimeUnit
import dev.dyzjct.kura.utils.TimerUtils
import melon.events.RunGameLoopEvent
import melon.system.event.AlwaysListening
import melon.system.event.listener
import kotlin.reflect.KProperty

class AutoUpdateValue<T>(private val block: () -> T) {
    private var value0: T? = null
    private var lastUpdateValueID = 0

    val value: T
        get() = get()

    init {
        instances.add(this)
    }

    fun get(): T {
        return if (lastUpdateValueID == curID) {
            getLazy()
        } else {
            getForce()
        }
    }

    fun getLazy(): T {
        return value0 ?: getForce()
    }

    fun getForce(): T {
        val value = block.invoke()
        value0 = value
        lastUpdateValueID = curID

        return value
    }

    fun updateLazy() {
        value0 = null
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return get()
    }

    private companion object : AlwaysListening {
        val instances = ArrayList<AutoUpdateValue<*>>()
        val timer = TimerUtils(TimeUnit.SECONDS)

        var curID = 0

        init {
            listener<RunGameLoopEvent.Render> {
                curID++

                if (timer.passed(1L)) {
                    curID = 0
                    instances.forEach { it.updateLazy() }
                }
            }
        }
    }
}