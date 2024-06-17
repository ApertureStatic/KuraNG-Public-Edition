package dev.dyzjct.kura.module.modules.player

import base.utils.block.BlockUtil
import base.utils.block.BlockUtil.checkNearBlocksExtended
import base.utils.inventory.slot.firstItem
import base.utils.inventory.slot.hotbarSlots
import base.utils.math.toVec3dCenter
import base.utils.player.RotationUtils
import dev.dyzjct.kura.event.eventbus.SafeClientEvent
import dev.dyzjct.kura.event.eventbus.StageType
import dev.dyzjct.kura.event.eventbus.safeEventListener
import dev.dyzjct.kura.event.events.player.PlayerMoveEvent
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.module.modules.render.PlaceRender
import dev.dyzjct.kura.system.render.graphic.Render2DEngine
import dev.dyzjct.kura.system.render.graphic.Render3DEngine
import dev.dyzjct.kura.utils.inventory.HotbarSlot
import dev.dyzjct.kura.utils.math.RotationUtils.getRotationTo
import net.minecraft.item.BlockItem
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import java.awt.Color

object Scaffold : Module(name = "Scaffold", langName = "自动搭路", category = Category.PLAYER) {
    private var color by csetting("Color", Color(-0x77ff0100))
    private var rotate = bsetting("Rotate", true)
    private var strictRotate by bsetting("StrictRotate", false).isTrue(rotate)
    private var rotateSide by bsetting("RotateSide", false).isTrue(rotate)
    private var allowShift by bsetting("AllowShift", false)
    private var tower by bsetting("Tower", true)
    private var safewalk by bsetting("SafeWalk", true)
    private var render by bsetting("Render", true)
    private var ignorer by bsetting("IgnorerRender", true)
    private var testjump by bsetting("TestJump", true)
    private var currentblock: BlockUtil.BlockPosWithFacing? = null
    private var spoofSlot: HotbarSlot? = null
    private var jumped = false
    private var towerTick = 0
    private var lastPos: BlockPos? = null

    override fun onEnable() {
        spoofSlot = null
        towerTick = 0
    }

    init {
        onMotion { event ->
            lastPos?.let {
                if (rotate.value) {
                    event.setRotation(
                        getRotationTo(it.toVec3dCenter(), side = rotateSide).x,
                        getRotationTo(it.toVec3dCenter(), side = rotateSide).y
                    )
                }
            }
            when (event.stageType) {
                StageType.START -> {
                    jumped = false
                    spoofSlot = getBlockSlot() ?: return@onMotion
                    if (strictRotate) player.isSprinting = false
                    if (countValidBlocks() <= 0) {
                        currentblock = null
                        return@onMotion
                    }
                    currentblock = null
                    if (player.isSneaking && !allowShift) return@onMotion

                    val playerPos = player.blockPos.add(0, -1, 0)
                    if (!world.getBlockState(playerPos).isReplaceable) return@onMotion
                    currentblock = checkNearBlocksExtended(playerPos)?.apply {
                        lastPos = position
                    } ?: return@onMotion
                    if (world.getBlockCollisions(
                            player,
                            player.boundingBox.expand(-0.2, 0.0, -0.2).offset(0.0, -0.5, 0.0)
                        ).iterator().hasNext()
                    ) return@onMotion
                    if (player.input.jumping && tower) {
//                        player.setVelocity(0.0, 0.399999986886978 + getJumpEffect() * 0.1, 0.0)
//                        connection.sendPacket(
//                            PlayerMoveC2SPacket.PositionAndOnGround(
//                                player.x,
//                                player.y + (0.399999986886978 + getJumpEffect() * 0.1),
//                                player.z,
//                                player.onGround
//                            )
//                        )
                        player.velocityDirty = true
                        if (++towerTick < 10) {
                            player.jump()
                        } else {
                            towerTick = 0
                        }
                        jumped = true
                    }
                }

                StageType.END -> {
                    currentblock?.let { currentblock ->
                        spoofSlot?.let { slot ->
                            lastPos?.let {
                                if (rotate.value) {
                                    event.setRotation(
                                        RotationUtils.getLegitRotations(it.toVec3dCenter())[0],
                                        RotationUtils.getLegitRotations(it.toVec3dCenter())[1]
                                    )
                                }
                            }
                            val oldSlot = player.inventory.selectedSlot
                            player.inventory.selectedSlot = slot.hotbarSlot
                            if (playerController.interactBlock(
                                    player, Hand.MAIN_HAND, BlockHitResult(
                                        currentblock.position.toVec3dCenter(),
                                        currentblock.facing,
                                        currentblock.position,
                                        false
                                    )
                                ) == ActionResult.SUCCESS
                            ) {
                                connection.sendPacket(HandSwingC2SPacket(Hand.MAIN_HAND))
                                if (jumped && testjump) {
                                    player.setVelocity(0.0, -0.14, 0.0)
                                    connection.sendPacket(
                                        PlayerMoveC2SPacket.PositionAndOnGround(
                                            player.x,
                                            player.y - 0.14,
                                            player.z,
                                            player.onGround
                                        )
                                    )
                                }
                                if (!ignorer) {
                                    PlaceRender.renderBlocks[currentblock.position.offset(currentblock.facing)] =
                                        System.currentTimeMillis()
                                }
                            }
                            player.inventory.selectedSlot = oldSlot
//                                sendSequencedPacket(world) {
//                                    PlayerInteractBlockC2SPacket(
//                                        Hand.MAIN_HAND, BlockHitResult(
//                                            currentblock.position.toCenterPos(),
//                                            currentblock.facing,
//                                            currentblock.position,
//                                            false
//                                        ), it
//                                    )
//                                }
                        }
                    }
                }
            }
        }

        safeEventListener<PlayerMoveEvent> { event ->
            if (safewalk) doSafeWalk(event)
        }

        onRender3D { event ->
            if (render) {
                currentblock?.let {
                    Render3DEngine.drawFilledBox(
                        event.matrices,
                        Box(it.position.offset(it.facing)),
                        Render2DEngine.injectAlpha(color, color.alpha)
                    )
                    Render3DEngine.drawBoxOutline(
                        Box(it.position.offset(it.facing)),
                        Render2DEngine.injectAlpha(color, color.alpha),
                        2f
                    )
                }
            }
        }
    }

