package dev.dyzjct.kura.utils

import org.lwjgl.glfw.GLFW

object KeyUtil {
    private val map = mutableMapOf<String, Int>()

    init {
        val prefix = "GLFW_KEY_"
        GLFW::class.java.declaredFields
            .filter { it.name.startsWith(prefix) }
            .forEach {
                dev.dyzjct.kura.utils.KeyUtil.map[it.name.removePrefix(prefix)] = it.get(null) as Int
            }
    }

    fun parseToKeyCode(keyName: String): Int {
        return dev.dyzjct.kura.utils.KeyUtil.map[keyName] ?: -1
    }
}