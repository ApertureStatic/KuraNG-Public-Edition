package dev.dyzjct.kura.utils.animations

import kotlin.math.*

@Suppress("NOTHING_TO_INLINE")
object MathUtils {
    inline fun ceilToPOT(valueIn: Int): Int {
        // Magical bit shifting
        var i = valueIn
        i--
        i = i or (i shr 1)
        i = i or (i shr 2)
        i = i or (i shr 4)
        i = i or (i shr 8)
        i = i or (i shr 16)
        i++
        return i
    }

    fun rad(angle: Float): Float {
        return (angle * Math.PI / 180).toFloat()
    }
    @JvmStatic
    inline fun random(min: Int, max: Int): Int {
        return min + (Math.random() * ((max - min) + 1)).toInt()
    }

    @JvmStatic
    inline fun random(min: Float, max: Float): Float {
        return (Math.random() * (max - min) + min).toFloat()
    }

    @JvmStatic
    inline fun random(value: Float, places: Int): Float {
        val scale = 10.0f.pow(places)
        return kotlin.math.round(value * scale) / scale
    }
    inline fun round(value: Float, places: Int): Float {
        val scale = 10.0f.pow(places)
        return round(value * scale) / scale
    }

    inline fun round(value: Double, places: Int): Double {
        val scale = 10.0.pow(places)
        return round(value * scale) / scale
    }

    inline fun decimalPlaces(value: Double) = value.toString().split('.').getOrElse(1) { "0" }.length

    inline fun decimalPlaces(value: Float) = value.toString().split('.').getOrElse(1) { "0" }.length

    inline fun isNumberEven(i: Int): Boolean {
        return i and 1 == 0
    }

    inline fun reverseNumber(num: Int, min: Int, max: Int): Int {
        return max + min - num
    }

    inline fun convertRange(valueIn: Int, minIn: Int, maxIn: Int, minOut: Int, maxOut: Int): Int {
        return convertRange(
            valueIn.toDouble(),
            minIn.toDouble(),
            maxIn.toDouble(),
            minOut.toDouble(),
            maxOut.toDouble()
        ).toInt()
    }

    inline fun convertRange(valueIn: Float, minIn: Float, maxIn: Float, minOut: Float, maxOut: Float): Float {
        return convertRange(
            valueIn.toDouble(),
            minIn.toDouble(),
            maxIn.toDouble(),
            minOut.toDouble(),
            maxOut.toDouble()
        ).toFloat()
    }

    inline fun convertRange(valueIn: Double, minIn: Double, maxIn: Double, minOut: Double, maxOut: Double): Double {
        val rangeIn = maxIn - minIn
        val rangeOut = maxOut - minOut
        val convertedIn = (valueIn - minIn) * (rangeOut / rangeIn) + minOut
        val actualMin = min(minOut, maxOut)
        val actualMax = max(minOut, maxOut)
        return min(max(convertedIn, actualMin), actualMax)
    }

    inline fun lerp(from: Double, to: Double, delta: Double): Double {
        return from + (to - from) * delta
    }

    inline fun lerp(from: Float, to: Float, delta: Float): Float {
        return from + (to - from) * delta
    }

    fun clamp(num: Int, min: Int, max: Int): Int {
        return if (num < min) min else num.coerceAtMost(max)
    }

    fun clamp(num: Float, min: Float, max: Float): Float {
        return if (num < min) min else num.coerceAtMost(max)
    }

    fun clamp(num: Double, min: Double, max: Double): Double {
        return if (num < min) min else num.coerceAtMost(max)
    }

    fun roundToDecimal(n: Double, point: Int): Double {
        if (point == 0) {
            return floor(n)
        }
        val factor = 10.0.pow(point.toDouble())
        return round(n * factor) / factor
    }
}