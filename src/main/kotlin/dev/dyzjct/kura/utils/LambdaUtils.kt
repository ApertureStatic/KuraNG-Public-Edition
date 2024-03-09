package dev.dyzjct.kura.utils

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

val FALSE_BLOCK = { false }

infix fun (() -> Boolean).or(block: (() -> Boolean)): () -> Boolean {
    return {
        this.invoke() || block.invoke()
    }
}

infix fun (() -> Boolean).and(block: (() -> Boolean)): () -> Boolean {
    return {
        this.invoke() && block.invoke()
    }
}

@OptIn(ExperimentalContracts::class)
inline fun <T> T.runIf(boolean: Boolean, block: T.() -> T): T {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }

    return if (boolean) block.invoke(this) else this
}