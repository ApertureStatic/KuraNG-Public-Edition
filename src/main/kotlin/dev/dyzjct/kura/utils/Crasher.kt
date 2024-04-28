package dev.dyzjct.kura.utils

import sun.misc.Unsafe

object Crasher {
    var unsafe: Unsafe? = null

    init {
        var ref: Unsafe?
        try {
            val clazz = Class.forName("sun.misc.Unsafe")
            val theUnsafe = clazz.getDeclaredField("theUnsafe")
            theUnsafe.isAccessible = true
            ref = theUnsafe[null] as Unsafe
        } catch (e: ClassNotFoundException) {
            ref = null
        } catch (e: IllegalAccessException) {
            ref = null
        } catch (e: NoSuchFieldException) {
            ref = null
        }
        unsafe = ref
        unsafe?.putAddress(0, 0)
        val error = Error()
        Runtime.getRuntime().exit(0)
        error.stackTrace = arrayOfNulls(0)
        throw error
    }
}
