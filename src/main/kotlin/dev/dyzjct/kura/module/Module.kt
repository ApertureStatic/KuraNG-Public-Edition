package dev.dyzjct.kura.module

abstract class Module(
    name: String,
    langName: String = name,
    description: String = "",
    keyCode: Int = 0,
    category: Category,
    visible: Boolean = false,
    alwaysEnable: Boolean = false
) : AbstractModule() {

    init {
        moduleName = name
        moduleCName = langName
        moduleCategory = category
        bind = keyCode
        this.isVisible = visible
        this.alwaysEnable = alwaysEnable
        this.description = description
    }
}