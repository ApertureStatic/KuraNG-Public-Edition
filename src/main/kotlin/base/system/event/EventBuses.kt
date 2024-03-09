package base.system.event

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import base.system.util.collections.ArrayMap
import base.system.util.interfaces.MinecraftWrapper
import base.utils.concurrent.threads.ConcurrentScope
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

open class NamedProfilerEventBus(private val profilerName: String) : ProfilerEventBus() {
    override fun post(event: Any) {
        mc.profiler.push(profilerName)
        super.post(event)
        mc.profiler.pop()
    }
}

open class ProfilerEventBus : EventBus(), MinecraftWrapper {
    override fun post(event: Any) {
        mc.profiler.push("serial/concurrent")
        for (listener in listeners) {
            mc.profiler.push(listener.ownerName)
            listener.function.invoke(event)
            mc.profiler.pop()
        }
        mc.profiler.pop()
        mc.profiler.push("parallel")
        invokeParallel(event)
        mc.profiler.pop()
    }
}

@Suppress("LeakingThis")
open class EventBus : IEventPosting {
    override val eventBus: EventBus
        get() = this

    val busID = id.getAndIncrement()

    init {
        eventBusMap[busID] = this
    }

    protected val listeners = CopyOnWriteArrayList<Listener>()
    private val parallelListeners = CopyOnWriteArrayList<ParallelListener>()

    override fun post(event: Any) {

        for (listener in listeners) {
            listener.function.invoke(event)
        }

        invokeParallel(event)
    }

    protected fun invokeParallel(event: Any) {
        if (!parallelListeners.isEmpty()) {
            runBlocking {
                for (listener in parallelListeners) {
                    launch(ConcurrentScope.context) {
                        listener.function.invoke(event)
                    }
                }
            }
        }
    }

    fun subscribe(listener: Listener) {
        for (i in listeners.indices) {
            val other = listeners[i]
            if (listener == other) {
                return
            } else if (listener.priority > other.priority) {
                if (listener != other) listeners.add(i, listener)
                return
            }
        }

        listeners.add(listener)
    }

    fun subscribe(listener: ParallelListener) {
        for (i in parallelListeners.indices) {
            val other = parallelListeners[i]
            if (listener == other) {
                return
            } else if (listener.priority > other.priority) {
                if (listener != other) parallelListeners.add(i, listener)
                return
            }
        }

        parallelListeners.add(listener)
    }

    fun unsubscribe(listener: Listener) {
        listeners.removeIf {
            it == listener
        }
    }

    fun unsubscribe(listener: ParallelListener) {
        parallelListeners.removeIf {
            it == listener
        }
    }

    companion object {
        private val id = AtomicInteger()
        private val eventBusMap = ArrayMap<EventBus>()

        operator fun get(busID: Int): EventBus {
            return eventBusMap[busID]!!
        }
    }
}