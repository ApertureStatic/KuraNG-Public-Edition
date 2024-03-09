package dev.dyzjct.kura.utils.math

object FrameRateCounter {
    private val records: MutableList<Long> = ArrayList()
    fun recordFrame() {
        val c = System.currentTimeMillis()
        records.add(c)
    }

    val fps: Int
        get() {
            records.removeIf { aLong: Long -> aLong + 1000 < System.currentTimeMillis() }
            return records.size / 2
        }
}
