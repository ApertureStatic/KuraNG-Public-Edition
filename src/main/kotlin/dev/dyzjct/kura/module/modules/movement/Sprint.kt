package dev.dyzjct.kura.module.modules.movement

import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import melon.events.player.PlayerMotionEvent
import melon.system.event.safeEventListener
import melon.utils.entity.EntityUtils

object Sprint : Module(
    name = "Sprint",
    description = "Automatically makes the player sprint",
    langName = "强制疾跑",
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