package dev.dyzjct.kura.module.modules.combat

import dev.dyzjct.kura.manager.RotationManager
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.module.modules.client.UiSetting
import dev.dyzjct.kura.module.modules.crystal.MelonAura2
import dev.dyzjct.kura.utils.animations.sq
import dev.dyzjct.kura.utils.inventory.HotbarSlot
import dev.dyzjct.kura.utils.math.LagCompensator
import melon.system.event.SafeClientEvent
import melon.utils.combat.getEntityTarget
import melon.utils.concurrent.threads.runSafe
import melon.utils.graphics.ESPRenderer
import melon.utils.inventory.slot.firstItem
import melon.utils.inventory.slot.hotbarSlots
import net.minecraft.entity.Entity
import net.minecraft.item.ShieldItem
import net.minecraft.item.SwordItem
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket
import net.minecraft.util.Hand
import team.exception.melon.util.math.distanceSqTo
import kotlin.math.max

object KillAura : Module(name = "KillAura", langName = "杀戮", category = Category.COMBAT) {
    private var range by dsetting("Range", 4.5, 0.1, 6.0)
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

    init {
        onMotion {
            if (pauseIfCA) {
                if (MelonAura2.isEnabled && MelonAura2.placeInfo != null) return@onMotion
            }
            target = getEntityTarget(if (autoBlock.value) max(range, abRange) else range, mob = mobs, ani = animals)
            target?.let { target ->
                val weaponSlot = player.hotbarSlots.firstItem<SwordItem, HotbarSlot>()
                if (onlySword && player.mainHandStack.item !is SwordItem) return@onMotion
                if (swapWeapon) {
                    weaponSlot?.let { swordSlot ->
                        if (player.inventory.selectedSlot != swordSlot.hotbarSlot) {
                            player.inventory.selectedSlot = swordSlot.hotbarSlot
                        }
                    }
                }
                RotationManager.addRotations(target.blockPos.up().toCenterPos())
                if (autoBlock.value && player.distanceSqTo(target.pos) <= abRange.sq) {
                    if (player.offHandStack.item is ShieldItem) {
                        if (player.offHandStack.item is ShieldItem) {
                            playerController.interactItem(player, Hand.OFF_HAND)
                        }
                    }
                }
                if (!delayCheck() || player.distanceSqTo(target.pos) > range.sq) return@onMotion
                if (autoBlock.value && pauseInHit) {
                    if (player.offHandStack.item is ShieldItem) {
                        playerController.stopUsingItem(player)
                    }
                }
                connection.sendPacket(PlayerInteractEntityC2SPacket.attack(target, player.isSneaking))
                player.swingHand(Hand.MAIN_HAND)
                player.resetLastAttackedTicks()
                stop = true
            }
            if (target == null && autoBlock.value && stop) playerController.stopUsingItem(player)
        }

        onRender3D { event ->
            if (render) {
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