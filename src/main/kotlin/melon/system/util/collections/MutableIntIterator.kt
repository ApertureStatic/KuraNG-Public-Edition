package melon.system.util.collections

interface MutableIntIterator : MutableIterator<Int> {
    override fun next(): Int = nextInt()

    fun nextInt(): Int
}