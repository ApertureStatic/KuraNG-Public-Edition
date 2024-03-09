package melon.system.event

interface ICancellable {
    var cancelled: Boolean

    fun cancel() {
        cancelled = true
    }
}

open class Cancellable : ICancellable {
    override var cancelled = false
        set(value) {
            field = field || value
        }
}

open class ListenerOwner : IListenerOwner {
    private val listeners = ArrayList<Listener>()
    private val parallelListeners = ArrayList<ParallelListener>()

    override fun register(listener: Listener) {
        listeners.add(listener)
    }

    override fun register(listener: ParallelListener) {
        parallelListeners.add(listener)
    }

    override fun subscribe() {
        for (listener in listeners) {
            EventBus[listener.eventID].subscribe(listener)
        }
        for (listener in parallelListeners) {
            EventBus[listener.eventID].subscribe(listener)
        }
    }

    override fun unsubscribe() {
        for (listener in listeners) {
            EventBus[listener.eventID].unsubscribe(listener)
        }
        for (listener in parallelListeners) {
            EventBus[listener.eventID].unsubscribe(listener)
        }
    }
}

interface AlwaysListening : IListenerOwner {
    override fun register(listener: Listener) {
        EventBus[listener.eventID].subscribe(listener)
    }

    override fun register(listener: ParallelListener) {
        EventBus[listener.eventID].subscribe(listener)
    }

    override fun subscribe() {}

    override fun unsubscribe() {}
}

interface IListenerOwner {
    fun register(listener: Listener)
    fun register(listener: ParallelListener)

    fun subscribe()
    fun unsubscribe()
}

interface Event : IEventPosting {
    fun post() = post(this)
}

interface IEventPosting {
    val eventBus: EventBus

    fun post(event: Any)
}

enum class StageType {
    START, END
}