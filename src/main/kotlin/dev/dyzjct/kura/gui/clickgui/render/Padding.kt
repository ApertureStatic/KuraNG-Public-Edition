package dev.dyzjct.kura.gui.clickgui.render

data class Padding(
    val right: Float = 0f,
    val left: Float = 0f,
    val top: Float = 0f,
    val bottom: Float = 0f
) {
    constructor(horizontal: Float = 0f, vertical: Float = 0f) : this(horizontal, horizontal, vertical, vertical)

    companion object {
        val Empty = Padding()
    }
}