package dev.dyzjct.kura.utils.animations

import kotlin.math.*

object EaseUtils {
    fun easeInSine(x: Double): Double {
        return 1 - cos((x * PI) / 2)
    }

    fun easeOutSine(x: Double): Double {
        return sin((x * PI) / 2)
    }

    fun easeInOutSine(x: Double): Double {
        return -(cos(PI * x) - 1) / 2
    }

    fun easeInQuad(x: Double): Double {
        return x * x
    }

    fun easeOutQuad(x: Double): Double {
        return 1 - (1 - x) * (1 - x)
    }

    fun easeInOutQuad(x: Double): Double {
        return if (x < 0.5) {
            2 * x * x
        } else {
            1 - (-2 * x + 2).pow(2) / 2
        }
    }

    fun easeInCubic(x: Double): Double {
        return x * x * x
    }

    fun easeOutCubic(x: Double): Double {
        return 1 - (1 - x).pow(3)
    }

    fun easeInOutCubic(x: Double): Double {
        return if (x < 0.5) {
            4 * x * x * x
        } else {
            1 - (-2 * x + 2).pow(3) / 2
        }
    }

    fun easeInQuart(x: Double): Double {
        return x * x * x * x
    }

    fun easeOutQuart(x: Double): Double {
        return 1 - (1 - x).pow(4)
    }

    fun easeInOutQuart(x: Double): Double {
        return if (x < 0.5) {
            8 * x * x * x * x
        } else {
            1 - (-2 * x + 2).pow(4) / 2
        }
    }

    fun easeInQuint(x: Double): Double {
        return x * x * x * x * x
    }

    fun easeOutQuint(x: Double): Double {
        return 1 - (1 - x).pow(5)
    }

    fun easeInOutQuint(x: Double): Double {
        return if (x < 0.5) {
            16 * x * x * x * x * x
        } else {
            1 - (-2 * x + 2).pow(5) / 2
        }
    }

    fun easeInExpo(x: Double): Double {
        return if (x == 0.0) {
            0.0
        } else {
            2.0.pow(10 * x - 10)
        }
    }

    fun easeOutExpo(x: Double): Double {
        return if (x == 1.0) {
            1.0
        } else {
            1 - 2.0.pow(-10 * x)
        }
    }

    fun easeInOutExpo(x: Double): Double {
        return if (x == 0.0) {
            0.0
        } else {
            if (x == 1.0) {
                1.0
            } else {
                if (x < 0.5) {
                    2.0.pow(20 * x - 10) / 2
                } else {
                    (2 - 2.0.pow(-20 * x + 10)) / 2
                }
            }
        }
    }

    fun easeInCirc(x: Double): Double {
        return 1 - sqrt(1 - x.pow(2))
    }

    fun easeOutCirc(x: Double): Double {
        return sqrt(1 - (x - 1).pow(2))
    }

    fun easeInOutCirc(x: Double): Double {
        return if (x < 0.5) {
            (1 - sqrt(1 - (2 * x).pow(2))) / 2
        } else {
            (sqrt(1 - (-2 * x + 2).pow(2)) + 1) / 2
        }
    }

    fun easeInBack(x: Double): Double {
        val c1 = 1.70158
        val c3 = c1 + 1

        return c3 * x * x * x - c1 * x * x
    }

    fun easeOutBack(x: Double): Double {
        val c1 = 1.70158
        val c3 = c1 + 1

        return 1 + c3 * (x - 1).pow(3) + c1 * (x - 1).pow(2)
    }

    fun easeInOutBack(x: Double): Double {
        val c1 = 1.70158
        val c2 = c1 * 1.525

        return if (x < 0.5) {
            ((2 * x).pow(2) * ((c2 + 1) * 2 * x - c2)) / 2
        } else {
            ((2 * x - 2).pow(2) * ((c2 + 1) * (x * 2 - 2) + c2) + 2) / 2
        }
    }

    fun easeInElastic(x: Double): Double {
        val c4 = (2 * Math.PI) / 3

        return if (x == 0.0) {
            0.0
        } else {
            if (x == 1.0) {
                1.0
            } else {
                (-2.0).pow(10 * x - 10) * sin((x * 10 - 10.75) * c4)
            }
        }
    }

    fun easeOutElastic(x: Double): Double {
        val c4 = (2 * Math.PI) / 3

        return if (x == 0.0) {
            0.0
        } else {
            if (x == 1.0) {
                1.0
            } else {
                2.0.pow(-10 * x) * sin((x * 10 - 0.75) * c4) + 1
            }
        }
    }

    fun easeInOutElastic(x: Double): Double {
        val c5 = (2 * Math.PI) / 4.5

        return if (x == 0.0) {
            0.0
        } else {
            if (x == 1.0) {
                1.0
            } else {
                if (x < 0.5) {
                    -(2.0.pow(20 * x - 10) * sin((20 * x - 11.125) * c5)) / 2
                } else {
                    (2.0.pow(-20 * x + 10) * sin((20 * x - 11.125) * c5)) / 2 + 1
                }
            }
        }
    }

    enum class EnumEasingType {
        NONE,
        SINE,
        QUAD,
        CUBIC,
        QUART,
        QUINT,
        EXPO,
        CIRC,
        BACK,
        ELASTIC;

    }

}