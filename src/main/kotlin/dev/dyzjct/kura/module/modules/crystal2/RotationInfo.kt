package dev.dyzjct.kura.module.modules.crystal2

import base.utils.math.vector.Vec2f

class RotationInfo(var rotation: Vec2f) {
    fun update(rotation: Vec2f) {
        this.rotation = rotation
    }

    fun reset() {
        this.rotation = Vec2f.ZERO
    }
}