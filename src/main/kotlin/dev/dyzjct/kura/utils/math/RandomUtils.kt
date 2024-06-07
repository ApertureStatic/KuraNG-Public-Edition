package dev.dyzjct.kura.utils.math

import java.util.*
import kotlin.math.min

object RandomUtils {
    var random: Random = Random()

    fun randomInRange(n: Int, n2: Int): Int {
        return n + random.nextInt(n2 - n + 1)
    }

    fun randomInRange(f: Float, f2: Float): Float {
        return findMiddleValue(f + random.nextFloat() * f2, f, f2)
    }

    fun findMiddleValue(f: Float, f2: Float, f3: Float): Float {
        return if (f < f2) f2 else min(f.toDouble(), f3.toDouble()).toFloat()
    }
}
