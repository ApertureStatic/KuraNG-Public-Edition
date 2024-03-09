package melon.system.event

import kotlinx.coroutines.launch
import melon.utils.ClassUtils.instance
import melon.utils.concurrent.threads.runSafe
import melon.utils.concurrent.threads.runSafeSuspend
import melon.system.util.interfaces.Nameable
import melon.utils.concurrent.threads.BackgroundScope
import melon.utils.concurrent.threads.ConcurrentScope

const val DEFAULT_LISTENER_PRIORITY: Int = 0

inline fun <reified E : Event> IListenerOwner.safeEventListener(
    noinline function: SafeClientEvent.(E) -> Unit
) = listener(this, E::class.java, DEFAULT_LISTENER_PRIORITY, false) { runSafe { function.invoke(this, it) } }

inline fun <reified E : Event> IListenerOwner.safeEventListener(
    priority: Int,
    noinline function: SafeClientEvent.(E) -> Unit
) = listener(this, E::class.java, priority, false) { runSafe { function.invoke(this, it) } }

inline fun <reified E : Event> IListenerOwner.safeEventListener(
    alwaysListening: Boolean,
    noinline function: SafeClientEvent.(E) -> Unit
) = listener(this, E::class.java, DEFAULT_LISTENER_PRIORITY, alwaysListening) { runSafe { function.invoke(this, it) } }

inline fun <reified E : Event> IListenerOwner.safeEventListener(
    priority: Int,
    alwaysListening: Boolean,
    noinline function: SafeClientEvent.(E) -> Unit
) = listener(this, E::class.java, priority, alwaysListening) { runSafe { function.invoke(this, it) } }


inline fun <reified E : Event> IListenerOwner.safeParallelListener(
    noinline function: suspend SafeClientEvent.(E) -> Unit
) = parallelListener(this, E::class.java, false) { runSafeSuspend { function.invoke(this, it) } }

inline fun <reified E : Event> IListenerOwner.safeParallelListener(
    alwaysListening: Boolean,
    noinline function: suspend SafeClientEvent.(E) -> Unit
) = parallelListener(this, E::class.java, alwaysListening) { runSafeSuspend { function.invoke(this, it) } }


inline fun <reified E : Event> IListenerOwner.safeConcurrentListener(
    noinline function: suspend SafeClientEvent.(E) -> Unit
) = concurrentListener(this, E::class.java, false) { runSafeSuspend { function.invoke(this, it) } }

inline fun <reified E : Event> IListenerOwner.safeConcurrentListener(
    alwaysListening: Boolean,
    noinline function: suspend SafeClientEvent.(E) -> Unit
) = concurrentListener(this, E::class.java, alwaysListening) { runSafeSuspend { function.invoke(this, it) } }


inline fun <reified E : Event> IListenerOwner.safeBackGroundTaskListener(
    noinline function: suspend SafeClientEvent.(E) -> Unit
) = backgroundTaskListener(this, E::class.java, false) { runSafe { function.invoke(this, it) } }

inline fun <reified E : Event> IListenerOwner.safeBackGroundTaskListener(
    alwaysListening: Boolean,
    noinline function: suspend SafeClientEvent.(E) -> Unit
) = backgroundTaskListener(this, E::class.java, alwaysListening) { runSafe { function.invoke(this, it) } }


inline fun <reified E : Event> IListenerOwner.listener(
    noinline function: (E) -> Unit
) = listener(this, E::class.java, DEFAULT_LISTENER_PRIORITY, false, function)

inline fun <reified E : Event> IListenerOwner.listener(
    priority: Int,
    noinline function: (E) -> Unit
) = listener(this, E::class.java, priority, false, function)

inline fun <reified E : Event> IListenerOwner.listener(
    alwaysListening: Boolean,
    noinline function: (E) -> Unit
) = listener(this, E::class.java, DEFAULT_LISTENER_PRIORITY, alwaysListening, function)

