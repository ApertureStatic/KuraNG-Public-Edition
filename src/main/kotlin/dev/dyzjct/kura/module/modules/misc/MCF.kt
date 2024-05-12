package dev.dyzjct.kura.module.modules.misc

import dev.dyzjct.kura.manager.FriendManager
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import base.events.input.MouseClickEvent
import base.system.event.safeEventListener
import base.utils.chat.ChatUtil
import base.utils.screen.ScreenUtils.safeReturn
import net.minecraft.entity.player.PlayerEntity

object MCF : Module(
    name = "MCF",
    langName = "中键添加好友",
    description = "Middle click to add friends",
    category = Category.MISC,
    type = Type.Both
) {
    init {
        safeEventListener<MouseClickEvent> {
            val entity = mc.targetedEntity
            if (it.button == MouseClickEvent.MouseButton.MIDDLE && it.action == MouseClickEvent.MouseAction.PRESS && entity != null && entity is PlayerEntity) {
                if (mc.currentScreen.safeReturn()) return@safeEventListener
                val playerName = entity.name.string
                if (FriendManager.isFriend(playerName)) {
                    FriendManager.removeFriend(playerName)
                    ChatUtil.sendMessage("Your friend $playerName has been removed")
                } else {
                    FriendManager.addFriend(playerName)
                    ChatUtil.sendMessage("Added player $playerName as your friend")
                }
            }
        }
    }
}