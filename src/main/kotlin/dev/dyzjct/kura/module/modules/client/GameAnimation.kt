package dev.dyzjct.kura.module.modules.client

import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.utils.animations.AnimationFlag
import dev.dyzjct.kura.utils.animations.Easing
import base.utils.concurrent.threads.runSafe

object GameAnimation : Module(name = "GameAnimation", langName = "游戏动画", category = Category.CLIENT, type = Type.Both) {
    private var hotbarAnimation = AnimationFlag(Easing.OUT_CUBIC, 200.0f)
    var hotbar = bsetting("Hotbar", true)

    override fun onEnable() {
        runSafe {
            val currentPos = player.inventory.selectedSlot * 20.0f
            hotbarAnimation.forceUpdate(currentPos, currentPos)
        }
    }

    fun updateHotbar(): Float {
        return runSafe {
            val currentPos = player.inventory.selectedSlot * 20f
            return hotbarAnimation.getAndUpdate(currentPos)
        } ?: 0f
    }
}