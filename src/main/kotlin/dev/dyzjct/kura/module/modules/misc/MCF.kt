package dev.dyzjct.kura.module.modules.misc

import dev.dyzjct.kura.manager.FriendManager
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import melon.events.input.MouseClickEvent
import melon.system.event.safeEventListener
import melon.utils.chat.ChatUtil
import melon.utils.screen.ScreenUtils
import melon.utils.screen.ScreenUtils.isMelonUIScreen
import melon.utils.screen.ScreenUtils.notWhiteListScreen
import melon.utils.screen.ScreenUtils.safeReturn
import net.minecraft.entity.player.PlayerEntity

object MCF : Module(
    name = "MCF",
    langName = "中键添加好友",
    description = "Middle click to add friends",
    category = Category.MISC
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