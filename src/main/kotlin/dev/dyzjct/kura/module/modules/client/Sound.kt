package dev.dyzjct.kura.module.modules.client

import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module

object Sound : Module(
    name = "Sound",
    langName = "播放音频",
    category = Category.CLIENT,
    description = "Play Sound."
) {
    val volume by fsetting("Volume", 0.5f, 0.0f, 1.0f)
    val mode = msetting("Mode", SoundMode.Sigma)
    val ezz by bsetting("EZZ", true)

    enum class SoundMode {
        Sigma, FDP ,Never
    }
}