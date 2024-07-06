package dev.dyzjct.kura.module.modules.combat

import base.utils.block.BlockUtil.getNeighbor
import base.utils.chat.ChatUtil
import base.utils.combat.getTarget
import base.utils.concurrent.threads.runSafe
import base.utils.entity.EntityUtils.boxCheck
import base.utils.entity.EntityUtils.isInWeb
import base.utils.entity.EntityUtils.spoofSneak
import base.utils.extension.fastPos
import base.utils.hole.SurroundUtils
import base.utils.hole.SurroundUtils.checkHole
import base.utils.math.distanceSqToCenter
import base.utils.math.sq
import base.utils.world.noCollision
import dev.dyzjct.kura.event.eventbus.SafeClientEvent
import dev.dyzjct.kura.manager.HotbarManager.spoofHotbarWithSetting
import dev.dyzjct.kura.manager.RotationManager
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.module.modules.client.CombatSystem
import dev.dyzjct.kura.module.modules.client.CombatSystem.swing
import dev.dyzjct.kura.module.modules.player.AntiMinePlace
import dev.dyzjct.kura.module.modules.player.PacketMine.hookPos
import dev.dyzjct.kura.utils.TimerUtils
import net.minecraft.block.Blocks
import net.minecraft.block.PistonBlock
import net.minecraft.block.RedstoneBlock
import net.minecraft.entity.ItemEntity
import net.minecraft.item.Items
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Direction

