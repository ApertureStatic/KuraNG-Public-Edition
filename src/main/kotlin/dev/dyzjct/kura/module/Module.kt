package dev.dyzjct.kura.module

abstract class Module(
    name: String,
    description: String = "",
    keyCode: Int = 0,
    category: Category,
    visible: Boolean = false,
    alwaysEnable: Boolean = false
) : AbstractModule() {

    init {
        moduleName = name
        moduleCategory = category
        bind = keyCode
        this.isVisible = visible
        this.alwaysEnable = alwaysEnable
        this.description = description
    }
}