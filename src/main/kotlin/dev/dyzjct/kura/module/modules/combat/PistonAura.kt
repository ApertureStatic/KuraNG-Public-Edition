package dev.dyzjct.kura.module.modules.combat

import dev.dyzjct.kura.manager.HotbarManager.spoofHotbar
import dev.dyzjct.kura.manager.HotbarManager.spoofHotbarBypass
import dev.dyzjct.kura.manager.RotationManager
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.module.modules.player.PacketMine.hookPos
import dev.dyzjct.kura.module.modules.render.PlaceRender
import dev.dyzjct.kura.utils.TimerUtils
import dev.dyzjct.kura.utils.animations.sq
import dev.dyzjct.kura.utils.inventory.HotbarSlot
import base.system.event.SafeClientEvent
import base.utils.block.BlockUtil.getNeighbor
import base.utils.chat.ChatUtil
import base.utils.extension.fastPos
import base.utils.inventory.slot.firstItem
import base.utils.inventory.slot.hotbarSlots
import base.utils.world.noCollision
import net.minecraft.block.Blocks
import net.minecraft.block.PistonBlock
import net.minecraft.entity.ItemEntity
import net.minecraft.entity.decoration.EndCrystalEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Items
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.util.Hand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Direction
import base.utils.math.distanceSqTo
import base.utils.math.distanceSqToCenter