object HolePush : Module(
    name = "HolePush",
    langName = "活塞推人",
    description = "Push the target away from the hole.",
    category = Category.COMBAT
) {
    private val rotate = bsetting("Rotation", false)
    private val side by bsetting("Side", false).isTrue(rotate)
    private val checkDown by bsetting("CheckDown", false)
    private val delay by dsetting("Delay", 50.0, 0.0, 250.0)
    private val airPlace by bsetting("AirPlace", false)
    private val autoToggle = bsetting("AutoToggle", true)
    private val pushDelay by dsetting("PushDelay", 250.0, 0.0, 1000.0).isFalse(autoToggle)
    private val debug by bsetting("Debug", false)
    private val timer = TimerUtils()
    private val pushTimer = TimerUtils()
    private var stage = 0

    override fun onEnable() {
        runSafe {
            stage = 0
        }
    }

    override fun onDisable() {
        runSafe { stage = 0 }
    }

    init {
        onMotion {
            val target = getTarget(CombatSystem.targetRange)

            if ((!spoofHotbarWithSetting(Items.PISTON, true) {} && !spoofHotbarWithSetting(
                    Items.STICKY_PISTON,
                    true
                ) {}) || !spoofHotbarWithSetting(
                    Items.REDSTONE_BLOCK, true
                ) {} || target == null
            ) {
                if (autoToggle.value) {
                    toggle()
                }
                return@onMotion
            }
            if (autoToggle.value && stage >= 4) {
                toggle()
                return@onMotion
            }
            if (!autoToggle.value) {
                if ((world.isAir(target.blockPos) && checkHole(target) == SurroundUtils.HoleType.NONE) || player.usingItem)
                    return@onMotion
            }
            if (!world.isAir(target.blockPos.up(2))) return@onMotion
            val targetUp = target.blockPos.up()
            if (pushTimer.passedMs(pushDelay.toLong())) {
                if (!world.isAir(targetUp.up())) return@onMotion
                if (isInWeb(target)) return@onMotion
                doHolePush(targetUp, true)
                if (!world.isAir(target.blockPos)) doHolePush(targetUp, false)
            }
        }
    }

    fun SafeClientEvent.doHolePush(
        targetPos: BlockPos,
        check: Boolean,
        test: Boolean = false
    ): BlockPos? {
        fun checkPull(face: Direction): Boolean {
            val opposite = targetPos.offset(face.opposite)
            return when (face) {
                Direction.NORTH -> {
                    !world.isAir(opposite) || !world.isAir(opposite.up())
                }

                Direction.SOUTH -> {
                    !world.isAir(opposite) || !world.isAir(opposite.up())
                }

                Direction.EAST -> {
                    !world.isAir(opposite) || !world.isAir(opposite.up())
                }

                Direction.WEST -> {
                    !world.isAir(opposite) || !world.isAir(opposite.up())
                }

                else -> false
            }
        }
        for (face in Direction.entries) {
            if (face == Direction.DOWN || face == Direction.UP) continue
            if (checkPull(face) && check) continue
            if (checkPull(face) && (!world.isAir(targetPos.offset(face).up()) && world.getBlockState(
                    targetPos.offset(
                        face
                    ).up()
                ).block !is RedstoneBlock)
            ) continue
            if (!world.entities.none {
                    it !is ItemEntity;it.isAlive;it.boundingBox.intersects(
                    Box(
                        targetPos.offset(
                            face
                        )
                    )
                )
                }) continue
            if (!world.isAir(targetPos.offset(face)) && world.getBlockState(targetPos.offset(face)).block !is PistonBlock) continue
            getRedStonePos(targetPos.offset(face), face)?.let {
                if (!world.isAir(it.pos.down()) || !checkDown) {
                    if (!test) {
                        if ((spoofHotbarWithSetting(Items.PISTON, true) {} || spoofHotbarWithSetting(
                                Items.STICKY_PISTON,
                                true
                            ) {}) && spoofHotbarWithSetting(
                                Items.REDSTONE_BLOCK, true
                            ) {}
                        ) {
                            placeBlock(
                                targetPos.offset(face),
                                it.pos,
                                face,
                                !check
                            )
                        }
                    }
                    return it.pos
                } else if (!world.isAir(it.pos.down(2)) && checkDown && it.direction == Direction.DOWN) {
                    if (!test) {
                        if (timer.tickAndReset(delay)) {
                            if (rotate.value) RotationManager.addRotations(it.pos.down())
                            player.spoofSneak {
                                spoofHotbarWithSetting(Items.OBSIDIAN) {
                                    connection.sendPacket(fastPos(it.pos.down()))
                                }
                            }
                        }
                    }
                }
            } ?: continue
        }
        return null
    }

    private fun SafeClientEvent.placeBlock(
        blockPos: BlockPos,
        stonePos: BlockPos,
        face: Direction,
        mine: Boolean = false
    ) {
        if (!timer.passedMs(delay.toLong())) return
        fun spoofPlace(stone: Boolean, doToggle: Boolean = false) {
            if (!stone) {
                RotationManager.addRotations(blockPos = blockPos, side = true)
                RotationManager.stopRotation()
                face.let {
                    connection.sendPacket(
                        PlayerMoveC2SPacket.LookAndOnGround(
                            when (it) {
                                Direction.EAST -> -90f
                                Direction.NORTH -> 180f
                                Direction.SOUTH -> 0f
                                Direction.WEST -> 90f
                                else -> 0f
                            }, player.pitch, true
                        )
                    )
                }
            }
            RotationManager.startRotation()
            if (!stone) RotationManager.addRotations(blockPos)
            if (!stone || world.isAir(stonePos)) {
                player.spoofSneak {
                    spoofHotbarWithSetting(
                        if (!stone) (if (spoofHotbarWithSetting(
                                Items.PISTON,
                                true
                            ) {}
                        ) Items.PISTON else Items.STICKY_PISTON) else Items.REDSTONE_BLOCK
                    ) {
                        connection.sendPacket(fastPos(if (!stone) blockPos else stonePos))
                    }
                }
                swing()
            }
            if (!stone) RotationManager.addRotations(blockPos)
            stage++
            if (debug) ChatUtil.sendMessage(if (stone) "[HolePush] -> PlaceStone!" else "[HolePush] -> PlacePiston!")
            if (!world.isAir(stonePos) && doToggle) {
                if (debug) ChatUtil.sendMessage("[HolePush] -> Doing Toggle!")
                if (mine) hookPos(stonePos)
                if (autoToggle.value) {
                    toggle()
                } else {
                    pushTimer.reset()
                    stage = 0
                }
            }
            timer.reset()
            return
        }
        if (getNeighbor(blockPos) != null || airPlace) {
            if (world.isAir(blockPos)) {
                if (rotate.value) {
                    RotationManager.addRotations(blockPos, side = side)
                }
                spoofPlace(stone = false, doToggle = true)
            } else {
                if (rotate.value && world.isAir(stonePos)) {
                    RotationManager.addRotations(stonePos, side = side)
                }
                spoofPlace(stone = true, doToggle = true)
            }
        } else {
            if (rotate.value && world.isAir(stonePos)) {
                RotationManager.addRotations(stonePos, side = side)
            }
            spoofPlace(stone = true, doToggle = false)
        }
    }

    private fun SafeClientEvent.getRedStonePos(pos: BlockPos, direction: Direction): StonePos? {
        val face = when (direction) {
            Direction.EAST -> Direction.WEST
            Direction.WEST -> Direction.EAST
            Direction.NORTH -> Direction.SOUTH
            Direction.SOUTH -> Direction.NORTH
            else -> direction
        }
        for (facing: Direction in Direction.entries) {
            if (world.getBlockState(pos.offset(facing)).block == Blocks.REDSTONE_BLOCK) {
                return StonePos(pos.offset(facing), facing)
            }
            if (facing != face) {
                if (player.distanceSqToCenter(pos.offset(facing)) > CombatSystem.placeRange.sq) continue
                if (!boxCheck(Box(pos.offset(facing)))) continue
                if (!world.noCollision(pos.offset(facing))) continue
                if (!world.isAir(pos.offset(facing))) continue
                if (getNeighbor(pos.offset(facing)) == null) continue
                val minePos = AntiMinePlace.mineMap[pos.offset(facing)]
                if (AntiMinePlace.isEnabled && minePos != null) {
                    if (System.currentTimeMillis() - minePos.mine >= minePos.start
                    ) continue
                }
                return StonePos(pos.offset(facing), facing)
            }
        }
        return null
    }

    class StonePos(var pos: BlockPos, var direction: Direction)
}