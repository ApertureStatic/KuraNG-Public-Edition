package dev.dyzjct.kura.module.modules.combat

import base.events.RunGameLoopEvent
import base.events.render.Render3DEvent
import base.system.event.SafeClientEvent
import base.system.event.safeConcurrentListener
import base.utils.block.BlockUtil.getAnchorBlock
import base.utils.block.BlockUtil.getNeighbor
import base.utils.chat.ChatUtil
import base.utils.combat.TargetInfo
import base.utils.combat.getPredictedTarget
import base.utils.entity.EntityUtils.isBurrowBlock
import base.utils.entity.EntityUtils.spoofSneak
import base.utils.extension.fastPos
import base.utils.extension.sendSequencedPacket
import base.utils.graphics.ESPRenderer
import base.utils.math.DamageCalculator.anchorDamage
import base.utils.math.distanceSqTo
import base.utils.math.distanceSqToCenter
import base.utils.math.toBlockPos
import base.utils.math.toVec3dCenter
import base.utils.player.getTargetSpeed
import dev.dyzjct.kura.manager.*
import dev.dyzjct.kura.manager.HotbarManager.spoofHotbarWithSetting
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.module.modules.crystal.AutoCrystal
import dev.dyzjct.kura.module.modules.crystal.CrystalHelper.scaledHealth
import dev.dyzjct.kura.module.modules.crystal.PlaceInfo
import dev.dyzjct.kura.utils.TimerUtils
import dev.dyzjct.kura.utils.animations.Easing
import dev.dyzjct.kura.utils.animations.sq
import net.minecraft.block.Blocks
import net.minecraft.block.RespawnAnchorBlock
import net.minecraft.entity.ItemEntity
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket
import net.minecraft.state.property.Properties
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import java.awt.Color
import java.util.concurrent.CopyOnWriteArrayList
import java.util.stream.Collectors

