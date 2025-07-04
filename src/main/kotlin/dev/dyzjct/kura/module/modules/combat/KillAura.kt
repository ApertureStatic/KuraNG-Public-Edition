package dev.dyzjct.kura.module.modules.combat

import base.utils.combat.getEntityTarget
import base.utils.concurrent.threads.runSafe
import base.utils.graphics.ESPRenderer
import base.utils.inventory.slot.firstItem
import base.utils.inventory.slot.hotbarSlots
import base.utils.item.attackDamage
import base.utils.math.distanceSqTo
import dev.dyzjct.kura.event.eventbus.SafeClientEvent
import dev.dyzjct.kura.manager.RotationManager.packetRotate
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.module.modules.client.CombatSystem
import dev.dyzjct.kura.module.modules.client.CombatSystem.swing
import dev.dyzjct.kura.module.modules.client.UiSetting
import dev.dyzjct.kura.module.modules.crystal.AutoCrystal
import dev.dyzjct.kura.utils.animations.sq
import dev.dyzjct.kura.utils.inventory.HotbarSlot
import dev.dyzjct.kura.utils.math.LagCompensator
import net.minecraft.entity.Entity
import net.minecraft.item.ShieldItem
import net.minecraft.item.SwordItem
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket
import net.minecraft.util.Hand
import kotlin.math.max

object KillAura : Module(name = "KillAura", langName = "杀戮", category = Category.COMBAT) {
    private var animals by bsetting("Animals", false)
    private var mobs by bsetting("Mobs", false)
    private var swapWeapon by bsetting("SwapWeapon", false)
    private var onlySword by bsetting("OnlySword", true)
    private var pauseIfCA by bsetting("PauseIfCA", true)
    private var autoBlock = bsetting("AutoBlock", false)
    private var abRange by dsetting("ABRange", 3.5, 1.0, 10.0)
    private var pauseInHit by bsetting("ABPauseInHit", false).isTrue(autoBlock)
    private var render by bsetting("Render", false)

    private var target: Entity? = null
    private var stop = false

    var kadamage = 0.0

    init {
        onMotion {
            if (pauseIfCA) {
                if (AutoCrystal.isEnabled && AutoCrystal.placeInfo != null || AnchorAura.isEnabled && AnchorAura.placeInfo != null) return@onMotion
            }
            target = getEntityTarget(
                if (autoBlock.value) max(CombatSystem.kaRange, abRange) else CombatSystem.kaRange,
                mob = mobs,
                ani = animals
            )
            target?.let { target ->
                val weaponSlot = player.hotbarSlots.firstItem<SwordItem, HotbarSlot>()
                weaponSlot?.let {
                    kadamage = player.inventory.getStack(weaponSlot.hotbarSlot).attackDamage.toDouble()
                }
                if (!CombatSystem.isBestAura(CombatSystem.AuraType.Anchor) && CombatSystem.calculateKA) return@onMotion
                if (onlySword && player.mainHandStack.item !is SwordItem) return@onMotion
                if (swapWeapon) {
                    weaponSlot?.let { swordSlot ->
                        if (player.inventory.selectedSlot != swordSlot.hotbarSlot) {
                            player.inventory.selectedSlot = swordSlot.hotbarSlot
                        }
                    }
                }
                packetRotate(target.blockPos.up().toCenterPos())
                if (autoBlock.value && player.distanceSqTo(target.pos) <= abRange.sq) {
                    if (player.offHandStack.item is ShieldItem) {
                        if (player.offHandStack.item is ShieldItem) {
                            playerController.interactItem(player, Hand.OFF_HAND)
                        }
                    }
                }
                if (!delayCheck() || player.distanceSqTo(target.pos) > CombatSystem.kaRange.sq) return@onMotion
                if (autoBlock.value && pauseInHit) {
                    if (player.offHandStack.item is ShieldItem) {
                        playerController.stopUsingItem(player)
                    }
                }
                connection.sendPacket(PlayerInteractEntityC2SPacket.attack(target, player.isSneaking))
                swing()
                player.resetLastAttackedTicks()
                stop = true
            }
            if (target == null) {
                if (autoBlock.value && stop) {
                    playerController.stopUsingItem(player)
                }
                kadamage = 0.0
            }
        }

        onRender3D { event ->
            if (render) {
                if (player.mainHandStack.item !is SwordItem) return@onRender3D
                target?.let {
                    val renderer = ESPRenderer()
                    renderer.aFilled = 120
                    renderer.aOutline = 0
                    renderer.add(
                        it.boundingBox, UiSetting.getThemeSetting().primary
                    )
                    renderer.render(event.matrices, false)
                }
            }
        }
    }

    override fun onDisable() {
        runSafe {
            if (target == null && autoBlock.value) playerController.stopUsingItem(player)
        }
    }

    private fun SafeClientEvent.delayCheck(): Boolean {
        var delay = 0.5f
        delay /= LagCompensator.tickRate / 20
        return player.getAttackCooldownProgress(delay) >= 1
    }

    override fun getHudInfo(): String? {
        return target?.name?.string
    }
}