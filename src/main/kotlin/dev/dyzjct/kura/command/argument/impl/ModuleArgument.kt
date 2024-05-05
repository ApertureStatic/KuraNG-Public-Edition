package dev.dyzjct.kura.command.argument.impl

import dev.dyzjct.kura.module.AbstractModule
import dev.dyzjct.kura.module.ModuleManager
import dev.dyzjct.kura.command.argument.Argument

class ModuleArgument(index: Int) : Argument<AbstractModule>(index) {
    override fun complete(input: String): List<String> {
        return ModuleManager.moduleList
            .map { it.moduleName }
            .filter { it.startsWith(input, true) }
    }

    override fun convertToType(input: String): AbstractModule? {
        return ModuleManager.moduleList
            .firstOrNull { it.moduleName.equals(input, true) }
    }

    override fun toString(): String {
        return "[Module]"
    }
}