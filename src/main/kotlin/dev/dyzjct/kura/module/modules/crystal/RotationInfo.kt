package dev.dyzjct.kura.module.modules.crystal

import team.exception.melon.util.math.vector.Vec2f

class RotationInfo(var rotation: Vec2f) {
    fun update(rotation: Vec2f) {
        this.rotation = rotation
    }

    fun reset() {
        this.rotation = Vec2f.ZERO
    }
}