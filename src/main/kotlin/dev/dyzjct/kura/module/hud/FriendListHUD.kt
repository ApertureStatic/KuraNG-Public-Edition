package dev.dyzjct.kura.module.hud

import dev.dyzjct.kura.manager.EntityManager
import dev.dyzjct.kura.manager.FriendManager
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.HUDModule
import base.system.render.newfont.FontRenderers
import net.minecraft.client.gui.DrawContext
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.Formatting
import java.awt.Color
import java.util.stream.Collectors

object FriendListHUD : HUDModule(
    name = "FriendListHUD",
    langName = "好友列表",
    x = 170f,
    y = 170f,
    category = Category.HUD
) {
    private val friendList: List<PlayerEntity>
        get() = EntityManager.players.stream()
            .filter { FriendManager.isFriend(it.name.string) }
            .collect(Collectors.toList())

    override fun onRender(context: DrawContext) {
        var addY = 4f
        var maxFontWidth = 60f
        if (friendList.isEmpty()) {
            FontRenderers.cn.drawString(
                context.matrices,
                "${Formatting.BOLD}${Formatting.GOLD}You Have No Friends!",
                x,
                y - 4f,
                Color.ORANGE.rgb
            )
            height = FontRenderers.cn.fontHeight
            width = FontRenderers.cn.getStringWidth("${Formatting.BOLD}${Formatting.GOLD}You Have No Friends!")
            return
        }
        FontRenderers.cn.drawString(
            context.matrices,
            "${Formatting.BOLD}${Formatting.GOLD}Your Friends: ",
            x,
            y - 4f,
            Color.ORANGE.rgb
        )
        friendList.forEach { p ->
            val text = "- ${p.entityName}"
            FontRenderers.cn.drawString(context.matrices, text, x, y + addY, Color.WHITE.rgb)
            maxFontWidth = maxFontWidth.coerceAtLeast(FontRenderers.cn.getStringWidth(text))
            addY += FontRenderers.cn.fontHeight + 2
        }
        height = if (addY == FontRenderers.cn.fontHeight) {
            FontRenderers.cn.fontHeight
        } else {
            (addY - FontRenderers.cn.fontHeight)
        }
        width = maxFontWidth
    }
}