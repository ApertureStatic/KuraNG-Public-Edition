package dev.dyzjct.kura.module.modules.client

import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module

object Rotations:Module(name = "Rotations", category = Category.CLIENT, description = "Rotation's Settings.") {
    var movement_fix by bsetting("MovementFix", false)
    var smooth_rotation by bsetting("SmoothRotation", false)
    val smooth_factor by fsetting("SmoothFactor", 4.0F, 0.0F, 16.0F)
    var grim_rotation by bsetting("GrimRotation", false)
    val fov by isetting("FOV", 10, 0, 360)
}