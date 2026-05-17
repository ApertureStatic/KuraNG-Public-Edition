package dev.dyzjct.kura.module.modules.render

import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module

object CameraClip: Module(name = "CameraClip", category = Category.RENDER) {
    var clip by bsetting("Clip", true)
    var distance by isetting("CameraDistance", 4, 0, 20)
}