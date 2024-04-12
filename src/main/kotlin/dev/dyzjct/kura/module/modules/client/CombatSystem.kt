package dev.dyzjct.kura.module.modules.client

import base.system.event.SafeClientEvent
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket
import net.minecraft.util.Hand

object CombatSystem : Module(
    name = "CombatSystem",
    langName = "战斗系统",
    category = Category.CLIENT
) {
    val mode = msetting("SpoofMode", SpoofMode.Normal)
    val autoSwitch by bsetting("AutoSwitch", false)
    val eating by bsetting("EatingPause", true)
    val autoToggle by bsetting("[CA/AA]AutoToggle", false)
    val mainToggle by msetting("MainToggle", MainToggle.Crystal)
    var predictTicks by isetting("PredictedTicks", 4, 0, 20)
    var maxTargets by isetting("MaxTarget", 3, 1, 8)
    val targetRange by dsetting("TargetRange", 8.0, 0.0, 12.0)
    val placeRange by dsetting("PlaceRange", 6.0, 0.0, 8.0)
    val attackRange by dsetting("AttackRange", 6.0, 0.0, 8.0)
    val interactRange by dsetting("InteractRange", 6.0, 0.0, 8.0)
    val kaRange by dsetting("KARange", 6.0, 0.0, 8.0)
    private val swing by bsetting("Swing", true)
    private val packetSwing by bsetting("PacketSwing", true).isTrue{swing}

    fun SafeClientEvent.swing() {
        if (swing) {
            if (packetSwing) connection.sendPacket(HandSwingC2SPacket(Hand.MAIN_HAND)) else player.swingHand(
                Hand.MAIN_HAND)
        }
    }
    enum class SpoofMode {
        Normal, Swap, China
    }

    enum class MainToggle {
        Crystal, Anchor
    }
}