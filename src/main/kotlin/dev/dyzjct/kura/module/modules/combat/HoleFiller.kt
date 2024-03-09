package dev.dyzjct.kura.module.modules.combat

import dev.dyzjct.kura.manager.*
import dev.dyzjct.kura.manager.HotbarManager.spoofHotbar
import dev.dyzjct.kura.manager.HotbarManager.spoofHotbarBypass
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.module.modules.player.AntiMinePlace
import dev.dyzjct.kura.utils.animations.Easing
import dev.dyzjct.kura.utils.animations.sq
import dev.dyzjct.kura.utils.inventory.HotbarSlot
import dev.dyzjct.kura.utils.runIf
import it.unimi.dsi.fastutil.longs.Long2LongMaps
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2LongMaps
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap
import base.events.WorldEvent
import base.system.event.SafeClientEvent
import base.system.event.listener
import base.system.util.color.ColorRGB
import base.utils.TickTimer
import base.utils.concurrent.threads.onMainThread
import base.utils.concurrent.threads.runSynchronized
import base.utils.entity.EntityUtils.eyePosition
import base.utils.entity.EntityUtils.isFriend
import base.utils.entity.EntityUtils.preventEntitySpawning
import base.utils.entity.EntityUtils.spoofSneak
import base.utils.extension.fastPos
import base.utils.graphics.ESPRenderer
import base.utils.hole.HoleType
import base.utils.inventory.slot.firstBlock
import base.utils.inventory.slot.hotbarSlots
import net.minecraft.block.Blocks
import net.minecraft.entity.Entity
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket
import net.minecraft.util.Hand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import team.exception.melon.util.math.distance
import team.exception.melon.util.math.distanceSqToCenter
import team.exception.melon.util.math.isInSight
import java.awt.Color
import java.util.*