    private fun SafeClientEvent.isOffsetBBEmpty(x: Double, z: Double): Boolean {
        return !world.getBlockCollisions(player, player.boundingBox.expand(-0.1, 0.0, -0.1).offset(x, -2.0, z))
            .iterator().hasNext()
    }

    private fun SafeClientEvent.doSafeWalk(event: PlayerMoveEvent) {
        var x = event.vec.x
        val y = event.vec.y
        var z = event.vec.z
        if (player.isOnGround && !player.noClip) {
            val increment = 0.05
            while (x != 0.0 && isOffsetBBEmpty(x, 0.0)) {
                if (x < increment && x >= -increment) {
                    x = 0.0
                } else if (x > 0.0) {
                    x -= increment
                } else {
                    x += increment
                }
            }
            while (z != 0.0 && isOffsetBBEmpty(0.0, z)) {
                if (z < increment && z >= -increment) {
                    z = 0.0
                } else if (z > 0.0) {
                    z -= increment
                } else {
                    z += increment
                }
            }
            while (x != 0.0 && z != 0.0 && isOffsetBBEmpty(x, z)) {
                if (x < increment && x >= -increment) {
                    x = 0.0
                } else if (x > 0.0) {
                    x -= increment
                } else {
                    x += increment
                }
                if (z < increment && z >= -increment) {
                    z = 0.0
                } else if (z > 0.0) {
                    z -= increment
                } else {
                    z += increment
                }
            }
        }
        event.vec.x = x
        event.vec.y = y
        event.vec.z = z
        event.cancelled = true
    }

    private fun SafeClientEvent.countValidBlocks(): Int {
        var n = 36
        var n2 = 0
        while (n < 45) {
            if (!player.inventory.getStack(if (n >= 36) n - 36 else n).isEmpty) {
                val itemStack = player.inventory.getStack(if (n >= 36) n - 36 else n)
                if (itemStack.item is BlockItem) {
                    if ((itemStack.item as BlockItem).block.defaultState.isSolid) n2 += itemStack.count
                }
            }
            n++
        }
        return n2
    }

    private fun SafeClientEvent.getBlockSlot(): HotbarSlot? {
        return player.hotbarSlots.firstItem<BlockItem, HotbarSlot>()
    }
}