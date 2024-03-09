package dev.dyzjct.kura.command.argument.impl

import dev.dyzjct.kura.command.argument.Argument
import dev.dyzjct.kura.utils.KeyUtil

class KeyArgument(index: Int) : Argument<Int>(index) {
    override fun complete(input: String): List<String> {
        return listOf("[Key]")
    }

    override fun convertToType(input: String): Int {
        val keyCode = dev.dyzjct.kura.utils.KeyUtil.parseToKeyCode(input.uppercase())

        if (keyCode == -1) {
            throw Exception("Unknown key name")
        }

        return keyCode
    }

    override fun toString(): String {
        return "[Key]"
    }
}