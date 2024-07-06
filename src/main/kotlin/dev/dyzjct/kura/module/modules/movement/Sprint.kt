package dev.dyzjct.kura.module.modules.movement

import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.event.events.player.PlayerMotionEvent
import dev.dyzjct.kura.event.eventbus.safeEventListener
import base.utils.entity.EntityUtils

object Sprint : Module(
    name = "Sprint",
    langName = "强制疾跑",
    description = "Automatically makes the player sprint",
    category = Category.MOVEMENT
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