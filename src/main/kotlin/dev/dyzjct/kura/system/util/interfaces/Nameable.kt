package dev.dyzjct.kura.system.util.interfaces

interface Nameable {
    val name: CharSequence
    val nameAsString: String get() = name.toString()
    val internalName: String get() = nameAsString.replace(" ", "")
}