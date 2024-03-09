package base.utils.extension

import base.system.util.interfaces.DisplayEnum
import base.system.util.interfaces.Nameable

val DisplayEnum.rootName: String
    get() = displayString

val Nameable.rootName: String
    get() = nameAsString