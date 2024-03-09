package melon.utils.extension

import java.util.concurrent.CopyOnWriteArrayList

fun <E : Any> ArrayList<E>.addIfNotContains(target: E) {
    if (!contains(target)) {
        add(target)
    }
}

fun <E : Any> CopyOnWriteArrayList<E>.addIfNotContains(target: E) {
    if (!contains(target)) {
        add(target)
    }
}