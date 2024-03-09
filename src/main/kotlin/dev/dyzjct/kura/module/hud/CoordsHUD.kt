package dev.dyzjct.kura.module.hud

import dev.dyzjct.kura.module.HUDModule
import dev.dyzjct.kura.module.modules.client.Colors
import base.system.event.SafeClientEvent
import base.system.render.newfont.FontRenderers
import base.utils.chat.ChatUtil
import base.utils.concurrent.threads.runSafe
import net.minecraft.client.gui.DrawContext
import net.minecraft.world.dimension.DimensionTypes

object CoordsHUD : HUDModule(name = "CoordsHUD", langName = "坐标显示", x = 150f, y = 150f) {
    override fun onRender(context: DrawContext) {
        runSafe {
            val fontColor = Colors.hudColor.value
            val s = if (isPlayerInHell()) 8.0 else 0.125
            val nowWorldPos = String.format("%.1f, %.1f, %.1f", player.x, player.y, player.z)
            val otherWorldPos = String.format("%.1f, %.1f, %.1f", player.x * s, player.y, player.z * s)
            val final =
                    if (isPlayerInHell())
                        "XYZ ${setRed(nowWorldPos)} [$otherWorldPos]"
                    else
                        "XYZ $nowWorldPos [${setRed(otherWorldPos)}]"
            FontRenderers.lexend.drawString(context.matrices, final, x + 2.0, y + 4.0, fontColor.rgb)
            this@CoordsHUD.width = FontRenderers.lexend.getStringWidth(final) + 4f
        }
    }

    private fun setRed(str: String) = ChatUtil.RED + str + ChatUtil.RESET

    private fun SafeClientEvent.isPlayerInHell() = player.world.dimensionKey == DimensionTypes.THE_NETHER
}