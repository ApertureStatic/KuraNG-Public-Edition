package dev.dyzjct.kura.module.modules.misc

import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import melon.utils.chat.ChatUtil
import net.minecraft.network.packet.c2s.play.*

object PacketLogger : Module("PacketLogger", langName = "抓包", category = Category.MISC) {
    private var jump by bsetting("Jump", false)
    private var move by bsetting("Move", false)
    private var place by bsetting("Place", false)
    private var jumped = false
    private var height = 0.0

    init {
        onPacketSend { event ->
            if (event.packet is PlayerMoveC2SPacket && move) {
                val packetMode =
                    if (event.packet is PlayerMoveC2SPacket.Full) "Full" else if (event.packet is PlayerMoveC2SPacket.PositionAndOnGround) "PositionAndOnGround" else if (event.packet is PlayerMoveC2SPacket.LookAndOnGround) "LookAndOnGround" else "OnGroundOnly"
                ChatUtil.sendMessage("${packetMode}:${event.packet.x.toInt()} ${event.packet.y.toInt()} ${event.packet.z.toInt()}")
            }
            if (event.packet is PlayerInteractBlockC2SPacket && place) {
                ChatUtil.sendMessage("Pos:" + event.packet.blockHitResult.blockPos.toString())
            }
            if (event.packet is PlayerActionC2SPacket) {
                ChatUtil.sendMessage(event.packet.action.name)
            }
            if (event.packet is PlayerMoveC2SPacket && jumped && jump) {
                if (player.onGround) {
                    jumped = false
                    height = 0.0
                } else {
                    ChatUtil.sendMessage((event.packet.y - height).toString())
                }
            }
        }

        onMotion {
            if (mc.options.jumpKey.isPressed && jump) {
                jumped = true
                height = player.y
                ChatUtil.sendMessage("Check Enabled!")
            }
        }
    }
}