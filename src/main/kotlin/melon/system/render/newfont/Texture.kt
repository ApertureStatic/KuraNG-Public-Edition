package melon.system.render.newfont

import net.minecraft.util.Identifier
import java.util.*

class Texture : Identifier {
    constructor(path: String) : super("melon", validatePath(path))
    constructor(i: Identifier) : super(i.namespace, i.path)

    companion object {
        fun validatePath(path: String): String {
            if (isValid(path)) {
                return path
            }
            val ret = StringBuilder()
            for (c in path.lowercase(Locale.getDefault()).toCharArray()) {
                if (isPathCharacterValid(c)) {
                    ret.append(c)
                }
            }
            return ret.toString()
        }
    }
}