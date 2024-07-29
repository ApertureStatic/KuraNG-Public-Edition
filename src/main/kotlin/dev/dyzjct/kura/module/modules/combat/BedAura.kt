package dev.dyzjct.kura.module.modules.combat

import base.utils.concurrent.threads.runSafe
import base.utils.entity.EntityUtils.getClosestEnemy
import base.utils.extension.fastPos
import base.utils.extension.sendSequencedPacket
import base.utils.graphics.ESPRenderer
import base.utils.math.distanceSqToCenter
import base.utils.math.toBox
import base.utils.math.toVec3dCenter
import base.utils.player.updateController
import dev.dyzjct.kura.event.eventbus.SafeClientEvent
import dev.dyzjct.kura.event.eventbus.safeConcurrentListener
import dev.dyzjct.kura.event.events.RunGameLoopEvent
import dev.dyzjct.kura.event.events.render.Render3DEvent
import dev.dyzjct.kura.manager.HotbarManager.spoofHotbar
import dev.dyzjct.kura.manager.RotationManager
import dev.dyzjct.kura.manager.SphereCalculatorManager
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.module.modules.client.CombatSystem
import dev.dyzjct.kura.module.modules.crystal.CrystalDamageCalculator.calcDamage
import dev.dyzjct.kura.module.modules.crystal.CrystalHelper.getPredictedPos
import dev.dyzjct.kura.module.modules.crystal.CrystalHelper.scaledHealth
import dev.dyzjct.kura.system.render.graphic.mask.DirectionMask
import dev.dyzjct.kura.utils.TimerUtils
import dev.dyzjct.kura.utils.animations.Easing
import dev.dyzjct.kura.utils.animations.sq
import net.minecraft.block.BedBlock
import net.minecraft.block.Blocks
import net.minecraft.client.gui.screen.ingame.CraftingScreen
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.BedItem
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.BlockPos.Mutable
import net.minecraft.util.math.Box
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import java.awt.Color
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.min