object HoleFiller :
    Module(name = "HoleFiller", langName = "自动填坑", description = "Auto Hole Filling.", category = Category.COMBAT) {
    private var bedrockHole = bsetting("BedrockHole", true)
    private var obbyHole = bsetting("ObbyHole", true)
    private var twoBlocksHole = bsetting("2BlocksHole", true)
    private var fourBlocksHole = bsetting("4BlocksHole", true)
    private var predictTicks = isetting("PredictTicks", 8, 0, 50)
    private var detectRange = fsetting("DetectRange", 5.0f, 0.0f, 16.0f)
    private var hRange = fsetting("HRange", 0.5f, 0.0f, 4.0f)
    private var vRange = fsetting("VRange", 3.0f, 0.0f, 8.0f)
    private var distanceBalance = fsetting("DistanceBalance", 1.0f, -5.0f, 5.0f)
    private var fillDelay = isetting("FillDelay", 50, 0, 1000)
    private var fillTimeout = isetting("FillTimeout", 100, 0, 1000)
    private var fillRange = fsetting("FillRange", 5.0f, 1.0f, 6.0f)
    private var rotation = bsetting("Rotation", true)
    private var webFill = bsetting("WebFill", false)
    private var switchBypass = bsetting("SwitchBypass", false)
    private var targetColor = csetting("TargetColor", Color(32, 255, 32, 50))
    private var otherColor = csetting("OtherColor", Color(255, 222, 32, 50))
    private var filledColor = csetting("FilledColor", Color(255, 32, 32, 50))

    private val placeMap = Long2LongMaps.synchronize(Long2LongOpenHashMap())
    private val updateTimer = TickTimer()
    private val placeTimer = TickTimer()

    private var holeInfos = emptyList<IntermediateHoleInfo>()
    private var nextHole: BlockPos? = null
    private val renderBlockMap = Object2LongMaps.synchronize(Object2LongOpenHashMap<BlockPos>())
    private val renderer = ESPRenderer().apply { aFilled = 33; aOutline = 233 }

    override fun onDisable() {
        holeInfos = emptyList()
        nextHole = null
        renderBlockMap.clear()
        renderer.replaceAll(Collections.emptyList())
    }

    init {
        listener<WorldEvent.ClientBlockUpdate> {
            if (!it.newState.isReplaceable) {
                placeMap.remove(it.pos.asLong())
                if (it.pos == nextHole) nextHole = null
                renderBlockMap.runSynchronized {
                    replace(it.pos, System.currentTimeMillis())
                }
            }
        }

        onRender3D { event ->
            val list = ArrayList<ESPRenderer.Info>()
            renderBlockMap.runSynchronized {
                object2LongEntrySet().mapTo(list) {
                    val color = when {
                        it.key == nextHole -> targetColor.value
                        it.longValue == -1L -> otherColor.value
                        else -> filledColor.value
                    }
                    val c = ColorRGB(color)
                    if (it.longValue == -1L) {
                        ESPRenderer.Info(it.key, c)
                    } else {
                        val progress = Easing.IN_CUBIC.dec(Easing.toDelta(it.longValue, 1000L))
                        val size = progress * 0.5
                        val n = 0.5 - size
                        val p = 0.5 + size
                        val box = Box(
                            it.key.x + n, it.key.y + n, it.key.z + n,
                            it.key.x + p, it.key.y + p, it.key.z + p,
                        )
                        ESPRenderer.Info(box, c.alpha((255.0f * progress).toInt()))
                    }
                }
            }
            renderer.replaceAll(list)
            renderer.render(event.matrices, false)
        }

        onLoop {
            val slot =
                if (webFill.value) player.hotbarSlots.firstBlock(Blocks.COBWEB)
                    ?: player.hotbarSlots.firstBlock(Blocks.OBSIDIAN)
                    ?: return@onLoop else player.hotbarSlots.firstBlock(Blocks.OBSIDIAN) ?: return@onLoop
            val place = placeTimer.tick(fillDelay.value)

            if (place || updateTimer.tickAndReset(25L)) {
                val newHoleInfo = getHoleInfos()
                holeInfos = newHoleInfo

                val current = System.currentTimeMillis()
                placeMap.runSynchronized {
                    values.removeIf { it <= current }
                    nextHole?.let {
                        if (!containsKey(it.asLong())) nextHole = null
                    }
                }

                if (place) {
                    getPos(newHoleInfo, rotation.value)?.let {
                        fun doPlace() {
                            nextHole = it
                            placeBlock(slot, it)
                        }
                        AntiMinePlace.mineMap[it]?.let { mine ->
                            if (System.currentTimeMillis() - mine.start < mine.mine) doPlace()
                        } ?: doPlace()
                    }
                } else {
                    updatePosRender(newHoleInfo)
                }
            }
        }
    }

    private fun SafeClientEvent.updatePosRender(holeInfos: List<IntermediateHoleInfo>) {
        val sqRange = detectRange.value.sq
        val set = LongOpenHashSet()

        for (entity in EntityManager.players) {
            if (entity == player) continue
            if (!entity.isAlive) continue
            if (entity == player) continue
            if (entity.isFriend) continue
            if (player.squaredDistanceTo(entity) > sqRange) continue

            val current = entity.pos
            val predict = entity.calcPredict(current)

            for (holeInfo in holeInfos) {
                if (entity.y <= holeInfo.blockPos.y + 0.5) continue
                if (holeInfo.toward && holeInfo.playerDist - entity.horizontalDist(holeInfo.center) < distanceBalance.value) continue

                if (holeInfo.detectBox.contains(current)
                    || !holeInfo.toward
                    && (holeInfo.detectBox.contains(predict) || Box.raycast(
                        entity.blockStateAtPos.getCollisionShape(world, entity.blockPos).boundingBoxes,
                        current,
                        predict,
                        entity.blockPos
                    ) != null)
                ) {
                    set.add(holeInfo.blockPos.asLong())
                    renderBlockMap.putIfAbsent(holeInfo.blockPos, -1L)
                }
            }
        }

        renderBlockMap.runSynchronized {
            object2LongEntrySet().removeIf {
                it.longValue == -1L && !placeMap.containsKey(it.key.asLong()) && !set.contains(it.key.asLong())
            }
        }
    }

    private fun SafeClientEvent.getPos(holeInfos: List<IntermediateHoleInfo>, checkRotation: Boolean): BlockPos? {
        val sqRange = detectRange.value.sq

        val placeable = Object2FloatOpenHashMap<BlockPos>()

        for (entity in EntityManager.players) {
            if (entity == player) continue
            if (!entity.isAlive) continue
            if (entity == player) continue
            if (FriendManager.isFriend(entity)) continue
            if (player.squaredDistanceTo(entity) > sqRange) continue

            val current = entity.pos
            val predict = entity.calcPredict(current)

            for (holeInfo in holeInfos) {
                if (entity.y <= holeInfo.blockPos.y + 0.5) continue
                val dist = entity.horizontalDist(holeInfo.center)
                if (holeInfo.toward && holeInfo.playerDist - dist < distanceBalance.value) continue

                if (holeInfo.detectBox.contains(current)
                    || !holeInfo.toward
                    && (holeInfo.detectBox.contains(predict) || Box.raycast(
                        entity.blockStateAtPos.getCollisionShape(world, entity.blockPos).boundingBoxes,
                        current,
                        predict,
                        entity.blockPos
                    ) != null)
                ) {

                    placeable[holeInfo.blockPos] = dist.toFloat()
                    renderBlockMap.putIfAbsent(holeInfo.blockPos, -1L)
                }
            }
        }

        val eyePos = CrystalManager.position.add(0.0, player.getEyeHeight(player.pose).toDouble(), 0.0)

        val targetPos = placeable.object2FloatEntrySet().asSequence()
            .runIf(checkRotation) {
                filter {
                    Box(
                        it.key.x.toDouble(), it.key.y - 1.0, it.key.z.toDouble(),
                        it.key.x + 1.0, it.key.y.toDouble(), it.key.z + 1.0,
                    ).isInSight(eyePos, CrystalManager.rotation)
                }
            }
            .minByOrNull { it.floatValue }
            ?.key

        renderBlockMap.runSynchronized {
            object2LongEntrySet().removeIf {
                it.longValue == -1L && !placeMap.containsKey(it.key.asLong()) && !placeable.containsKey(it.key)
            }
        }

        return targetPos
    }

    private fun SafeClientEvent.getRotationPos(holeInfos: List<IntermediateHoleInfo>): BlockPos? {
        val sqRange = detectRange.value.sq

        var minDist = Double.MAX_VALUE
        var minDistPos: BlockPos? = null

        for (entity in EntityManager.players) {
            if (entity == player) continue
            if (!entity.isAlive) continue
            if (entity == player) continue
            if (FriendManager.isFriend(entity)) continue
            if (player.squaredDistanceTo(entity) > sqRange) continue

            val current = entity.pos
            val predict = entity.calcPredict(current)

            for (holeInfo in holeInfos) {
                if (entity.y <= holeInfo.blockPos.y + 0.5) continue

                val dist = entity.horizontalDist(holeInfo.center)
                if (dist >= minDist) continue
                if (holeInfo.toward && holeInfo.playerDist - dist < distanceBalance.value) continue

                if (holeInfo.detectBox.contains(current)
                    || !holeInfo.toward
                    && (holeInfo.detectBox.contains(predict) || Box.raycast(
                        entity.blockStateAtPos.getCollisionShape(world, entity.blockPos).boundingBoxes,
                        current,
                        predict,
                        entity.blockPos
                    ) != null)
                ) {

                    minDistPos = holeInfo.blockPos
                    minDist = dist
                }
            }
        }

        return minDistPos
    }

    private fun SafeClientEvent.getHoleInfos(): List<IntermediateHoleInfo> {
        val eyePos = player.eyePosition
        val rangeSq = fillRange.value.sq
        val entities = EntityManager.entity.filter {
            it.preventEntitySpawning && it.isAlive
        }

        return HoleManager.holeInfos.asSequence()
            .filterNot {
                it.isFullyTrapped
            }
            .filter {
                when (it.type) {
                    HoleType.BEDROCK -> bedrockHole.value
                    HoleType.OBBY -> obbyHole.value
                    HoleType.TWO -> twoBlocksHole.value
                    HoleType.FOUR -> fourBlocksHole.value
                    else -> false
                }
            }
            .filter { holeInfo ->
                holeInfo.holePos.any {
                    eyePos.distanceSqToCenter(it) <= rangeSq
                }
            }
            .filter { holeInfo ->
                entities.none {
                    it.boundingBox.intersects(holeInfo.boundingBox)
                }
            }
            .mapNotNull { holeInfo ->
                holeInfo.holePos.asSequence()
                    .filter { !placeMap.containsKey(it.asLong()) }
                    .minByOrNull { eyePos.distanceSqToCenter(it) }
                    ?.let {
                        val box = Box(
                            holeInfo.boundingBox.minX - hRange.value,
                            holeInfo.boundingBox.minY,
                            holeInfo.boundingBox.minZ - hRange.value,
                            holeInfo.boundingBox.maxX + hRange.value,
                            holeInfo.boundingBox.maxY + vRange.value,
                            holeInfo.boundingBox.maxZ + hRange.value
                        )

                        if (player.boundingBox.intersects(holeInfo.boundingBox)) {
                            null
                        } else {
                            val dist = player.horizontalDist(holeInfo.center)
                            val prevDist =
                                distance(player.lastRenderX, player.lastRenderZ, holeInfo.center.x, holeInfo.center.z)
                            IntermediateHoleInfo(
                                holeInfo.center,
                                it,
                                box,
                                dist,
                                dist - prevDist < -0.15
                            )
                        }
                    }
            }
            .toList()
    }

    private fun Entity.horizontalDist(vec3d: Vec3d): Double {
        return distance(this.x, this.z, vec3d.x, vec3d.z)
    }

    private fun Entity.calcPredict(current: Vec3d): Vec3d {
        return if (predictTicks.value == 0) {
            current
        } else {
            Vec3d(
                this.x + (this.x - this.lastRenderX) * predictTicks.value,
                this.y + (this.y - this.lastRenderY) * predictTicks.value,
                this.z + (this.z - this.lastRenderZ) * predictTicks.value
            )
        }
    }

    private fun SafeClientEvent.placeBlock(slot: HotbarSlot, pos: BlockPos) {
        onMainThread {
            if (rotation.value) {
                (nextHole ?: getRotationPos(holeInfos))?.let {
                    RotationManager.addRotations(it)
                }
            }
            player.spoofSneak {
                if (!switchBypass.value) {
                    spoofHotbar(slot) {
                        connection.sendPacket(fastPos(pos))
                    }
                } else {
                    spoofHotbarBypass(slot) {
                        connection.sendPacket(fastPos(pos))
                    }
                }
            }
            connection.sendPacket(HandSwingC2SPacket(Hand.MAIN_HAND))
        }

        placeMap[pos.asLong()] = System.currentTimeMillis() + fillTimeout.value
        placeTimer.reset()
    }

    private class IntermediateHoleInfo(
        val center: Vec3d,
        val blockPos: BlockPos,
        val detectBox: Box,
        val playerDist: Double,
        val toward: Boolean
    )
}