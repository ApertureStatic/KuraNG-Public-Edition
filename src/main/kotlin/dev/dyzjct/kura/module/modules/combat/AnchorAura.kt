package dev.dyzjct.kura.module.modules.combat

import base.events.render.Render3DEvent
import base.system.event.SafeClientEvent
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
import dev.dyzjct.kura.module.modules.client.CombatSystem
import dev.dyzjct.kura.module.modules.client.CombatSystem.swing
import dev.dyzjct.kura.module.modules.crystal.AutoCrystal
import dev.dyzjct.kura.module.modules.crystal.CrystalDamageCalculator.anchorDamageNew
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
    private var minDamage by dsetting("MinDamage", 4.0, 0.0, 36.0)
    private var maxSelfDamage by isetting("MaxSelfDamage", 10, 0, 36)
    private var noSuicide by fsetting("NoSuicide", 2f, 0f, 20f)
    private val glowDelay by isetting("GlowDelay", 50, 0, 500)
    private val anchorDelay by isetting("AnchorDelay", 50, 0, 500)
    private val clickDelay by isetting("ClickDelay", 50, 0, 500)
    private val damageMode by msetting("DamageMode", DamageMode.Thunder)
    private var airPlace by bsetting("AirPlace", false)
    private val strictDirection by bsetting("StrictDirection", false)
    private val rotate by bsetting("Rotation", false)
    private val fillColor by csetting("FillColor", Color(255, 255, 255, 50))
    private val lineColor by csetting("LineColor", Color(255, 255, 255, 255))
    private val movingLength by isetting("MovingLength", 400, 0, 1000)
    private val fadeLength by isetting("FadeLength", 200, 0, 1000)
    private val debug by bsetting("Debug", false)

    private var lastBlockPos: BlockPos? = null
    private var lastRenderPos: Vec3d? = null
    private var prevPos: Vec3d? = null
    private var currentPos: Vec3d? = null
    private var lastCrystalAuraState = false
    var placeInfo: PlaceInfo? = null
    private val glowTimer = TimerUtils()
    private val anchorTimer = TimerUtils()
    private val clickTimer = TimerUtils()
    private var rawPosList = CopyOnWriteArrayList<BlockPos>()

    private var lastTargetDamage = 0.0
    private var lastUpdateTime = 0L
    private var startTime = 0L
    private var scale = 0.0f

    var anchorDamage = 0.0

    override fun onEnable() {
        prevPos = null
        currentPos = null
        lastRenderPos = null
        lastBlockPos = null
        lastTargetDamage = 0.0
        lastUpdateTime = 0L
        startTime = 0L
        scale = 0.0f
        if (CombatSystem.autoToggle && AutoCrystal.isEnabled && !CombatSystem.smartAura) {
            if (CombatSystem.mainToggle != CombatSystem.MainToggle.Anchor) lastCrystalAuraState = true
            AutoCrystal.disable()
        }
    }

    override fun onDisable() {
        if (lastCrystalAuraState) {
            lastCrystalAuraState = false
            AutoCrystal.enable()
        }
    }

    private fun update(placeInfo: PlaceInfo?) {
        if (placeInfo?.blockPos != lastBlockPos) {
            if (placeInfo?.blockPos != null && CombatSystem.isBestAura(CombatSystem.AuraType.Anchor)) {
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

    init {
        onMotion {
            if (CombatSystem.eating && player.isUsingItem) return@onMotion
            if (!spoofHotbarWithSetting(Items.GLOWSTONE, true) {} || !spoofHotbarWithSetting(
                    Items.RESPAWN_ANCHOR,
                    true
                ) {}) {
                anchorDamage = 0.0
                return@onMotion
            }
            rawPosList = getPlaceablePos()
            placeInfo = calcPlaceInfo()
            if (!CombatSystem.isBestAura(CombatSystem.AuraType.Anchor)) return@onMotion
            placeInfo?.let { placeInfo ->
                if (rotate) RotationManager.addRotations(placeInfo.blockPos)
                globalPlace(placeInfo, true)
                checkGlowPlaceable(placeInfo, Items.GLOWSTONE)
                globalPlace(placeInfo, false)
                if (getTargetSpeed(placeInfo.target) < 10) {
                    globalPlace(placeInfo, true)
                }
            }
        }

        onRender3D { event ->
            onRender3D(event, placeInfo)
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
        val filled = fillColor.alpha > 0
        val outline = lineColor.alpha > 0
        val flag = filled || outline

        if (flag) {
            try {
                update(placeInfo)
                scale = if (placeInfo != null) {
                    Easing.OUT_CUBIC.inc(Easing.toDelta(startTime, fadeLength))
                } else {
                    Easing.IN_CUBIC.dec(Easing.toDelta(startTime, fadeLength))
                }

                prevPos?.let { prevPos ->
                    currentPos?.let { currentPos ->
                        val multiplier = Easing.OUT_QUART.inc(
                            Easing.toDelta(
                                lastUpdateTime, movingLength
                            )
                        )
                        val motionRenderPos = prevPos.add(currentPos.subtract(prevPos).multiply(multiplier.toDouble()))

                        val box = toRenderBox(motionRenderPos, scale)
                        val renderer = ESPRenderer()

                        renderer.aFilled = (fillColor.alpha * scale).toInt()
                        renderer.aOutline = (lineColor.alpha * scale).toInt()
                        renderer.add(box, fillColor, lineColor)
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
                .filter { player.distanceSqToCenter(it.up()) <= CombatSystem.placeRange.sq }
                .filter { world.isAir(it.up()) || world.getBlockState(it.up()).block is RespawnAnchorBlock }.filter {
                    if (airPlace) {
                        true
                    } else {
                        getNeighbor(it.up(), strictDirection) != null
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
            val selfDamage = when (damageMode) {
                DamageMode.Thunder -> {
                    anchorDamageNew(blockPos, player).toDouble()
                }

                else -> {
                    anchorDamage(player, player.pos, player.boundingBox, blockPos)
                }
            }

            if (player.scaledHealth - selfDamage <= noSuicide) continue
            if (selfDamage > maxSelfDamage) continue

            for ((target, targetPos, targetBox, currentPos) in targets) {
                if (targetBox.intersects(placeBox)) continue
                if (placeBox.intersects(targetPos, currentPos.toVec3dCenter())) continue
                val targetDamage = when (damageMode) {
                    DamageMode.Thunder -> {
                        anchorDamageNew(blockPos, target).toDouble()
                    }

                    else -> {
                        anchorDamage(target, targetPos, targetBox, blockPos)
                    }
                }
                val minDamage = minDamage
                val headPos = target.blockPos.up(2)
                if (!isBurrowBlock(target.blockPos, target)) {
                    if (world.entities.none {
                            it !is ItemEntity;it.isAlive;it.boundingBox.intersects(
                            Box(headPos)
                        )
                        } && (world.isAir(
                            headPos
                        ) || world.getBlockState(
                            headPos
                        ).block is RespawnAnchorBlock) && if (airPlace) true else (if (!strictDirection) getNeighbor(
                            headPos, false
                        ) != null else getAnchorBlock(headPos, true) != null) && player.distanceSqToCenter(
                            headPos
                        ) <= CombatSystem.placeRange.sq && (world.isAir(target.blockPos.up()) || world.getBlockState(
                            target.blockPos.up()
                        ).block == Blocks.COBWEB)) {
                        normal.update(
                            target, headPos, selfDamage, targetDamage
                        )
                        anchorDamage = 255.0
                    } else {
                        if (targetDamage >= minDamage && (if (!strictDirection) getNeighbor(
                                blockPos,
                                false
                            ) != null else getAnchorBlock(blockPos, true) != null || airPlace)
                        ) {
                            if (targetDamage > normal.targetDamage) {
                                normal.update(
                                    target, blockPos, selfDamage, targetDamage
                                )
                                anchorDamage = targetDamage
                            }
                        }
                    }
                }
            }
        }
        placeInfo = normal.takeValid()
        placeInfo?.calcPlacement(this)
        return placeInfo
    }

    private fun SafeClientEvent.globalPlace(placeInfo: PlaceInfo, explode: Boolean) {
        if (explode) {
            if (anchorTimer.tickAndReset(anchorDelay) && world.isAir(placeInfo.blockPos)) {
                AutoWeb.onAnchorPlacing = true
                if (rotate) RotationManager.addRotations(placeInfo.blockPos)
                player.spoofSneak {
                    spoofHotbarWithSetting(Items.RESPAWN_ANCHOR) {
                        sendSequencedPacket(world) {
                            fastPos(
                                placeInfo.blockPos,
                                face = placeInfo.side,
                                strictDirection = strictDirection,
                                sequence = it,
                                render = false
                            )
                        }
                    }
                }
                swing()
                AutoWeb.onAnchorPlacing = false
            }
        } else {
            if (clickTimer.tickAndReset(clickDelay)) {
                AutoWeb.onAnchorPlacing = true
                if (rotate) RotationManager.addRotations(placeInfo.blockPos)
                sendSequencedPacket(world) {
                    PlayerInteractBlockC2SPacket(
                        Hand.MAIN_HAND, BlockHitResult(
                            placeInfo.blockPos.toCenterPos(),
                            getAnchorBlock(placeInfo.blockPos, strictDirection)?.clickFace ?: placeInfo.side,
                            placeInfo.blockPos,
                            true
                        ), it
                    )
                }
                swing()
                AutoWeb.onAnchorPlacing = false
            }
        }
    }

    private fun SafeClientEvent.checkGlowPlaceable(
        placeInfo: PlaceInfo, item: Item, ignore: Boolean = false
    ) {
        val currentBlockState = world.getBlockState(placeInfo.blockPos)
        if ((currentBlockState.block == Blocks.RESPAWN_ANCHOR && currentBlockState.get(Properties.CHARGES) < 1) || ignore) {
            if (glowTimer.tickAndReset(glowDelay)) {
                AutoWeb.onAnchorPlacing = true
                if (rotate) RotationManager.addRotations(placeInfo.blockPos)
                spoofHotbarWithSetting(item) {
                    sendSequencedPacket(world) {
                        PlayerInteractBlockC2SPacket(
                            Hand.MAIN_HAND, BlockHitResult(
                                placeInfo.blockPos.toCenterPos(),
                                getAnchorBlock(placeInfo.blockPos, strictDirection)?.clickFace ?: placeInfo.side,
                                placeInfo.blockPos,
                                true
                            ), it
                        )
                    }
                    if (debug) ChatUtil.sendMessage(
                        "[ANCHOR -> glowSide = ${
                            getAnchorBlock(
                                placeInfo.blockPos,
                                strictDirection
                            )?.clickFace ?: placeInfo.side
                        }"
                    )
                    swing()
                }
                AutoWeb.onAnchorPlacing = false
            }
        }
    }

    private val SafeClientEvent.targetList: Sequence<TargetInfo>
        get() {
            val rangeSq = CombatSystem.targetRange.sq
            val list = CopyOnWriteArrayList<TargetInfo>()
            val eyePos = CrystalManager.eyePosition

            for (target in EntityManager.players) {
                if (target == player) continue
                if (!target.isAlive) continue
                if (target.distanceSqTo(eyePos) > rangeSq) continue
                if (FriendManager.isFriend(target.name.string)) continue

                list.add(getPredictedTarget(target, CombatSystem.predictTicks))
            }

            return list.asSequence().filter { it.entity.isAlive }
                .sortedWith(compareByDescending<TargetInfo> { it.entity.scaledHealth }).take(CombatSystem.maxTargets)
        }

    @Suppress("UNUSED")
    enum class DamageMode {
        Melon, Thunder
    }
}