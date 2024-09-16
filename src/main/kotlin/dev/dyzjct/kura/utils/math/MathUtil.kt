package dev.dyzjct.kura.utils.math

import net.minecraft.entity.LivingEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

object MathUtil {
    fun clamp(num: Float, min: Float, max: Float): Float {
        return if (num < min) min else min(num.toDouble(), max.toDouble()).toFloat()
    }

    fun clamp(value: Double, min: Double, max: Double): Double {
        if (value < min) return min
        return min(value, max)
    }

    fun round(value: Double, places: Int): Double {
        var bd = BigDecimal(value)
        bd = bd.setScale(places, RoundingMode.HALF_UP)
        return bd.toDouble()
    }

    fun square(input: Double): Double {
        return input * input
    }

    fun random(min: Float, max: Float): Float {
        return (Math.random() * (max - min) + min).toFloat()
    }

    fun random(min: Double, max: Double): Double {
        return (Math.random() * (max - min) + min).toFloat().toDouble()
    }

    fun rad(angle: Float): Float {
        return (angle * Math.PI / 180).toFloat()
    }

    fun interpolate(previous: Double, current: Double, delta: Double): Double {
        return previous + (current - previous) * delta
    }

    fun interpolate(previous: Float, current: Float, delta: Float): Float {
        return previous + (current - previous) * delta
    }

    fun getFacingOrder(yaw: Float, pitch: Float): Direction {
        val f = pitch * 0.017453292f
        val g = -yaw * 0.017453292f
        val h: Float = sin(f)
        val i: Float = cos(f)
        val j: Float = sin(g)
        val k: Float = cos(g)
        val bl = j > 0.0f
        val bl2 = h < 0.0f
        val bl3 = k > 0.0f
        val l = if (bl) j else -j
        val m = if (bl2) -h else h
        val n = if (bl3) k else -k
        val o = l * i
        val p = n * i
        val direction = if (bl) Direction.EAST else Direction.WEST
        val direction2 = if (bl2) Direction.UP else Direction.DOWN
        val direction3 = if (bl3) Direction.SOUTH else Direction.NORTH
        return if (l > n) {
            if (m > o) {
                direction2
            } else {
                direction
            }
        } else if (m > p) {
            direction2
        } else {
            direction3
        }
    }

    fun getDirectionFromEntityLiving(pos: BlockPos, entity: LivingEntity): Direction {
        if (abs(entity.x - (pos.x.toDouble() + 0.5)) < 2.0 && abs(
                entity.z - (pos.z.toDouble() + 0.5)
            ) < 2.0
        ) {
            val d0: Double = entity.y + entity.getEyeHeight(entity.pose).toDouble()
            if (d0 - pos.y.toDouble() > 2.0) {
                return Direction.UP
            }

            if (pos.y.toDouble() - d0 > 0.0) {
                return Direction.DOWN
            }
        }

        return entity.horizontalFacing.opposite
    }
}