object AnchorAura : Module(
    name = "AnchorAura", langName = "恶俗狗", category = Category.COMBAT, description = "Auto using crystals for pvp."
) {
    private var targetRange by dsetting("TargetRange", 8.0, 0.1, 20.0)
    var placeRange = dsetting("PlaceRange", 4.5, 0.1, 6.0)
    private var maxTargets = isetting("MaxTarget", 3, 1, 8)
    private var predictTicks = isetting("PredictedTicks", 4, 0, 20)
    private var noSuicide = fsetting("NoSuicide", 2f, 0f, 20f)
    private var maxSelfDamage = isetting("MaxSelfDamage", 10, 0, 36)
    private var minDamage = dsetting("MinDamage", 4.0, 0.0, 36.0)
    private val globalDelay by isetting("GlobalDelay", 50, 0, 500)
    private var airPlace = bsetting("AirPlace", false)
    private val strictDirection = bsetting("StrictDirection", false)
    private val disableCrystalAura by bsetting("DisableCAura", false)
    private val rotate = bsetting("Rotation", false)
    private val swing = bsetting("Swing", true)
    private val packetSwing by bsetting("PacketSwing", true).isTrue(swing)
    private val render = bsetting("Render", true)
    private val fillColor = csetting("FillColor", Color(255, 255, 255, 50)).isTrue(render)
    private val lineColor = csetting("LineColor", Color(255, 255, 255, 255)).isTrue(render)
    private val movingLength = isetting("MovingLength", 400, 0, 1000).isTrue(render)
    private val fadeLength = isetting("FadeLength", 200, 0, 1000).isTrue(render)
    private val debug = bsetting("Debug", false)

    private var lastBlockPos: BlockPos? = null
    private var lastRenderPos: Vec3d? = null
    private var prevPos: Vec3d? = null
    private var currentPos: Vec3d? = null
    private var lastCrystalAuraState = false
    private var placeInfo: PlaceInfo? = null
    private val globalTimer = TimerUtils()
    private var rawPosList = CopyOnWriteArrayList<BlockPos>()

    private var lastTargetDamage = 0.0
    private var lastUpdateTime = 0L
    private var startTime = 0L
    private var scale = 0.0f

    override fun onEnable() {
        globalTimer.reset()
        prevPos = null
        currentPos = null
        lastRenderPos = null
        lastBlockPos = null
        lastTargetDamage = 0.0
        lastUpdateTime = 0L
        startTime = 0L
        scale = 0.0f
        if (disableCrystalAura && AutoCrystal.isEnabled) {
            lastCrystalAuraState = true
            AutoCrystal.disable()
        }
    }

    override fun onDisable() {
        super.onDisable()
        if (lastCrystalAuraState) {
            lastCrystalAuraState = false
            AutoCrystal.enable()
        }
    }

    private fun update(placeInfo: PlaceInfo?) {
        if (placeInfo?.blockPos != lastBlockPos) {
            if (placeInfo?.blockPos != null) {
                currentPos = placeInfo.blockPos.toVec3dCenter()
                prevPos = lastRenderPos ?: currentPos
                lastUpdateTime = System.currentTimeMillis()
                if (lastBlockPos == null) startTime = System.currentTimeMillis()
            } else {
                lastUpdateTime = System.currentTimeMillis()
                if (lastBlockPos != null) startTime = System.currentTimeMillis()
            }

            lastBlockPos = placeInfo?.blockPos
        }

        placeInfo?.let {
            lastTargetDamage = it.targetDamage
        }
    }

    private fun toRenderBox(vec3d: Vec3d, scale: Float): Box {
        val halfSize = 0.5 * scale
        return Box(
            vec3d.x - halfSize,
            vec3d.y - halfSize,
            vec3d.z - halfSize,
            vec3d.x + halfSize,
            vec3d.y + halfSize,
            vec3d.z + halfSize
        )
    }

    private fun onRender3D(event: Render3DEvent, placeInfo: PlaceInfo?) {
        val filled = fillColor.value.alpha > 0
        val outline = lineColor.value.alpha > 0
        val flag = filled || outline

        if (flag) {
            try {
                update(placeInfo)
                scale = if (placeInfo != null) {
                    Easing.OUT_CUBIC.inc(Easing.toDelta(startTime, fadeLength.value))
                } else {
                    Easing.IN_CUBIC.dec(Easing.toDelta(startTime, fadeLength.value))
                }

                prevPos?.let { prevPos ->
                    currentPos?.let { currentPos ->
                        val multiplier = Easing.OUT_QUART.inc(
                            Easing.toDelta(
                                lastUpdateTime, movingLength.value
                            )
                        )
                        val motionRenderPos = prevPos.add(currentPos.subtract(prevPos).multiply(multiplier.toDouble()))

                        val box = toRenderBox(motionRenderPos, scale)
                        val renderer = ESPRenderer()

                        renderer.aFilled = (fillColor.value.alpha * scale).toInt()
                        renderer.aOutline = (lineColor.value.alpha * scale).toInt()
                        renderer.add(box, fillColor.value, lineColor.value)
                        renderer.render(event.matrices, false)

                        lastRenderPos = motionRenderPos
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun SafeClientEvent.getPlaceablePos(): CopyOnWriteArrayList<BlockPos> {
        val positions = CopyOnWriteArrayList<BlockPos>()
        positions.addAll(
            SphereCalculatorManager.sphereList.stream()
                .filter { world.isInBuildLimit(it.up()) && world.worldBorder.contains(it.up()) }
                .filter { player.distanceSqToCenter(it.up()) <= placeRange.value.sq }
                .filter { world.isAir(it.up()) || world.getBlockState(it.up()).block is RespawnAnchorBlock }.filter {
                    if (airPlace.value) {
                        true
                    } else {
                        getNeighbor(it.up(), strictDirection.value) != null
                    }
                }.sorted(Comparator.comparingInt { interactPriority(it.up()) }).collect(Collectors.toList())
        )
        return positions
    }

    private fun SafeClientEvent.interactPriority(blockPos: BlockPos): Int {
        var priority = 0
        if (world.getBlockState(blockPos).block is RespawnAnchorBlock) {
            priority++
        }
        return priority
    }

    private fun SafeClientEvent.calcPlaceInfo(): PlaceInfo? {
        val placeInfo: PlaceInfo.Mutable?
        val normal = PlaceInfo.Mutable(player)

        val targets = targetList.toList()
        if (targets.isEmpty()) return null

        if (rawPosList.isEmpty()) return null

        outerList@ for (pos in rawPosList) {
            val blockPos = pos.toCenterPos().add(0.0, 0.5, 0.0).toBlockPos()
            val placeBox = Box(blockPos)
            if (!world.entities.none {
                    it !is ItemEntity;it.isAlive;it.boundingBox.intersects(
                    placeBox
                )
                } && world.isAir(pos)) continue
            val selfDamage = anchorDamage(player, player.pos, player.boundingBox, blockPos)
            if (player.scaledHealth - selfDamage <= noSuicide.value) continue
            if (selfDamage > maxSelfDamage.value) continue

            for ((target, targetPos, targetBox, currentPos) in targets) {
                if (targetBox.intersects(placeBox)) continue
                if (placeBox.intersects(targetPos, currentPos.toVec3dCenter())) continue
                val targetDamage = anchorDamage(target, targetPos, targetBox, blockPos)
                val minDamage = minDamage.value
                val balance = -8f
                if (!isBurrowBlock(target.blockPos) && world.entities.none {
                        it !is ItemEntity;it.isAlive;it.boundingBox.intersects(
                        Box(target.blockPos.up(2))
                    )
                    } && (world.isAir(
                        target.blockPos.up(2)
                    ) || world.getBlockState(
                        target.blockPos.up(2)
                    ).block is RespawnAnchorBlock) && if (airPlace.value) true else (if (!strictDirection.value) getNeighbor(
                        target.blockPos.up(2), false
                    ) != null else getAnchorBlock(target.blockPos.up(2), true) != null) && player.distanceSqToCenter(
                        target.blockPos.up(2)
                    ) <= placeRange.value.sq) {
                    normal.update(
                        target, target.blockPos.up(2), selfDamage, targetDamage
                    )
                } else {
                    if (targetDamage >= minDamage && targetDamage - selfDamage >= balance && (if (!strictDirection.value) getNeighbor(
                            blockPos,
                            false
                        ) != null else getAnchorBlock(blockPos, true) != null) || airPlace.value
                    ) {
                        if (targetDamage > normal.targetDamage) {
                            normal.update(
                                target, blockPos, selfDamage, targetDamage
                            )
                        }
                    }
                }
            }
        }
        placeInfo = normal.takeValid()
        placeInfo?.calcPlacement(this)
        return placeInfo
    }

    init {
        safeConcurrentListener<RunGameLoopEvent.Tick> {
            runCatching {
                rawPosList = getPlaceablePos()
                placeInfo = calcPlaceInfo()
                placeInfo?.let { placeInfo ->
                    if (rotate.value) RotationManager.addRotations(placeInfo.blockPos)
                    if (globalTimer.tickAndReset(globalDelay)) {
                        globalPlace(Items.RESPAWN_ANCHOR, placeInfo, true)
                        checkGlowPlaceable(placeInfo, Items.GLOWSTONE, true)
                        globalPlace(Items.RESPAWN_ANCHOR, placeInfo, false)
                        if (getTargetSpeed(placeInfo.target) < 10) {
                            globalPlace(Items.RESPAWN_ANCHOR, placeInfo, true)
                        }
                    }
                }
            }
        }

        onRender3D { event ->
            onRender3D(event, placeInfo)
        }
    }

    private fun SafeClientEvent.globalPlace(item: Item, placeInfo: PlaceInfo, explode: Boolean) {
        if (explode) {
            AutoWeb.onAnchorPlacing = true
            player.spoofSneak {
                spoofHotbarWithSetting(item) {
                    sendSequencedPacket(world) {
                        fastPos(
                            placeInfo.blockPos,
                            face = placeInfo.side,
                            strictDirection = strictDirection.value,
                            sequence = it,
                            render = false
                        )
                    }
                }
            }
            AutoWeb.onAnchorPlacing = false
        } else {
            sendSequencedPacket(world) {
                PlayerInteractBlockC2SPacket(
                    Hand.MAIN_HAND, BlockHitResult(
                        placeInfo.blockPos.toCenterPos(),
                        getAnchorBlock(placeInfo.blockPos, strictDirection.value)?.clickFace ?: placeInfo.side,
                        placeInfo.blockPos,
                        true
                    ), it
                )
            }
        }
        if (swing.value) {
            if (packetSwing) connection.sendPacket(HandSwingC2SPacket(Hand.MAIN_HAND)) else player.swingHand(Hand.MAIN_HAND)
        }
    }

    private fun SafeClientEvent.checkGlowPlaceable(
        placeInfo: PlaceInfo, item: Item, ignore: Boolean = false
    ) {
        val currentBlockState = world.getBlockState(placeInfo.blockPos)
        if ((currentBlockState.block == Blocks.RESPAWN_ANCHOR && currentBlockState.get(Properties.CHARGES) < 1) || ignore) {
            spoofHotbarWithSetting(item) {
                sendSequencedPacket(world) {
                    PlayerInteractBlockC2SPacket(
                        Hand.MAIN_HAND, BlockHitResult(
                            placeInfo.blockPos.toCenterPos(),
                            getAnchorBlock(placeInfo.blockPos, strictDirection.value)?.clickFace ?: placeInfo.side,
                            placeInfo.blockPos,
                            true
                        ), it
                    )
                }
                if (debug.value) ChatUtil.sendMessage(
                    "[ANCHOR -> glowSide = ${
                        getAnchorBlock(
                            placeInfo.blockPos,
                            strictDirection.value
                        )?.clickFace ?: placeInfo.side
                    }"
                )
                if (swing.value) {
                    if (packetSwing) connection.sendPacket(HandSwingC2SPacket(Hand.MAIN_HAND)) else player.swingHand(
                        Hand.MAIN_HAND
                    )
                }
            }
        }
    }

    private val SafeClientEvent.targetList: Sequence<TargetInfo>
        get() {
            val rangeSq = targetRange.sq
            val list = CopyOnWriteArrayList<TargetInfo>()
            val eyePos = CrystalManager.eyePosition

            for (target in EntityManager.players) {
                if (target == player) continue
                if (!target.isAlive) continue
                if (target.distanceSqTo(eyePos) > rangeSq) continue
                if (FriendManager.isFriend(target.entityName)) continue

                list.add(getPredictedTarget(target, predictTicks.value))
            }

            return list.asSequence().filter { it.entity.isAlive }
                .sortedWith(compareByDescending<TargetInfo> { it.entity.scaledHealth }).take(maxTargets.value)
        }
}