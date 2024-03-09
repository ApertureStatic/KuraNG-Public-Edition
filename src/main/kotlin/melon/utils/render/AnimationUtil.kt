package melon.utils.render

object AnimationUtil {
    fun moveTowards(current: Float, end: Float, smoothSpeed: Float, minSpeed: Float, back: Boolean): Float {
        var movement = (end - current) * smoothSpeed
        if (movement > 0) {
            movement = minSpeed.coerceAtLeast(movement)
            movement = (end - current).coerceAtMost(movement)
        } else if (movement < 0) {
            movement = (-minSpeed).coerceAtMost(movement)
            movement = (end - current).coerceAtLeast(movement)
        }
        return if (back) {
            current - movement
        } else {
            current + movement
        }
    }

    fun moveTowards(target: Double, current0: Double, speed: Double): Double {
        var current = current0
        val larger = target > current
        val dif = Math.max(target, current) - Math.min(target, current)
        var factor = dif * speed
        if (factor < 0.1) factor = 0.1
        if (larger) current += factor else current -= factor
        return current
    }

    fun expand(target: Double, current0: Double, speed: Double): Double {
        var current = current0
        if (current > target) current = target
        if (current < -target) current = -target
        current += speed
        return current
    }
}