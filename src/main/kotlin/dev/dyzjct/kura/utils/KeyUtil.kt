package dev.dyzjct.kura.utils

import org.lwjgl.glfw.GLFW

object KeyUtil {
    private val map = mutableMapOf<String, Int>()

    init {
        val prefix = "GLFW_KEY_"
        GLFW::class.java.declaredFields
            .filter { it.name.startsWith(prefix) }
            .forEach {
                map[it.name.removePrefix(prefix)] = it.get(null) as Int
            }
    }

    fun parseToKeyCode(keyName: String): Int {
        return map[keyName] ?: -1
    }
}