package dev.dyzjct.kura.utils.animations

class AnimationFlag(private val interpolation: (Long, Float, Float) -> Float) {

    constructor(easing: Easing, length: Float) : this({ time, prev, current ->
        easing.incOrDec(Easing.toDelta(time, length), prev, current)
    })

    var prev = 0.0f; private set
    var current = 0.0f; private set
    var time = System.currentTimeMillis(); private set

    fun forceUpdate(prev: Float, current: Float): Float {
        if (prev.isNaN() || current.isNaN()) return this.prev

        this.prev = prev
        this.current = current
        time = System.currentTimeMillis()
        return prev
    }

    fun forceUpdate(all: Float): Float = forceUpdate(all, all)

    fun getAndUpdate(input: Float): Float = get(input, true)


    fun update(input: Float) {
        if (!input.isNaN() && this.current != input) {
            this.prev = this.current
            this.current = input
            this.time = System.currentTimeMillis()
        }
    }

    fun get(input: Float, update: Boolean = false): Float {
        val render = interpolation.invoke(time, prev, current)

        if (update && !input.isNaN() && input != this.current) {
            this.prev = render
            this.current = input
            this.time = System.currentTimeMillis()
        }

        return render
    }
}