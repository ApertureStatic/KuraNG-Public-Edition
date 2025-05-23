package dev.dyzjct.kura.module.modules.combat

import base.utils.chat.ChatUtil
import base.utils.math.distanceSqTo
import base.utils.math.sq
import dev.dyzjct.kura.event.eventbus.SafeClientEvent
import dev.dyzjct.kura.manager.HotbarManager.spoofHotbarWithSetting
import dev.dyzjct.kura.manager.RotationManager.packetRotate
import dev.dyzjct.kura.manager.SphereCalculatorManager
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.module.modules.client.CombatSystem
import dev.dyzjct.kura.module.modules.client.CombatSystem.swing
import dev.dyzjct.kura.module.modules.crystal.CrystalDamageCalculator.calcDamage
import dev.dyzjct.kura.utils.TimerUtils
import dev.dyzjct.kura.utils.block.BlockUtil.canSee
import dev.dyzjct.kura.utils.block.BlockUtil.getNeighbor
import dev.dyzjct.kura.utils.block.getBlock
import dev.dyzjct.kura.utils.extension.fastPos
import net.minecraft.block.Blocks
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.decoration.EndCrystalEntity
import net.minecraft.entity.projectile.thrown.EnderPearlEntity
import net.minecraft.item.Items
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box

object PearlFucker : Module(
    name = "PearlFucker",
    langName = "防止珍珠侠",
    category = Category.COMBAT
) {
    private val mode by msetting("Mode", Mode.Crystal)
    private val rotate by bsetting("Rotation", false)
    private val ignore by bsetting("IgnoreSelf", true)
    private val ignoreTime by isetting("IgnoreTime", 500, 500, 1500)
    private val delay by isetting("Delay", 25, 0, 500)
    private val debug by bsetting("Debug", false)

    private val timer = TimerUtils()
    val ignoreTimer = TimerUtils()


    init {
        onPacketSend {
            if (ignore) if (player.mainHandStack.item == Items.ENDER_PEARL && it.packet is PlayerInteractItemC2SPacket) {
                ignoreTimer.reset()
            }
        }
        onMotion {
            if (!ignoreTimer.passedMs(ignoreTime.toLong())) return@onMotion
            for (pearl in world.entities.filterIsInstance<EnderPearlEntity>()) {
                if (player.distanceSqTo(pearl) > CombatSystem.targetRange.sq) continue
                if (mode == Mode.WEB) {
                    if (timer.tickAndReset(delay)) {
                        if (rotate) packetRotate(pearl.blockPos)
                        spoofHotbarWithSetting(Items.COBWEB) {
                            if (getNeighbor(pearl.blockPos) != null) {
                                if (debug) ChatUtil.sendMessage("WebPlacing")
                                connection.sendPacket(fastPos(pearl.blockPos))
                                swing()
                            }
                        }
                    }
                } else {
                    if (findTargetCrystal(pearl) != null) {
                        if (timer.tickAndReset(delay)) {
                            if (debug) ChatUtil.sendMessage("CrystalAttacking")
                            if (rotate) packetRotate(findTargetCrystal(pearl)!!.blockPos)
                            connection.sendPacket(
                                PlayerInteractEntityC2SPacket.attack(
                                    findTargetCrystal(pearl),
                                    player.isSneaking
                                )
                            )
                            swing()
                        }
                    } else {
                        findPlacePos(pearl)?.let { pos ->
                            if (timer.tickAndReset(delay)) {
                                if (debug) ChatUtil.sendMessage("CrystalPlacing")
                                if (rotate) packetRotate(pos)
                                spoofHotbarWithSetting(Items.END_CRYSTAL) {
                                    connection.sendPacket(fastPos(pos))
                                    swing()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun SafeClientEvent.findPlacePos(entity: Entity): BlockPos? {
        return SphereCalculatorManager.sphereList.stream().filter {
            world.isInBuildLimit(it) && world.worldBorder.contains(it) && canPlace(it)
        }.filter {
            player.distanceSqTo(it) <= CombatSystem.placeRange.sq && (!CombatSystem.oldVersion || player.distanceSqTo(
                it
            ) <= CombatSystem.wallRange.sq || canSee(
                it.x.toDouble(),
                it.y.toDouble(),
                it.z.toDouble()
            ))
        }.toList().minByOrNull {
            calcDamage(
                entity as LivingEntity,
                entity.pos,
                entity.boundingBox,
                it.x.toDouble(),
                it.y.toDouble(),
                it.z.toDouble(),
                BlockPos.Mutable(),
                true
            ) - calcDamage(
                player,
                player.pos,
                player.boundingBox,
                it.x.toDouble(),
                it.y.toDouble(),
                it.z.toDouble(),
                BlockPos.Mutable(),
                true
            )
        }
    }

    private fun SafeClientEvent.findTargetCrystal(entity: Entity): EndCrystalEntity? {
        return world.entities.filterIsInstance<EndCrystalEntity>().filter {
            player.distanceSqTo(it) <= CombatSystem.attackRange.sq && (!CombatSystem.oldVersion || player.distanceSqTo(
                it
            ) <= CombatSystem.wallRange.sq || canSee(
                it.x,
                it.y,
                it.z
            ))
        }.toList().minByOrNull {
            calcDamage(
                entity as LivingEntity,
                entity.pos,
                entity.boundingBox,
                it.x,
                it.y,
                it.z,
                BlockPos.Mutable(),
                true
            ) - calcDamage(
                player,
                player.pos,
                player.boundingBox,
                it.x,
                it.y,
                it.z,
                BlockPos.Mutable(),
                true
            )
        }
    }

    private fun SafeClientEvent.canPlace(pos: BlockPos): Boolean {
        if (world.getBlock(pos.down()) != Blocks.OBSIDIAN || world.getBlock(pos.down()) != Blocks.BEDROCK) return false
        if (!world.isAir(pos) || (CombatSystem.oldVersion && !world.isAir(pos.up()))) return false
        if (world.entities.any { it !is EndCrystalEntity && it.boundingBox.intersects(Box(pos)) }) return false
        return true
    }

    enum class Mode {
        Crystal, WEB
    }
}