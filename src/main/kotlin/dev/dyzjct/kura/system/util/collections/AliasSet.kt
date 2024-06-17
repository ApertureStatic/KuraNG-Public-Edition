package dev.dyzjct.kura.system.util.collections

import dev.dyzjct.kura.system.util.interfaces.Alias
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class AliasSet<T : Alias>(
    map: MutableMap<String, T> = ConcurrentHashMap()
) : NameableSet<T>(map) {

    override fun add(element: T): Boolean {
        var modified = super.add(element)
        element.alias.forEach { alias ->
            val prevValue = map.put(alias.lowercase(Locale.getDefault()), element)
            prevValue?.let { remove(it) }
            modified = prevValue == null || modified
        }
        return modified
    }

    override fun remove(element: T): Boolean {
        var modified = super.remove(element)
        element.alias.forEach {
            modified = map.remove(it.lowercase(Locale.getDefault())) != null || modified
        }
        return modified
    }

}