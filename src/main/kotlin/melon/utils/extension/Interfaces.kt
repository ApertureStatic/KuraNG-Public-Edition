package melon.utils.extension

import melon.system.util.interfaces.DisplayEnum
import melon.system.util.interfaces.Nameable

val DisplayEnum.rootName: String
    get() = displayString

val Nameable.rootName: String
    get() = nameAsString