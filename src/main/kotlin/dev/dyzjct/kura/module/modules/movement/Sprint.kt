package dev.dyzjct.kura.module.modules.movement

import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import base.events.player.PlayerMotionEvent
import base.system.event.safeEventListener
import base.utils.entity.EntityUtils

object Sprint : Module(
    name = "Sprint",
    description = "Automatically makes the player sprint",
    langName = "强制疾跑",
    category = Category.MOVEMENT,
    safeModule = true
) {
    private var legit = bsetting("Legit", false)

    init {
        safeEventListener<PlayerMotionEvent> {
            if (legit.value) {
                mc.options.sprintKey.isPressed = true
            } else {
                if (EntityUtils.isMoving()) {
                    player.isSprinting = true
                }
            }
        }
    }
}