object BedAura :
    Module(name = "BedAura", langName = "自动床", description = "Auto using bed for pvp.", category = Category.COMBAT) {
    var range = isetting("Range", 5, 1, 8)
    private var minDmg = isetting("MinDMG", 4, 0, 20)
    private var maxSelfDmg = isetting("MaxSelfDmg", 4, 0, 20)
    private var predictedTicks = isetting("PredictedTicks", 1, 0, 20)
    private var ignoreBox = bsetting("IgnoreBox", true).biggerThan(predictedTicks, 0)
    private var placeDelay = isetting("PlaceDelay", 15, 0, 1000)
    private var clickDelay = isetting("ClickDelay", 15, 0, 1000)
    private var invClickDelay = isetting("InvClickDelay", 5, 0, 1000)
    private var rotate by bsetting("Rotation", false)
    private var motionRender = bsetting("MotionRender", false)
    private var color = csetting("Color", Color(132, 84, 122))
    private var lineColor = csetting("LineColor", Color(132, 84, 122))
    private var blockPos: BlockPos? = null
    private var bedExplodePos: BlockPos? = null
    private var direction: Direction? = null
    private var renderEnt: PlayerEntity? = null
    private var packetTimer = TimerUtils()
    private var placeTimer = TimerUtils()
    private var clickTimer = TimerUtils()
    private var inventoryTimer = TimerUtils()
    private var yawOffset = 0f
    private var offhand = false
    private val renderBlocks = ConcurrentHashMap<BlockPos, Long>()

    //Render
    private var lastUpdateTime = 0L
    private var startTime = 0L
    private var scale = 0.0f

    //Fixed Render
    private var lastBlockPos: BlockPos? = null
    private var lastRenderPos: Vec3d? = null
    private var prevPos: Vec3d? = null
    private var currentPos: Vec3d? = null

    //Expanded Render
    private var lastBlockPosE: BlockPos? = null
    private var lastRenderPosE: Vec3d? = null
    private var prevPosE: Vec3d? = null
    private var currentPosE: Vec3d? = null

    init {
        safeConcurrentListener<RunGameLoopEvent.Tick> {
            runCatching {
                if (CombatSystem.eating && player.isUsingItem) return@safeConcurrentListener
                renderEnt = getClosestEnemy(range.value.toDouble())
                if (renderEnt == null) {
                    blockPos = null
                    return@safeConcurrentListener
                }
                if (mc.currentScreen is CraftingScreen) return@safeConcurrentListener
                var d = minDmg.value.toDouble()
                renderEnt?.let { entity ->
                    val bedPos =
                        if (CombatSystem.oldVersion) canPlaceBed1122(SphereCalculatorManager.sphereList) else canPlaceBed(
                            entity,
                            SphereCalculatorManager.sphereList
                        )
                    for (pos in bedPos) {
                        if (entity.distanceSqToCenter(pos.blockPos) > range.value.sq) continue
                        for (i in pos.canPlaceDirection.indices) {
                            val boost2 = pos.blockPos.add(0, 1, 0).offset(pos.canPlaceDirection[i])
                            val predictTarget =
                                getPredictedPos(entity, predictedTicks.value, ignoreBox = ignoreBox.value)
                            val d2 = calcDamage(
                                entity,
                                predictTarget,
                                entity.boundingBox,
                                boost2.x.toDouble() + 0.5,
                                boost2.y.toDouble() + 0.5,
                                boost2.z.toDouble() + 0.5,
                                Mutable(),
                                isCrystal = false
                            ).toDouble()
                            if (d2 < pos.selfDamage[i] && d2 <= entity.scaledHealth || d2 < d) continue
                            if (d2 <= minDmg.value) continue
                            d = d2
                            blockPos = pos.blockPos
                            direction = pos.canPlaceDirection[i]
                        }
                    }
                }
                offhand = player.getStackInHand(Hand.OFF_HAND).item is BedItem
                var bedSlot =
                    if (player.getStackInHand(Hand.MAIN_HAND).item is BedItem) player.inventory.selectedSlot else -1
                if (bedSlot == -1) {
                    for (l in 0..8) {
                        if (player.inventory.getStack(l).item !is BedItem) continue
                        bedSlot = l
                        break
                    }
                }
                if (bedSlot == -1 && !offhand) {
                    if (mc.currentScreen !is ScreenHandlerContext) {
                        for (i in 9..36) {
                            if (player.inventory.getStack(i).item !is BedItem) continue
                            if (inventoryTimer.tickAndReset(invClickDelay.value)) {
                                if (mc.currentScreen !is CraftingScreen) {
                                    playerController.clickSlot(
                                        player.currentScreenHandler.syncId, i, 0, SlotActionType.QUICK_MOVE, player
                                    )
                                    playerController.updateController()
                                }
                            }
                            break
                        }
                    }
                    bedSlot =
                        if (player.getStackInHand(Hand.MAIN_HAND).item is BedItem) player.inventory.selectedSlot else -1
                    if (bedSlot == -1) {
                        for (l in 0..8) {
                            if (player.inventory.getStack(l).item !is BedItem) continue
                            bedSlot = l
                            break
                        }
                    }
                    if (bedSlot == -1 && !offhand) {
                        return@safeConcurrentListener
                    }
                }
                blockPos?.let { blockPos ->
                    bedExplodePos = blockPos.up()
                    bedExplodePos?.let { bedExplodePos ->
                        if (rotate) RotationManager.rotationTo(bedExplodePos)
                        direction?.let { direction ->
                            renderBlocks[bedExplodePos] = System.currentTimeMillis()
                            when (direction) {
                                Direction.EAST -> {
                                    RotationManager.rotationTo(-91.0f, player.pitch)
                                    yawOffset = -91f
                                    //event.setRotation(-91.0f, player.rotationPitch)
                                }

                                Direction.NORTH -> {
                                    RotationManager.rotationTo(179.0f, player.pitch)
                                    yawOffset = 179f
                                    //event.setRotation(179.0f, player.rotationPitch)
                                }

                                Direction.WEST -> {
                                    RotationManager.rotationTo(89.0f, player.pitch)
                                    yawOffset = 89f
                                    //event.setRotation(89.0f, player.rotationPitch)
                                }

                                else -> {
                                    RotationManager.rotationTo(-1.0f, player.pitch)
                                    yawOffset = -1f
                                    //event.setRotation(-1.0f, player.rotationPitch)
                                }
                            }
                        }
                        if (world.getBlockState(bedExplodePos).block is BedBlock) {
                            connection.sendPacket(HandSwingC2SPacket(if (offhand) Hand.OFF_HAND else Hand.MAIN_HAND))
                            if (clickTimer.tickAndReset(clickDelay.value)) {
                                sendSequencedPacket(world) {
                                    PlayerInteractBlockC2SPacket(
                                        Hand.MAIN_HAND, BlockHitResult(
                                            bedExplodePos.toCenterPos(), Direction.UP, bedExplodePos, false
                                        ), it
                                    )
                                }
                                //sendSequencedPacket(world) { fastPos(bedExplodePos.offset(direction)) }
                            }
                        }
                        if (placeTimer.tickAndReset(placeDelay.value)) {
                            spoofHotbar(bedSlot) {
                                sendSequencedPacket(world) {
                                    fastPos(bedExplodePos, sequence = it)
                                }
                            }
                            connection.sendPacket(HandSwingC2SPacket(if (offhand) Hand.OFF_HAND else Hand.MAIN_HAND))
                            sendSequencedPacket(world) {
                                PlayerInteractBlockC2SPacket(
                                    Hand.MAIN_HAND,
                                    BlockHitResult(bedExplodePos.toCenterPos(), Direction.UP, bedExplodePos, false),
                                    it
                                )
                            }
                        }
                    }
                } ?: {
                    bedExplodePos = null
                }
            }
        }

        onRender3D { event ->
            if (!motionRender.value) {
                bedExplodePos?.let { bedExplodePos ->
                    val posOffset = direction?.let { bedExplodePos.offset(it) } ?: return@onRender3D
                    //val box = Box(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), pos.x + 1.0, pos.y + 0.5, pos.z + 1.0)
                    if (renderBlocks.isNotEmpty()) {
                        renderBlocks.forEach { (pos: BlockPos, time: Long) ->
                            if (System.currentTimeMillis() - time > 250f) {
                                renderBlocks.remove(pos)
                            } else {
                                val scale = Easing.OUT_CUBIC.dec(Easing.toDelta(time, 250f))
                                //event.matrices.translate((bedExplodePos.x - mc.gameRenderer.camera.pos.x).toFloat(), (bedExplodePos.y - mc.gameRenderer.camera.pos.y).toFloat(), (bedExplodePos.z - mc.gameRenderer.camera.pos.z).toFloat())
                                val renderer = ESPRenderer()
                                val box = Box(posOffset)
                                val box2 = Box(bedExplodePos)
                                val bestBox = Box(
                                    min(box.minX, box2.minX),
                                    box.minY,
                                    min(box.minZ, box2.minZ),
                                    max(box.maxX, box2.maxX),
                                    box.maxY - 0.4375,
                                    max(box.maxZ, box2.maxZ)
                                )
                                renderer.aFilled = (color.value.alpha * scale).toInt()
                                renderer.aOutline = (lineColor.value.alpha * scale).toInt()
                                renderer.add(
                                    bestBox, color.value, sides = DirectionMask.ALL xor DirectionMask.SOUTH
                                )
                                renderer.render(event.matrices, false)
                            }
                        }
                    }
                    //Render3DEngine.drawFilledBox(event.matrices, box, color.valueSetting)
                }
            } else {
                onRender3D(event)
            }
        }
    }

    private fun toRenderBox(box: Box, box2: Box): Box {
        return Box(
            min(box.minX, box2.minX),
            box.minY,
            min(box.minZ, box2.minZ),
            max(box.maxX, box2.maxX),
            (box.maxY - 0.4375),
            max(box.maxZ, box2.maxZ)
        )
    }

    private fun onRender3D(event: Render3DEvent) {
        val filled = color.value.alpha > 0
        val outline = lineColor.value.alpha > 0
        val flag = filled || outline

        val posOffset = direction?.let { bedExplodePos?.offset(it) ?: bedExplodePos } ?: bedExplodePos
        if (flag) {
            update(bedExplodePos, posOffset)
            scale = if (bedExplodePos != null && posOffset != null) {
                Easing.OUT_CUBIC.inc(Easing.toDelta(startTime, 300L))
            } else {
                Easing.IN_CUBIC.dec(Easing.toDelta(startTime, 300L))
            }
        }

        prevPos?.let { prevPos ->
            currentPos?.let { currentPos ->
                prevPosE?.let { prevPosE ->
                    currentPosE?.let { currentPosE ->
                        val multiplier = Easing.OUT_QUART.inc(Easing.toDelta(lastUpdateTime, 300L))
                        val prevBox = prevPos.toBox()
                        val motionRenderPos =
                            prevBox.offset(currentPos.subtract(prevPos).multiply(multiplier.toDouble()))
                        val prevBoxE = prevPosE.toBox()
                        val motionRenderPosExpand =
                            prevBoxE.offset(currentPosE.subtract(prevPosE).multiply(multiplier.toDouble()))

                        val box = toRenderBox(
                            motionRenderPos, motionRenderPosExpand
                        )
                        val renderer = ESPRenderer()

                        renderer.aFilled = (color.value.alpha * scale).toInt()
                        renderer.aOutline = (lineColor.value.alpha * scale).toInt()
                        renderer.add(
                            box, color.value, lineColor.value, sides = DirectionMask.ALL xor DirectionMask.SOUTH
                        )
                        renderer.render(event.matrices, false)

                        lastRenderPos = motionRenderPos.center
                        lastRenderPosE = motionRenderPosExpand.center
                    }
                }
            }
        }
    }

    private fun update(fixedPos: BlockPos?, expandedPos: BlockPos?) {
        if (fixedPos != lastBlockPos) {
            if (fixedPos != null) {
                currentPos = fixedPos.toVec3dCenter()
                prevPos = lastRenderPos ?: currentPos
                lastUpdateTime = System.currentTimeMillis()
                if (lastBlockPos == null) startTime = System.currentTimeMillis()
            } else {
                lastUpdateTime = System.currentTimeMillis()
                if (lastBlockPos != null) startTime = System.currentTimeMillis()
            }

            lastBlockPos = fixedPos
        }
        if (expandedPos != lastBlockPosE) {
            if (expandedPos != null) {
                currentPosE = expandedPos.toVec3dCenter()
                prevPosE = lastRenderPosE ?: currentPosE
                lastUpdateTime = System.currentTimeMillis()
                if (lastBlockPosE == null) startTime = System.currentTimeMillis()
            } else {
                lastUpdateTime = System.currentTimeMillis()
                if (lastBlockPosE != null) startTime = System.currentTimeMillis()
            }

            lastBlockPosE = expandedPos
        }
    }

    override fun onEnable() {
        runSafe {
            blockPos = null
            prevPos = null
            currentPos = null
            lastRenderPos = null
            lastBlockPos = null
            lastUpdateTime = 0L
            startTime = 0L
            scale = 0.0f
            packetTimer.reset()
            clickTimer.reset()
            inventoryTimer.reset()
        }
    }

    private fun SafeClientEvent.canPlaceBed(
        target: PlayerEntity, blockPosList: List<BlockPos>
    ): List<BedSaver> {
        val bedSaverList = ArrayList<BedSaver>()
        val list = ArrayList<Direction>()
        val damage = ArrayList<Float>()
        for (pos in blockPosList) {
            for (facing in Direction.entries) {
                if (facing == Direction.UP || facing == Direction.DOWN) continue
                var selfDmg = 0f
                val boost = pos.add(0, 1, 0)
                val boost2 = pos.add(0, 1, 0).offset(facing)
                val boostBlock = world.getBlockState(boost).block
                val boostBlock2 = world.getBlockState(boost2).block
                if (!world.isAir(boost) && boostBlock !is BedBlock || !world.isAir(boost2) && boostBlock2 !is BedBlock || !world.getBlockState(
                        pos
                    ).isOpaqueFullCube(world, pos) || target.boundingBox.intersects(Box(boost)) || calcDamage(
                        player,
                        player.pos,
                        player.boundingBox,
                        boost2.x.toDouble() + 0.5,
                        boost2.y.toDouble() + 0.5,
                        boost2.z.toDouble() + 0.5,
                        Mutable(),
                        isCrystal = false
                    ).also {
                        selfDmg = it
                    } > maxSelfDmg.value.toDouble() || selfDmg >= player.scaledHealth
                ) continue
                list.add(facing)
                damage.add(selfDmg)
            }
            if (list.isEmpty()) continue
            bedSaverList.add(BedSaver(pos, list, damage))
            list.clear()
            damage.clear()
        }
        return bedSaverList
    }

    private fun SafeClientEvent.canPlaceBed1122(
        blockPosList: List<BlockPos>
    ): List<BedSaver> {
        val bedSaverList = ArrayList<BedSaver>()
        val list = ArrayList<Direction>()
        val damage = ArrayList<Float>()
        for (pos in blockPosList) {
            for (facing in Direction.entries) {
                if (facing == Direction.UP || facing == Direction.DOWN) continue
                var selfDmg = 0.0f
                val side = pos.offset(facing)
                val boost = pos.add(0, 1, 0)
                val boost2 = pos.add(0, 1, 0).offset(facing)
                val boostBlock = world.getBlockState(boost).block
                val boostBlock2 = world.getBlockState(boost2).block
                if (!world.isAir(boost) && boostBlock !is BedBlock || boostBlock2 !== Blocks.AIR && boostBlock2 !is BedBlock || !world.getBlockState(
                        side
                    ).isOpaqueFullCube(world, side) || !world.getBlockState(
                        pos
                    ).isOpaqueFullCube(world, pos) || calcDamage(
                        player,
                        player.pos,
                        player.boundingBox,
                        boost2.x.toDouble() + 0.5,
                        boost2.y.toDouble() + 0.5,
                        boost2.z.toDouble() + 0.5,
                        Mutable()
                    ).also {
                        selfDmg = it
                    } > maxSelfDmg.value.toDouble() || selfDmg >= (player.health + player.absorptionAmount + 2.0f).toDouble()
                ) continue
                list.add(facing)
                damage.add(selfDmg)
            }
            if (list.isEmpty()) continue
            bedSaverList.add(BedSaver(pos, list, damage))
            list.clear()
            damage.clear()
        }
        return bedSaverList
    }

    class BedSaver(var blockPos: BlockPos, canPlaceDirection: List<Direction>, selfDamage: List<Float>) {
        var canPlaceDirection: List<Direction>
        var selfDamage: List<Float>

        init {
            this.canPlaceDirection = ArrayList(canPlaceDirection)
            this.selfDamage = ArrayList(selfDamage)
        }
    }
}