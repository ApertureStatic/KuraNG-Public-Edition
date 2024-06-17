package dev.dyzjct.kura.system.util.collections

interface MutableIntIterator : MutableIterator<Int> {
    override fun next(): Int = nextInt()

    fun nextInt(): Int
}