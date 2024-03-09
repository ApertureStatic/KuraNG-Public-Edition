package dev.dyzjct.kura.command

import dev.dyzjct.kura.command.argument.ArgumentTree
import dev.dyzjct.kura.command.argument.impl.StringArgument
import base.system.util.interfaces.Alias

abstract class Command(
    final override val name: String,
    final override val alias: Array<out String> = emptyArray(),
    val description: String = "Empty"
) : Alias {
    private val root = ArgumentTree(StringArgument(0, name, alias, true))
    protected val builder = CommandBuilder(0, root)

    fun complete(args: List<String>): List<String> {
        return root.complete(args)
    }

    fun invoke(input: String) {
        if (input.isEmpty()) {
            return
        }

        root.invoke(input)
    }

    fun getArgumentTreeString(): String {
        return root.getArgumentTreeString()
    }
}