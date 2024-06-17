package dev.dyzjct.kura.module.hud

import dev.dyzjct.kura.event.eventbus.SafeClientEvent
import base.utils.concurrent.threads.runSafe
import base.utils.math.distanceSqTo
import dev.dyzjct.kura.gui.clickgui.render.DrawScope
import dev.dyzjct.kura.manager.EntityManager
import dev.dyzjct.kura.manager.FriendManager
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.HUDModule
import dev.dyzjct.kura.module.modules.crystal.CrystalHelper.scaledHealth
import dev.dyzjct.kura.utils.animations.MathUtils
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.Formatting
import java.awt.Color

object PlayerListHUD :
    HUDModule(name = "PlayerListHUD", langName = "玩家列表", x = 170f, y = 170f, category = Category.HUD) {
    private val SafeClientEvent.playerList: List<PlayerEntity>
        get() = EntityManager.players.stream()
            .filter { it != player }
            .sorted(Comparator.comparingDouble { player.distanceSqTo(it.pos) })
            .toList()

    private val highlightList = arrayOf(
        "fin",
        "lemon",
        "yangchao",
        "nigger",
        "xbs1"
    )

    override fun DrawScope.renderOnGame() {
        runSafe {
            var offsetY = 0f
            var maxTextWidth = 75f

            if (playerList.isEmpty()) {
                drawText(
                    "${Formatting.BOLD}${Formatting.GOLD}PlayerList Is Empty!",
                    x,
                    y,
                    Color.ORANGE
                )

                this@PlayerListHUD.height = textHeight
                this@PlayerListHUD.width = getTextWidth("${Formatting.BOLD}${Formatting.GOLD}PlayerList Is Empty!")

                return
            }

            playerList.forEach { p ->
                val playerName = if (highlightList.any { p.name.string.contains(it) }) {
                    "${Formatting.BOLD}${Formatting.BLACK}${p.name.string}"
                } else if (FriendManager.isFriend(p)) {
                    "${Formatting.AQUA}${p.name.string}"
                } else {
                    "${Formatting.RED}${p.name.string}"
                }

                val text =
                    "- ${Formatting.GREEN}${
                        MathUtils.round(
                            p.scaledHealth,
                            2
                        )
                    } $playerName ${Formatting.RESET}[${Formatting.BOLD}${Formatting.YELLOW}${
                        MathUtils.round(
                            p.distanceTo(player),
                            2
                        )
                    }${Formatting.RESET}]"

                drawText(
                    text,
                    PlayerListHUD.x,
                    PlayerListHUD.y + offsetY,
                    Color.WHITE
                )

                val playNameWidth = getTextWidth(text)
                if (playNameWidth > maxTextWidth) {
                    maxTextWidth = playNameWidth
                }

                offsetY += textHeight + 2
            }

            this@PlayerListHUD.height = offsetY
            this@PlayerListHUD.width = maxTextWidth
        }
    }

//    override fun onRender(context: DrawContext) {
//        runSafe {
//            var offsetY = 0f
//            var maxFontWidth = 75f
//            if (playerList.isEmpty()) {
//                FontRenderers.cn.drawString(
//                    context.matrices,
//                    "${Formatting.BOLD}${Formatting.GOLD}PlayerList Is Empty!",
//                    x,
//                    y,
//                    Color.ORANGE.rgb
//                )
//                height = FontRenderers.cn.fontHeight
//                width = FontRenderers.cn.getStringWidth("${Formatting.BOLD}${Formatting.GOLD}PlayerList Is Empty!")
//                return
//            }
//            playerList.forEach { p ->
//                val playerName = if (highlightList.any { p.name.string.contains(it) }) {
//                    "${Formatting.BOLD}${Formatting.BLACK}${p.name.string}"
//                } else if (FriendManager.isFriend(p)) {
//                    "${Formatting.AQUA}${p.name.string}"
//                } else {
//                    "${Formatting.RED}${p.name.string}"
//                }
//
//                val text =
//                    "- ${Formatting.GREEN}${
//                        MathUtils.round(
//                            p.scaledHealth,
//                            2
//                        )
//                    } $playerName ${Formatting.RESET}[${Formatting.BOLD}${Formatting.YELLOW}${
//                        MathUtils.round(
//                            p.distanceTo(player),
//                            2
//                        )
//                    }${Formatting.RESET}]"
//
//                FontRenderers.cn.drawString(context.matrices, text, x, y + offsetY, Color.WHITE.rgb)
//                val playNameWidth = FontRenderers.cn.getStringWidth(text)
//                if (playNameWidth > maxFontWidth) {
//                    maxFontWidth = playNameWidth
//                }
//                offsetY += FontRenderers.cn.fontHeight + 2
//            }
//            height = offsetY
//            width = maxFontWidth
//        }
//    }
}