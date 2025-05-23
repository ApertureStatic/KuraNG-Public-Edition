package dev.dyzjct.kura.module.modules.combat

import base.utils.concurrent.threads.runSafe
import base.utils.entity.EntityUtils.spoofSneak
import base.utils.inventory.slot.firstItem
import base.utils.inventory.slot.hotbarSlots
import base.utils.world.isPlaceable
import dev.dyzjct.kura.manager.EntityManager
import dev.dyzjct.kura.manager.HotbarManager.spoofHotbar
import dev.dyzjct.kura.manager.RotationManager.packetRotate
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.utils.TimerUtils
import dev.dyzjct.kura.utils.extension.fastPos
import net.minecraft.entity.decoration.EndCrystalEntity
import net.minecraft.item.Items
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket
import net.minecraft.util.Hand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box

object SelfTrap : Module(name = "SelfTrap", "自动裹自己", category = Category.COMBAT) {

    private var placeDelay = isetting("PlaceDelay", 10, 0, 1000)
    private var rotate = bsetting("Rotate", false)
    private var placeTimer = TimerUtils()
    private var attackTimer = TimerUtils()

    override fun onEnable() {
        runSafe {
            if (player.hotbarSlots.firstItem(Items.OBSIDIAN) == null) {
                disable()
                return
            }
            placeTimer.reset()
        }
    }

    init {
        onMotion {
            player.hotbarSlots.firstItem(Items.OBSIDIAN)?.let { obs ->
                if (placeTimer.tickAndReset(placeDelay.value)) {
                    for (offset in offsetsDefault) {
                        val pos = player.blockPos.add(offset)
                        if (!world.isPlaceable(pos)) continue
                        if (rotate.value) packetRotate(pos)
                        var breakCrystal = false
                        if (attackTimer.passedMs(50L)) {
                            for (entity in EntityManager.entity) {
                                if (breakCrystal && entity is EndCrystalEntity) continue
                                if (!entity.isAlive) continue
                                if (!entity.boundingBox.intersects(Box(pos))) continue
                                if (entity !is EndCrystalEntity) continue
                                connection.sendPacket(PlayerInteractEntityC2SPacket.attack(entity, false))
                                connection.sendPacket(HandSwingC2SPacket(Hand.MAIN_HAND))
                                breakCrystal = true
                                attackTimer.reset()
                            }
                        }
                        player.spoofSneak {
                            spoofHotbar(obs) {
                                connection.sendPacket(fastPos(pos))
                            }
                        }
                    }
                }
            }
        }
    }

    private var offsetsDefault = arrayOf(
        BlockPos(0, 0, -1),
        BlockPos(1, 0, 0),
        BlockPos(0, 0, 1),
        BlockPos(-1, 0, 0),
        BlockPos(0, 1, -1),
        BlockPos(1, 1, 0),
        BlockPos(0, 1, 1),
        BlockPos(-1, 1, 0),
        BlockPos(0, 2, -1),
        BlockPos(1, 2, 0),
        BlockPos(0, 2, 1),
        BlockPos(-1, 2, 0),
        BlockPos(0, 2, 0),
    )
}