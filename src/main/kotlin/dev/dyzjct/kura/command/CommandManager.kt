package dev.dyzjct.kura.command

import dev.dyzjct.kura.command.impl.*
import dev.dyzjct.kura.Kura

object CommandManager {
    private val commands = mutableListOf<Command>()

    private val commandPrefix
        get() = Kura.commandPrefix.value

    fun onInit() {
        addCommand(BindCommand)
        addCommand(HelpCommand)
        addCommand(FriendCommand)
        addCommand(FakePlayerCommand)
        addCommand(PrefixCommand)
        addCommand(ResetUiCommand)
        addCommand(ConfigCommand)
        addCommand(IRCCommand)
    }

    fun addCommand(command: Command) {
        commands.add(command)
    }

    fun complete(args: List<String>): List<String> {
        if (args.isEmpty()) {
            return emptyList()
        }

        return commands.flatMap { it.complete(args) }
    }

    fun invoke(input: String): String? {
        return runCatching {
            commands.forEach { it.invoke(input.removePrefix(commandPrefix)) }
        }.exceptionOrNull()?.message
    }

    fun getHelpMessage(): String {
        val builder = StringBuilder()
        builder.appendLine("Commands:")
        builder.appendLine("----- Divider -----")
        commands.forEach {
            builder.appendLine(" Command: ${it.name}")
            if (it.alias.isNotEmpty()) {
                builder.appendLine(" Alias: ${it.alias.joinToString(" ")}")
            }
            builder.appendLine(" Description: ${it.description}")
            builder.appendLine(" Usage:")

            val lines = it.getArgumentTreeString().lines()
            val usage = lines
                .filter { line -> line.isNotEmpty() }
                .joinToString("\n") {
                    " - $commandPrefix$it"
                }

            builder.appendLine(usage)
            builder.appendLine("----- Divider -----")
        }
        return builder.toString()
    }
}