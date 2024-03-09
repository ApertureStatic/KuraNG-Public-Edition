package melon.utils.graphics


enum class HAlign(val multiplier: Float, val offset: Float) {
    LEFT(0.0f, -1.0f),
    CENTER(0.5f, 0.0f),
    RIGHT(1.0f, 1.0f)
}

enum class VAlign(val multiplier: Float, val offset: Float) {
    TOP(0.0f, -1.0f),
    CENTER(0.5f, 0.0f),
    BOTTOM(1.0f, 1.0f)
}