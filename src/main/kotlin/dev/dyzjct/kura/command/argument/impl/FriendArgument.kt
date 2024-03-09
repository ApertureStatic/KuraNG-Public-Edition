package dev.dyzjct.kura.command.argument.impl

import dev.dyzjct.kura.manager.FriendManager
import dev.dyzjct.kura.command.argument.Argument

class FriendArgument(index: Int) : Argument<String>(index) {
    override fun complete(input: String): List<String> {
        return FriendManager.friendStringList
            .filter { it.startsWith(input, true) }
    }

    override fun convertToType(input: String): String {
        return FriendManager.friendStringList.first {
            it.equals(input, ignoreCase = true)
        }
    }

    override fun toString(): String {
        return "[Friend]"
    }
}