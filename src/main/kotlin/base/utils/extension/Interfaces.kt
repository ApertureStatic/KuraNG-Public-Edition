package base.utils.extension

import dev.dyzjct.kura.system.util.interfaces.DisplayEnum
import dev.dyzjct.kura.system.util.interfaces.Nameable

val DisplayEnum.rootName: String
    get() = displayString

val Nameable.rootName: String
    get() = nameAsString