package dev.dyzjct.kura

import net.minecraft.util.Identifier
import java.util.*

class KuraIdentifier(path: String) {
    private val identifier: Identifier

    init {
        val namespace = Kura.MOD_NAME.lowercase(Locale.ROOT)
        val validatedPath = validatePath(path)
        identifier = Identifier.of(namespace, validatedPath)
    }

    companion object {
        fun validatePath(path: String): String {
            if (Identifier.isPathValid(path)) return path

            val ret = StringBuilder()
            for (c in path.lowercase(Locale.getDefault()).toCharArray()) {
                if (Identifier.isPathCharacterValid(c)) {
                    ret.append(c)
                }
            }
            return ret.toString()
        }
    }

    // 添加一些方法来暴露 Identifier 的功能
    fun getNamespace(): String {
        return identifier.namespace
    }

    fun getPath(): String {
        return identifier.path
    }

    override fun toString(): String {
        return identifier.toString()
    }
}