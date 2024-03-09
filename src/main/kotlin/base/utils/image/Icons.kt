package base.utils.image

import dev.dyzjct.kura.Kura
import java.io.IOException
import java.io.InputStream

object Icons {
    @Throws(IOException::class)
    fun getIcons(): List<InputStream?> {
        val inputstream = Kura::class.java.getResourceAsStream("/assets/kura/logo/logo.png")
        return listOf(inputstream)
    }
}