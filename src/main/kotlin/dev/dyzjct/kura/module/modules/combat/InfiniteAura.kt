package dev.dyzjct.kura.module.modules.combat

import dev.dyzjct.kura.manager.RotationManager
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.utils.inventory.HotbarSlot
import dev.dyzjct.kura.utils.math.LagCompensator
import dev.dyzjct.kura.utils.math.path.TeleportPath.teleportTo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import dev.dyzjct.kura.event.eventbus.SafeClientEvent
import base.utils.combat.getEntityTarget
import base.utils.concurrent.threads.runSafe
import base.utils.inventory.slot.firstItem
import base.utils.inventory.slot.hotbarSlots
import dev.dyzjct.kura.module.modules.client.CombatSystem.swing
import net.minecraft.entity.Entity
import net.minecraft.item.ShieldItem
import net.minecraft.item.SwordItem
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.util.Hand


object InfiniteAura : Module(name = "InfiniteAura", langName = "百米大刀", category = Category.COMBAT) {
    private var range by isetting("Range", 50, 5, 150)
    private var animals by bsetting("Animals", false)
    private var mobs by bsetting("Mobs", false)
    private var swapWeapon by bsetting("SwapWeapon", false)
    private var onlySword by bsetting("OnlySword", true)
    private var rotate by bsetting("Rotation", false)
    private var autoBlock by bsetting("AutoBlock", false)
    private var pauseInHit by bsetting("ABPauseInHit", false)
    private var path by bsetting("PathTP", true)

    private var target: Entity? = null
    private var stop = false

    init {
        onMotion {
            target = getEntityTarget(range.toDouble(), mob = mobs, ani = animals)
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
                if (rotate) RotationManager.rotationTo(target.blockPos.up().toCenterPos())
                if (autoBlock) {
                    if (player.offHandStack.item is ShieldItem) {
                        playerController.interactItem(player, Hand.OFF_HAND)
                    }
                }
                if (!delayCheck()) return@onMotion
                if (autoBlock && pauseInHit) {
                    if (player.offHandStack.item is ShieldItem) {
                        playerController.stopUsingItem(player)
                    }
                }
                val playerPos = player.pos
                CoroutineScope(Dispatchers.Default).launch {
                    runCatching {
                        if (path) teleportTo(target.pos) else connection.sendPacket(
                            PlayerMoveC2SPacket.Full(
                                target.x,
                                target.y,
                                target.z,
                                player.yaw,
                                player.pitch,
                                true
                            )
                        )
                        playerController.attackEntity(player, target)
                        swing()
                        if (path) teleportTo(playerPos) else connection.sendPacket(
                            PlayerMoveC2SPacket.Full(
                                playerPos.x,
                                playerPos.y,
                                playerPos.z,
                                player.yaw,
                                player.pitch,
                                true
                            )
                        )
                    }
                }
                if (autoBlock) stop = true
            }
            if (target == null && autoBlock && stop) playerController.stopUsingItem(player)
        }
    }

    override fun onDisable() {
        runSafe {
            if (target == null && autoBlock) playerController.stopUsingItem(player)
        }
    }

    private fun SafeClientEvent.delayCheck(): Boolean {
        var delay = 0.5f
        delay /= LagCompensator.tickRate / 20
        return player.getAttackCooldownProgress(delay) >= 1
    }
}
