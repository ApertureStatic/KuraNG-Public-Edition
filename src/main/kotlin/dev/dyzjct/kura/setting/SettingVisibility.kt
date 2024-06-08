package dev.dyzjct.kura.setting

interface ISettingVisibility {
    fun <S : Setting<out Any>> addVisibility(visibleBlock: () -> Boolean): S
}

interface SettingVisibility<S : Setting<out Any>> : ISettingVisibility {

    fun isTrue(value: Setting<Boolean>): S {
        return addVisibility { value.value }
    }

    fun isTrue(value: () -> Boolean): S {
        return addVisibility { value() }
    }

    fun isFalse(value: Setting<Boolean>): S {
        return addVisibility { !value.value }
    }

    fun isFalse(value: () -> Boolean): S {
        return addVisibility { !value() }
    }

    fun inRange(value: Setting<Int>, min: Int, max: Int): S {
        return addVisibility { value.value in min..max }
    }

    fun inRange(value: Int, min: Int, max: Int): S {
        return addVisibility { value in min..max }
    }

    fun inRange(value: Setting<Long>, min: Long, max: Long): S {
        return addVisibility { value.value in min..max }
    }

    fun inRange(value: Long, min: Long, max: Long): S {
        return addVisibility { value in min..max }
    }

    fun inRange(value: Setting<Float>, min: Float, max: Float): S {
        return addVisibility { value.value in min..max }
    }

    fun inRange(value: Float, min: Float, max: Float): S {
        return addVisibility { value in min..max }
    }

    fun inRange(value: Setting<Double>, min: Double, max: Double): S {
        return addVisibility { value.value in min..max }
    }

    fun inRange(value: Double, min: Double, max: Double): S {
        return addVisibility { value in min..max }
    }

    fun biggerThan(value: Setting<Int>, min: Int): S {
        return addVisibility { value.value >= min }
    }

    fun biggerThan(value: Int, min: Int): S {
        return addVisibility { value >= min }
    }

    fun biggerThan(value: Setting<Long>, min: Long): S {
        return addVisibility { value.value >= min }
    }

    fun biggerThan(value: Long, min: Long): S {
        return addVisibility { value >= min }
    }

    fun biggerThan(value: Setting<Float>, min: Float): S {
        return addVisibility { value.value >= min }
    }

    fun biggerThan(value: Float, min: Float): S {
        return addVisibility { value >= min }
    }

    fun biggerThan(value: Setting<Double>, min: Double): S {
        return addVisibility { value.value >= min }
    }

    fun biggerThan(value: Double, min: Double): S {
        return addVisibility { value >= min }
    }

    fun lessThan(value: Setting<Int>, max: Int): S {
        return addVisibility { value.value <= max }
    }

    fun lessThan(value: Int, max: Int): S {
        return addVisibility { value <= max }
    }

    fun lessThan(value: Setting<Long>, max: Long): S {
        return addVisibility { value.value <= max }
    }

    fun lessThan(value: Long, max: Long): S {
        return addVisibility { value <= max }
    }

    fun lessThan(value: Setting<Float>, max: Float): S {
        return addVisibility { value.value <= max }
    }

    fun lessThan(value: Float, max: Float): S {
        return addVisibility { value <= max }
    }

    fun lessThan(value: Setting<Double>, max: Double): S {
        return addVisibility { value.value <= max }
    }

    fun lessThan(value: Double, max: Double): S {
        return addVisibility { value <= max }
    }

    fun enumIs(value: Setting<out Enum<*>>, target: Enum<*>): S {
        return addVisibility { value.value == target }
    }

    fun enumIs(value: Enum<*>, target: Enum<*>): S {
        return addVisibility { value == target }
    }

    fun enumIsNot(value: Setting<out Enum<*>>, target: Enum<*>): S {
        return addVisibility { value.value !== target }
    }

    fun enumIsNot(value: Enum<*>, target: Enum<*>): S {
        return addVisibility { value !== target }
    }
}