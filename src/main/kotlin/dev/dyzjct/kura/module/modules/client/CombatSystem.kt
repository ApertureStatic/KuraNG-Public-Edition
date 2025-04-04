package dev.dyzjct.kura.module.modules.client

import dev.dyzjct.kura.event.eventbus.SafeClientEvent
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.module.modules.combat.AnchorAura
import dev.dyzjct.kura.module.modules.aura.KillAura
import dev.dyzjct.kura.module.modules.crystal.AutoCrystal
import net.minecraft.item.EndCrystalItem
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket
import net.minecraft.util.Hand
import java.util.concurrent.CopyOnWriteArrayList

object CombatSystem : Module(
    name = "CombatSystem",
    langName = "战斗系统",
    category = Category.CLIENT
) {
    val spoofMode = msetting("SpoofMode", SpoofMode.Normal)
    val autoSwitch by bsetting("AutoSwitch", false)
    val eating by bsetting("EatingPause", true)
    val oldVersion by bsetting("1.12.2Support", false)
    val wallRange by dsetting("WallRange", 3.5, 0.0, 6.0).isTrue { oldVersion }
    val strictDirection by bsetting("StrictDirection", true)
    val smartAura by bsetting("SmartAura", false)
    val calculateKA by bsetting("CalculateKA", false).isTrue { smartAura }
    val autoToggle by bsetting("[CA/AA]AutoToggle", false).isFalse { smartAura }
    val mainToggle by msetting("MainToggle", MainToggle.Crystal)
        .isFalse { smartAura }
    var predictTicks by isetting("PredictedTicks", 4, 0, 20)
    var maxTargets by isetting("MaxTarget", 3, 1, 8)
    val targetRange by dsetting("TargetRange", 8.0, 0.0, 12.0)
    val placeRange by dsetting("PlaceRange", 6.0, 0.0, 8.0)
    val attackRange by dsetting("AttackRange", 6.0, 0.0, 8.0)
    val interactRange by dsetting("InteractRange", 6.0, 0.0, 8.0)
    val kaRange by dsetting("KARange", 6.0, 0.0, 8.0)
    val renderRotate by bsetting("RenderRotate", true)
    private val swing by bsetting("Swing", true)
    private val packetSwing by bsetting("PacketSwing", true).isTrue { swing }
    private val swingHand by msetting("SwingHand", SwingHand.MainHand).isTrue { swing }
    val debug by bsetting("Debug", false)

    fun isBestAura(aura: AuraType): Boolean {
        if (smartAura) {
            kotlin.runCatching {
                val auraList = CopyOnWriteArrayList<Aura>()
                if (KillAura.isEnabled && calculateKA) auraList.add(
                    Aura(
                        AuraType.Sword,
                        KillAura.kadamage
                    )
                )
                if (AutoCrystal.isEnabled) auraList.add(Aura(AuraType.Crystal, AutoCrystal.cadamage))
                if (AnchorAura.isEnabled) auraList.add(Aura(AuraType.Anchor, AnchorAura.anchorDamage))
                return auraList.maxByOrNull { it.damage }!!.aura == aura
            }
        } else return true
        return false
    }

    fun SafeClientEvent.swing() {
        if (swing) {
            if (packetSwing) connection.sendPacket(
                HandSwingC2SPacket(
                    when (swingHand) {
                        SwingHand.OffHand -> Hand.OFF_HAND
                        else -> Hand.MAIN_HAND
                    }
                )
            ) else player.swingHand(
                if (player.offHandStack.item is EndCrystalItem) Hand.OFF_HAND else
                    when (swingHand) {
                        SwingHand.OffHand -> Hand.OFF_HAND
                        else -> Hand.MAIN_HAND
                    }
            )
            player.resetLastAttackedTicks()
        }
    }

    enum class SpoofMode {
        Normal, Swap, China
    }

    enum class MainToggle {
        Crystal, Anchor
    }

    enum class SwingHand {
        MainHand, OffHand
    }

    enum class AuraType {
        Crystal, Anchor, Sword
    }

    class Aura(val aura: AuraType, val damage: Double)
}