object PistonAura : Module(
    name = "PistonAura",
    langName = "活塞水晶",
    category = Category.COMBAT,
    description = "Push Crystal to killing target."
) {
    private val targetRange by isetting("TargetRange", 8, 0, 8)
    private val range by isetting("Range", 6, 0, 8)
    private val delay by isetting("Delay", 50, 0, 300)
    private val rotate by bsetting("rotate", false)
    private val spoofBypass by bsetting("SpoofBypass", false)
    private val strictDirection by bsetting("StrictDirection", false)
    private val swing = bsetting("Swing", true)
    private val packet by bsetting("packet", true).isTrue(swing)
    private val debug by bsetting("Debug", false)
    private val timer = TimerUtils()
    private val cTimer = TimerUtils()

    init {
        onMotion {
            val pistonSlot =
                player.hotbarSlots.firstItem(Items.PISTON) ?: player.hotbarSlots.firstItem(Items.STICKY_PISTON)
            val stoneSlot = player.hotbarSlots.firstItem(
                Items.REDSTONE_BLOCK
            )
            val crystalSlot = player.hotbarSlots.firstItem(Items.END_CRYSTAL)
            if (pistonSlot == null || stoneSlot == null || crystalSlot == null) {
                return@onMotion
            }
            for (target in world.entities.filter { it is PlayerEntity && player.distanceSqTo(it) <= targetRange.sq && it != player }) {
                upLoop@ for (i in 1..3) {
                    for (facing in Direction.values()) {
                        var stop = false
                        if (facing == Direction.DOWN || facing == Direction.UP) continue
                        if (getCrystalPos(fixCrystalPos(target.blockPos.up(), facing)) == null) continue
                        getPistonPos(target.blockPos.up(i), facing)?.let { pistonPos ->
                            getStonePos(pistonPos, facing)?.let { stonePos ->
                                getCrystalPos(fixCrystalPos(target.blockPos.up(), facing))?.let { crystalPos ->
                                    if (crystalPos.y <= pistonPos.y) {
                                        if (!world.isAir(pistonPos)) {
                                            PlaceRender.renderBlocks[crystalPos] = System.currentTimeMillis()
                                            if (world.noCollision(crystalPos) && world.entities.none {
                                                    it is EndCrystalEntity && it.boundingBox.intersects(
                                                        Box(crystalPos)
                                                    )
                                                }) {
                                                placeBlock(stonePos, stoneSlot)
                                                if (world.getBlockState(stonePos).block == Blocks.REDSTONE_BLOCK) {
                                                    if (debug) ChatUtil.sendMessage("[PistonAura] -> PlaceCrystal!")
                                                    if (rotate) RotationManager.addRotations(crystalPos)
                                                    if (spoofBypass) {
                                                        spoofHotbarBypass(crystalSlot) {
                                                            connection.sendPacket(fastPos(crystalPos))
                                                        }
                                                    } else spoofHotbar(crystalSlot) {
                                                        connection.sendPacket(fastPos(crystalPos))
                                                    }
                                                    swingHand()
                                                }
                                            } else if (!world.isAir(stonePos)) {
                                                if (timer.tickAndReset(delay)) {
                                                    if (rotate) RotationManager.addRotations(stonePos)
                                                    hookPos(stonePos)
                                                }
                                            }
                                        } else if (getNeighbor(pistonPos, false) != null) {
                                            placeBlock(pistonPos, pistonSlot, facing)
                                        } else placeBlock(stonePos, stoneSlot)

                                        if (cTimer.tickAndReset(delay) && world.isAir(stonePos)) {
                                            for (ent in world.entities) {
                                                if (ent !is EndCrystalEntity) continue
                                                if (rotate) RotationManager.addRotations(ent.blockPos)
                                                connection.sendPacket(
                                                    PlayerInteractEntityC2SPacket.attack(
                                                        ent,
                                                        player.isSneaking
                                                    )
                                                )
                                                swingHand()
                                            }
                                        }
                                        stop = true
                                    }
                                }
                            }
                        }
                        if (stop) break@upLoop
                    }
                }

            }
        }
    }

    private fun SafeClientEvent.isSafe(pos: BlockPos, piston: Boolean = false): Boolean {
        if (piston && world.getBlockState(pos).block is PistonBlock) return true
        return player.distanceSqToCenter(pos) <= range.sq && world.isAir(pos) && world.noCollision(pos) && world.entities.none {
            it !is ItemEntity && it.boundingBox.intersects(
                Box(pos)
            )
        }
    }

    private fun SafeClientEvent.getPistonPos(blockPos: BlockPos, direction: Direction): BlockPos? {
        if (getStonePos(blockPos, direction) == null) return null
        fun pistonSafe(blockPos: BlockPos): Boolean {
            return isSafe(blockPos, true) && isSafe(blockPos.offset(direction), true)
        }
        when (direction) {
            Direction.NORTH -> {
                if (pistonSafe(blockPos.offset(direction).west())) return blockPos.offset(direction)
                    .west() else if (pistonSafe(
                        blockPos.offset(direction).east()
                    )
                ) blockPos.offset(direction).east()
            }

            Direction.SOUTH -> {
                if (pistonSafe(blockPos.offset(direction).west())) return blockPos.offset(direction)
                    .west() else if (pistonSafe(
                        blockPos.offset(direction).east()
                    )
                ) blockPos.offset(direction).east()
            }

            Direction.WEST -> {
                if (pistonSafe(blockPos.offset(direction).north())) return blockPos.offset(direction)
                    .north() else if (pistonSafe(
                        blockPos.offset(direction).south()
                    )
                ) blockPos.offset(direction).south()
            }

            Direction.EAST -> {
                if (pistonSafe(blockPos.offset(direction).north())) return blockPos.offset(direction)
                    .north() else if (pistonSafe(
                        blockPos.offset(direction).south()
                    )
                ) blockPos.offset(direction).south()
            }

            else -> return null
        }
        return null
    }

    private fun SafeClientEvent.getStonePos(pos: BlockPos, direction: Direction): BlockPos? {
        val face = when (direction) {
            Direction.EAST -> Direction.WEST
            Direction.WEST -> Direction.EAST
            Direction.NORTH -> Direction.SOUTH
            Direction.SOUTH -> Direction.NORTH
            else -> direction
        }
        for (facing: Direction in Direction.values()) {
            if (world.getBlockState(pos.offset(facing)).block == Blocks.REDSTONE_BLOCK) {
                return pos.offset(facing)
            }
            if (facing == face || player.distanceSqToCenter(pos.offset(facing)) > range.sq) continue
            if (!world.entities.none {
                    it !is ItemEntity;it.isAlive;it.boundingBox.intersects(
                    Box(
                        pos.offset(facing)
                    )
                )
                } || !world.noCollision(pos.offset(facing)) || !world.isAir(pos.offset(facing))) continue
            return pos.offset(facing)
        }
        return null
    }

    private fun SafeClientEvent.getCrystalPos(pos: BlockPos): BlockPos? {
        return if (world.isAir(pos)) pos
        else if (world.isAir(pos.up())) pos.up()
        else null
    }

    private fun SafeClientEvent.placeBlock(blockPos: BlockPos, slot: HotbarSlot, face: Direction? = null) {
        if (isSafe(blockPos)) {
            if (timer.tickAndReset(delay)) {
                if (rotate) RotationManager.addRotations(blockPos)
                if (debug) ChatUtil.sendMessage("[PistonAura] -> PlaceBlock!")
                face?.let {
                    PlayerMoveC2SPacket.LookAndOnGround(
                        when (it) {
                            Direction.EAST -> -90f
                            Direction.NORTH -> 180f
                            Direction.SOUTH -> 0f
                            Direction.WEST -> 90f
                            else -> 0f
                        }, 0f, true
                    )
                }
                if (spoofBypass) spoofHotbarBypass(slot) { connection.sendPacket(fastPos(blockPos, strictDirection)) }
                else spoofHotbar(slot) { connection.sendPacket(fastPos(blockPos, strictDirection)) }
                swingHand()
            }
        }
    }

    private fun fixCrystalPos(pos: BlockPos, direction: Direction): BlockPos {
        return when (direction) {
            Direction.NORTH -> pos.offset(Direction.NORTH)
            Direction.SOUTH -> pos.offset(Direction.SOUTH)
            Direction.WEST -> pos.offset(Direction.WEST)
            Direction.EAST -> pos.offset(Direction.EAST)
            else -> pos
        }
    }

    private fun SafeClientEvent.swingHand() {
        if (!swing.value) return
        if (packet) {
            connection.sendPacket(HandSwingC2SPacket(Hand.MAIN_HAND))
        } else player.swingHand(Hand.MAIN_HAND)
    }
}