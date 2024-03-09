package melon.utils.concurrent.threads

import kotlinx.coroutines.CompletableDeferred
import melon.system.event.SafeClientEvent
import melon.utils.chat.ChatUtil
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
inline fun <R> runSafeOrElse(block: SafeClientEvent.() -> R, defaultValue: R): R {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }

    val instance = SafeClientEvent.instance
    return if (instance != null) {
        block.invoke(instance)
    } else {
        defaultValue
    }
}

@OptIn(ExperimentalContracts::class)
inline fun runSafeOrFalse(block: SafeClientEvent.() -> Boolean): Boolean {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }

    val instance = SafeClientEvent.instance
    return if (instance != null) {
        block.invoke(instance)
    } else {
        false
    }
}

@OptIn(ExperimentalContracts::class)
inline fun <R> runSafe(block: SafeClientEvent.() -> R): R? {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }

    val instance = SafeClientEvent.instance
    return if (instance != null) {
        block.invoke(instance)
    } else {
        null
    }
}

suspend fun <R> runSafeSuspend(block: suspend SafeClientEvent.() -> R): R? {
    return SafeClientEvent.instance?.let { block(it) }
}

fun <T> onMainThreadSafe(block: SafeClientEvent.() -> T) =
    onMainThread { SafeClientEvent.instance?.block() }

fun <T> onMainThread(block: () -> T) =
    MainThreadExecutor.add(block)

/**
 * Runs [block] on Minecraft main thread (Client thread)
 * The [block] will the called with a [SafeClientEvent] to ensure null safety.
 *
 * @return [CompletableDeferred] callback
 *
 * @see [onMainThreadSuspend]
 */
suspend fun <T> onMainThreadSafeSuspend(block: SafeClientEvent.() -> T) =
    onMainThreadSuspend { SafeClientEvent.instance?.block() }

/**
 * Runs [block] on Minecraft main thread (Client thread)
 *
 * @return [CompletableDeferred] callback
 *
 * @see [onMainThreadSafeSuspend]
 */
suspend fun <T> onMainThreadSuspend(block: () -> T) =
    MainThreadExecutor.addSuspend(block)

@OptIn(ExperimentalContracts::class)
inline fun <T : Any, R> T.runSynchronized(block: T.() -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    return synchronized(this@runSynchronized) {
        block.invoke(this@runSynchronized)
    }
}