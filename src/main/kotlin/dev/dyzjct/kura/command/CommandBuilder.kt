package dev.dyzjct.kura.command

import dev.dyzjct.kura.command.argument.Argument
import dev.dyzjct.kura.command.argument.ArgumentTree
import dev.dyzjct.kura.command.argument.impl.*
import dev.dyzjct.kura.command.argument.impl.*

class CommandBuilder(
    private val index: Int,
    private val root: ArgumentTree
) {
    private val nextIndex = index + 1

    fun literal(block: CommandBuilder.() -> Unit) {
        this.block()
    }

    fun key(block: CommandBuilder.(KeyArgument) -> Unit) {
        appendArgument(KeyArgument(nextIndex), block)
    }

    fun friend(block: CommandBuilder.(FriendArgument) -> Unit) {
        appendArgument(FriendArgument(nextIndex), block)
    }

    fun any(block: CommandBuilder.(AnyArgument) -> Unit) {
        appendArgument(AnyArgument(nextIndex), block)
    }

    fun module(block: CommandBuilder.(ModuleArgument) -> Unit) {
        appendArgument(ModuleArgument(nextIndex), block)
    }

    fun match(
        string: String,
        alias: Array<String> = emptyArray(),
        ignoreCase: Boolean = false,
        block: CommandBuilder.() -> Unit
    ) {
        appendArgument(StringArgument(nextIndex, string, alias, ignoreCase)) { block() }
    }

    fun player(block: CommandBuilder.(PlayerArgument) -> Unit) {
        appendArgument(PlayerArgument(nextIndex), block)
    }

    fun executor(description: String = "Empty", block: CommandExecutor.() -> Unit) {
        val executorArgument = ExecutorArgument(nextIndex, description, block)
        root.appendArgument(ArgumentTree(executorArgument))
    }

    private fun <T : Argument<*>> appendArgument(argument: T, block: CommandBuilder.(T) -> Unit) {
        val argumentTree = ArgumentTree(argument)
        CommandBuilder(argument.index, argumentTree).block(argument)
        root.appendArgument(argumentTree)
    }
}