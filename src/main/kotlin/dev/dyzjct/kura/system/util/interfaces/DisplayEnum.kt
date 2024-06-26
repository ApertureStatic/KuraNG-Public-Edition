package dev.dyzjct.kura.system.util.interfaces

interface DisplayEnum {
    val displayName: CharSequence
    val displayString: String get() = displayName.toString()
}