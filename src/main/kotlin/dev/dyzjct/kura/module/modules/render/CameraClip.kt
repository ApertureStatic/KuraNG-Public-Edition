package dev.dyzjct.kura.module.modules.render

import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module

object CameraClip: Module(name = "CameraClip", langName = "无死角视觉", category = Category.RENDER, safeModule = true) {
    var clip by bsetting("Clip", true)
    var distance by isetting("CameraDistance", 4, 0, 20)
}