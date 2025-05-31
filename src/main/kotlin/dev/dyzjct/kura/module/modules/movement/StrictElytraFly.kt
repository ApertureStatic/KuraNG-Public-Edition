package dev.dyzjct.kura.module.modules.movement

import base.utils.chat.ChatUtil
import dev.dyzjct.kura.event.eventbus.SafeClientEvent
import dev.dyzjct.kura.event.eventbus.safeEventListener
import dev.dyzjct.kura.event.events.TickEvent
import dev.dyzjct.kura.manager.HotbarManager.spoofHotbarWithSetting
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.utils.TimerUtils
import dev.dyzjct.kura.utils.extension.sendSequencedPacket
import net.minecraft.entity.EquipmentSlot
import net.minecraft.item.ElytraItem
import net.minecraft.item.Items
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket
import net.minecraft.util.Hand


object StrictElytraFly : Module(
    name = "StrictElytraFly",
    category = Category.PLAYER
) {

    private val toggle by bsetting("Toggle", false)
    private val delay by isetting("FireWorkDelay", 3, 1, 3).isFalse { toggle }
    private val ground by bsetting("GroundMode", false)
    private val sprint by bsetting("Sprint", false).isTrue { ground }

    private val timer = TimerUtils()

    init {
        onMotion {
            if (!ground) {
                if (!timer.passedMs(delay * 1000L) && !toggle) return@onMotion
                if (!spoofHotbarWithSetting(Items.FIREWORK_ROCKET, true) {}) {
                    ChatUtil.sendNoSpamMessage("${ChatUtil.RED}[${ChatUtil.YELLOW}ElytraFLY WARNING! ${ChatUtil.RED}] ${ChatUtil.BLUE}NO FIREWORK_ROCKET IN INV!")
                    return@onMotion
                }
                if (player.isOnGround) player.jump()
                if (!player.isFallFlying && !player.isOnGround) {
                    connection.sendPacket(ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.START_FALL_FLYING))
                }
                if (player.isFallFlying) {
                    spoofHotbarWithSetting(Items.FIREWORK_ROCKET, false) {
                        sendSequencedPacket(
                            world
                        ) {
                            PlayerInteractItemC2SPacket(
                                Hand.MAIN_HAND, it
                            )
                        }
                    }
                    timer.reset()
                    if (toggle) {
                        toggle()
                        return@onMotion
                    }
                }
            }
        }

        safeEventListener<TickEvent.Post> {
            if (ground) {
                mc.options.jumpKey.isPressed = true
                if (!player.isFallFlying) mc.networkHandler!!
                    .sendPacket(ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.START_FALL_FLYING))

                if (checkConditions()) {
                    if (!sprint) {
                        if (player.isFallFlying) player.isSprinting = player.isOnGround
                        else player.isSprinting = true
                    }
                }
                mc.options.jumpKey.isPressed = false
            }
        }
        safeEventListener<TickEvent.Pre> {
            if (checkConditions() && sprint && ground) player.isSprinting = true
        }
    }

    private fun SafeClientEvent.checkConditions(): Boolean {
        val itemStack = player.getEquippedStack(EquipmentSlot.CHEST)
        return (!player.abilities.flying && !player.hasVehicle() && !player.isClimbing && itemStack.isOf(Items.ELYTRA) && ElytraItem.isUsable(
            itemStack
        ))
    }

}