inline fun <reified E : Event> IListenerOwner.listener(
    priority: Int,
    alwaysListening: Boolean,
    noinline function: (E) -> Unit
) = listener(this, E::class.java, priority, alwaysListening, function)


inline fun <reified E : Event> IListenerOwner.parallelListener(
    noinline function: suspend (E) -> Unit
) = parallelListener(this, E::class.java, false, function)

inline fun <reified E : Event> IListenerOwner.parallelListener(
    alwaysListening: Boolean,
    noinline function: suspend (E) -> Unit
) = parallelListener(this, E::class.java, alwaysListening, function)


inline fun <reified E : Event> IListenerOwner.concurrentListener(
    noinline function: suspend (E) -> Unit
) = concurrentListener(this, E::class.java, false, function)

inline fun <reified E : Event> IListenerOwner.concurrentListener(
    alwaysListening: Boolean,
    noinline function: suspend (E) -> Unit
) = concurrentListener(this, E::class.java, alwaysListening, function)

@Suppress("UNCHECKED_CAST")
fun <E : Event> listener(
    owner: IListenerOwner,
    eventClass: Class<E>,
    priority: Int,
    alwaysListening: Boolean,
    function: (E) -> Unit
) {
    val eventBus = getEventBus(eventClass)
    val listener = Listener(owner, eventBus.busID, priority, function as (Any) -> Unit)

    if (alwaysListening) eventBus.subscribe(listener)
    else owner.register(listener)
}

@Suppress("UNCHECKED_CAST")
fun <E : Event> parallelListener(
    owner: IListenerOwner,
    eventClass: Class<E>,
    alwaysListening: Boolean,
    function: suspend (E) -> Unit
) {
    val eventBus = getEventBus(eventClass)
    val listener = ParallelListener(owner, eventBus.busID, function as suspend (Any) -> Unit)

    if (alwaysListening) eventBus.subscribe(listener)
    else owner.register(listener)
}

@Suppress("UNCHECKED_CAST")
fun <E : Event> concurrentListener(
    owner: IListenerOwner,
    eventClass: Class<E>,
    alwaysListening: Boolean,
    function: suspend (E) -> Unit
) {
    val eventBus = getEventBus(eventClass)
    val listener =
        Listener(owner, eventBus.busID, Int.MAX_VALUE) { ConcurrentScope.launch { function.invoke(it as E) } }

    if (alwaysListening) eventBus.subscribe(listener)
    else owner.register(listener)
}

@Suppress("UNCHECKED_CAST")
fun <E : Event> backgroundTaskListener(
    owner: IListenerOwner,
    eventClass: Class<E>,
    alwaysListening: Boolean,
    function: suspend (E) -> Unit
) {
    val eventBus = getEventBus(eventClass)
    val listener =
        Listener(owner, eventBus.busID, Int.MAX_VALUE) { BackgroundScope.launch { function.invoke(it as E) } }

    if (alwaysListening) eventBus.subscribe(listener)
    else owner.register(listener)
}

private fun getEventBus(eventClass: Class<out Event>): EventBus {
    return try {
        eventClass.instance
    } catch (e: NoSuchFieldException) {
        eventClass.getDeclaredField("Companion")[null] as IEventPosting
    }.eventBus
}

class Listener(
    owner: Any,
    eventID: Int,
    priority: Int,
    function: (Any) -> Unit
) : AbstractListener<(Any) -> Unit>(owner, eventID, priority, function)

class ParallelListener(
    owner: Any,
    eventID: Int,
    function: suspend (Any) -> Unit
) : AbstractListener<suspend (Any) -> Unit>(owner, eventID, DEFAULT_LISTENER_PRIORITY, function)

sealed class AbstractListener<F>(
    owner: Any,
    val eventID: Int,
    val priority: Int,
    val function: F
) {
    val ownerName: String = if (owner is Nameable) owner.nameAsString else owner.javaClass.simpleName
}