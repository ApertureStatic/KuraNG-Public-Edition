package dev.dyzjct.kura.module.modules.combat

import base.utils.TickTimer
import base.utils.TimeUnit
import base.utils.chat.ChatUtil
import base.utils.combat.CrystalUtils
import base.utils.concurrent.threads.onMainThread
import base.utils.concurrent.threads.onMainThreadSafe
import base.utils.concurrent.threads.runSafe
import base.utils.concurrent.threads.runSynchronized
import base.utils.entity.EntityUtils
import base.utils.entity.EntityUtils.preventEntitySpawning
import base.utils.entity.EntityUtils.spoofSneak
import base.utils.extension.fastPos
import base.utils.extension.sendSequencedPacket
import base.utils.extension.synchronized
import base.utils.hole.HoleType
import base.utils.hole.SurroundUtils.betterPosition
import base.utils.inventory.slot.firstBlock
import base.utils.inventory.slot.hotbarSlots
import base.utils.math.distanceSqTo
import base.utils.math.isInSight
import base.utils.math.vector.Vec2f
import base.utils.world.*
import dev.dyzjct.kura.event.eventbus.SafeClientEvent
import dev.dyzjct.kura.event.eventbus.listener
import dev.dyzjct.kura.event.eventbus.safeEventListener
import dev.dyzjct.kura.event.events.*
import dev.dyzjct.kura.event.events.block.BlockBreakEvent
import dev.dyzjct.kura.event.events.player.PlayerMoveEvent
import dev.dyzjct.kura.manager.CrystalManager
import dev.dyzjct.kura.manager.EntityManager
import dev.dyzjct.kura.manager.HoleManager
import dev.dyzjct.kura.manager.HotbarManager.serverSideItem
import dev.dyzjct.kura.manager.HotbarManager.spoofHotbar
import dev.dyzjct.kura.manager.HotbarManager.spoofHotbarWithSetting
import dev.dyzjct.kura.manager.RotationManager
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.module.modules.client.CombatSystem
import dev.dyzjct.kura.module.modules.crystal.CrystalHelper.realSpeed
import dev.dyzjct.kura.module.modules.movement.Step
import dev.dyzjct.kura.module.modules.player.PacketMine
import dev.dyzjct.kura.module.modules.render.PlaceRender
import dev.dyzjct.kura.system.util.collections.EnumMap
import dev.dyzjct.kura.utils.TimerUtils
import dev.dyzjct.kura.utils.extension.sq
import dev.dyzjct.kura.utils.inventory.HotbarSlot
import dev.dyzjct.kura.utils.rotation.RotationUtils.getRotationToVec2f
import it.unimi.dsi.fastutil.longs.Long2LongMaps
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import it.unimi.dsi.fastutil.longs.LongSets
import net.minecraft.block.Blocks
import net.minecraft.entity.decoration.EndCrystalEntity
import net.minecraft.item.ItemPlacementContext
import net.minecraft.item.Items
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket
import net.minecraft.sound.SoundCategory
import net.minecraft.text.Text
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d

object Surround : Module(
    name = "Surround",
    langName = "自动围脚",
    description = "Continually places obsidian around your feet",
    category = Category.COMBAT
) {
    private var placeDelay = isetting("PlaceDelay", 50, 0, 1000)
    private var multiPlace = isetting("MultiPlace", 2, 1, 5)
    private var groundCheck = bsetting("GroundCheck", true)
    private var autoCenter = bsetting("AutoCenter", true)
    private var rotation = bsetting("Rotation", false)
    private var checkRotation = bsetting("CheckRotation", false)
    private var enableInHole = bsetting("EnableInHole", false)
    private var inHoleTimeout = isetting("InHoleTimeOut", 50, 1, 100).isTrue(enableInHole)
    private var superSafe = bsetting("SuperSafe", false)
    private var attackCrystal = bsetting("AttackCrystal", false)
    private val placing = EnumMap<SurroundOffset, List<PlaceInfo>>().synchronized()
    private val placingSet = LongOpenHashSet()
    private val pendingPlacing = Long2LongMaps.synchronize(Long2LongOpenHashMap()).apply { defaultReturnValue(-1L) }
    private val placed = LongSets.synchronize(LongOpenHashSet())
    private val toggleTimer = TickTimer(TimeUnit.TICKS)
    private var placeTimer = TickTimer()
    private var safeTimer = TimerUtils()
    private var holePos: BlockPos? = null
    private var enableTicks = 0
    private var minePos: BlockPos? = null
    private var mineName: Text? = null

    override fun onDisable() {
        placeTimer.reset(-114514L)
        toggleTimer.reset()

        placing.clear()
        placingSet.clear()
        pendingPlacing.clear()
        placed.clear()

        holePos = null
        enableTicks = 0
    }

    override fun onEnable() {
        runSafe {
            if (autoCenter.value) {
                EntityUtils.autoCenter()
            }
        }
    }

    init {
        safeEventListener<CrystalSetDeadEvent> { event ->
            if (event.crystals.none { it.distanceSqTo(player.pos) < 6.0 }) return@safeEventListener
            var placeCount = 0

            placing.runSynchronized {
                val iterator = values.iterator()
                while (iterator.hasNext()) {
                    val list = iterator.next()
                    var allPlaced = true

                    loop@ for (placeInfo in list) {
                        if (event.crystals.none {
                                CrystalUtils.placeBoxIntersectsCrystalBox(
                                    placeInfo.placedPos, it
                                )
                            }) continue

                        val long = placeInfo.placedPos.asLong()
                        if (placed.contains(long)) continue
                        allPlaced = false

                        if (System.currentTimeMillis() <= pendingPlacing[long]) continue
                        if (!checkRotation(placeInfo)) continue

                        placeBlock(placeInfo)
                        placeCount++
                        if (placeCount >= multiPlace.value) return@safeEventListener
                    }

                    if (allPlaced) iterator.remove()
                }
            }
        }

        safeEventListener<WorldEvent.ServerBlockUpdate> { event ->
            val pos = event.pos
            if (!event.newState.isReplaceable) {
                val long = pos.asLong()
                if (placingSet.contains(long)) {
                    pendingPlacing.remove(long)
                    placed.add(long)
                }
            } else {
                val relative = pos.subtract(player.betterPosition)
                if (SurroundOffset.values().any { it.offset == relative } && checkColliding(pos)) {
                    if (safeTimer.tickAndReset(50L)) {
                        for (entity in EntityManager.entity) {
                            if (entity !is EndCrystalEntity) continue
                            if (!entity.boundingBox.intersects(Box(pos))) continue
                            if (entity.distanceSqTo(player.pos) > 5.sq) continue
                            if (!entity.isAlive || !attackCrystal.value) continue
                            connection.sendPacket(PlayerInteractEntityC2SPacket.attack(entity, false))
                            connection.sendPacket(HandSwingC2SPacket(Hand.MAIN_HAND))
                        }
                    }
                    getNeighbor(pos)?.let { placeInfo ->
                        if (checkRotation(placeInfo)) {
                            placingSet.add(placeInfo.placedPos.asLong())
                            placeBlock(placeInfo)
                        }
                    }
                }
            }
        }

        safeEventListener<TickEvent.Pre> {
            enableTicks++
        }

        listener<StepEvent> {
            placing.clear()
            placingSet.clear()
            pendingPlacing.clear()
            placed.clear()
            holePos = null
        }

        safeEventListener<RunGameLoopEvent.Tick>(true) {
            if (groundCheck.value) {
                if (!player.onGround) {
                    if (isEnabled) disable()
                    return@safeEventListener
                }
            }

            var playerPos = player.betterPosition
            val isInHole =
                player.onGround && player.realSpeed < 0.1 && HoleManager.getHoleInfo(playerPos).type == HoleType.OBBY

            if (isDisabled) {
                enableInHoleCheck(isInHole)
                return@safeEventListener
            }

            if (!world.getBlockState(playerPos.down())
                    .getCollisionShape(world, playerPos).isEmpty && world.getBlockState(playerPos.down())
                    .getCollisionShape(world, playerPos).boundingBox == null
            ) {
                playerPos = world.getGroundPos(player).up()
            }

            if (isInHole || holePos == null) {
                holePos = playerPos
            }

            updatePlacingMap(playerPos)

            if (placing.isNotEmpty() && placeTimer.tickAndReset(placeDelay.value)) {
                runPlacing()
            }

            minePos?.let { mine ->
                mineName?.let { name ->
                    if (player.name == name) return@safeEventListener
                    val slot = getSlot() ?: run {
                        disable()
                        return@safeEventListener
                    }
                    if (isInHole && superSafe.value) {
                        if (CombatSystem.eating && player.isUsingItem) return@let
                        for (face in Direction.entries) {
                            val safePos = mine.offset(face)
                            if (safePos == playerPos || !world.isAir(safePos)) continue
                            if (world.isPlaceable(safePos) && safeTimer.tickAndReset(placeDelay.value)) {
                                spoofHotbar(slot) {
                                    connection.sendPacket(fastPos(safePos))
                                }
                            }
                        }
                    }
                }
            }
        }

        safeEventListener<PlayerMoveEvent> {
            if (Step.isEnabled && EntityUtils.isMoving()) disable()
        }

        safeEventListener<BlockBreakEvent> { event ->
            minePos = event.blockPos
            world.getEntityById(event.breakerID)?.let {
                mineName = it.name
            }
        }
    }

    private fun enableInHoleCheck(isInHole: Boolean) {
        if (enableInHole.value && isInHole) {
            if (toggleTimer.tickAndReset(inHoleTimeout.value)) {
                enable()
            }
        } else {
            toggleTimer.reset()
        }
    }

    private fun SafeClientEvent.updatePlacingMap(playerPos: BlockPos) {
        pendingPlacing.runSynchronized {
            keys.removeIf {
                if (!world.getBlockState(BlockPos.fromLong(it)).isReplaceable) {
                    placed.add(it)
                    true
                } else {
                    false
                }
            }
        }

        if (placing.isEmpty() && (pendingPlacing.isEmpty() || pendingPlacing.runSynchronized { values.all { System.currentTimeMillis() > it } })) {
            placing.clear()
            placed.clear()
        }

        val tempPosition: MutableMap<BlockPos, SurroundOffset> = HashMap()

        for (surroundOffset in SurroundOffset.values()) {
            val offsetPos = playerPos.add(surroundOffset.offset)
            if (!world.getBlockState(offsetPos).isReplaceable) continue

            if (isEntityIntersecting(offsetPos)) {
                for (offset in SurroundOffset.values()) {
                    val extendedOffset = offsetPos.add(offset.offset)

                    if (extendedOffset == playerPos) {
                        continue
                    }
                    if (!isEntityIntersecting(extendedOffset)) {
                        tempPosition[extendedOffset] = offset
                    } else {
                        for (offsetTry in SurroundOffset.values()) {
                            val tryPos = extendedOffset.add(offsetTry.offset)
                            if (tryPos == playerPos) {
                                continue
                            }
                            if (isEntityIntersecting(tryPos)) continue
                            if (!world.isAir(tryPos)) continue
                            tempPosition[tryPos] = surroundOffset
                        }
                    }
                }
            } else {
                tempPosition[offsetPos] = surroundOffset
            }

        }

        tempPosition.forEach {
            getNeighborSequence(it.key, 2, 5.0f, false)?.let { list ->
                placing[it.value] = list
                list.forEach { placeInfo ->
                    placingSet.add(placeInfo.placedPos.asLong())
                }
            }
        }

    }

    private fun isEntityIntersecting(pos: BlockPos): Boolean {
        for (target in EntityManager.players) {
            if (target.boundingBox.intersects(Box(pos))) {
                return true
            }
        }

        return false
    }

    private fun SafeClientEvent.runPlacing() {
        var placeCount = 0

        placing.runSynchronized {
            val iterator = placing.values.iterator()
            while (iterator.hasNext()) {
                val list = iterator.next()
                var allPlaced = true
                loop@ for (placeInfo in list) {
                    val long = placeInfo.placedPos.asLong()
                    if (PacketMine.blockData?.blockPos == placeInfo.placedPos) continue
                    if (placed.contains(long)) continue
                    allPlaced = false

                    if (System.currentTimeMillis() <= pendingPlacing[long]) continue
                    if (!checkRotation(placeInfo)) continue

                    if (safeTimer.tickAndReset(50)) {
                        for (entity in EntityManager.entity) {
                            if (entity !is EndCrystalEntity) continue
                            if (!entity.isAlive) continue
                            if (!entity.boundingBox.intersects(Box(placeInfo.placedPos))) continue
                            if (!attackCrystal.value) continue
                            connection.sendPacket(PlayerInteractEntityC2SPacket.attack(entity, false))
                            connection.sendPacket(HandSwingC2SPacket(Hand.MAIN_HAND))
                            safeTimer.reset()
                        }
                    }
                    placeBlock(placeInfo)
                    placeCount++
                    if (placeCount >= multiPlace.value) return
                }

                if (allPlaced) iterator.remove()
            }
        }
    }

    private fun SafeClientEvent.getNeighbor(pos: BlockPos): PlaceInfo? {
        for (side in Direction.values()) {
            val offsetPos = pos.offset(side)
            val oppositeSide = side.opposite

            if (CombatSystem.strictDirection && !getVisibleSides(offsetPos, true).contains(oppositeSide)) continue
            if (world.getBlockState(offsetPos).isReplaceable) continue

            val hitVec = getHitVec(offsetPos, oppositeSide)
            val hitVecOffset = getHitVecOffset(oppositeSide)

            return PlaceInfo(offsetPos, oppositeSide, 0.0, hitVecOffset, hitVec, pos)
        }

        return null
    }

    private fun checkColliding(pos: BlockPos): Boolean {
        val box = Box(pos)

        return EntityManager.entity.none {
            it.isAlive && it.preventEntitySpawning && it.boundingBox.intersects(box)
        }
    }

    private fun SafeClientEvent.placeBlock(placeInfo: PlaceInfo) {
        val slot = getSlot() ?: run {
            disable()
            return
        }

        onMainThread {
            player.spoofSneak {
                if (rotation.value) {
                    var eyeHeight = player.getEyeHeight(player.pose)
                    if (!player.isSneaking) eyeHeight -= 0.08f
                    RotationManager.rotationTo(
                        getRotationToVec2f(
                            Vec3d(player.x, player.y + eyeHeight, player.z), placeInfo.hitVec
                        )
                    )
                }

                spoofHotbarWithSetting(Items.OBSIDIAN) {
                    sendSequencedPacket(world) {
                        placeInfo.toPlacePacket(Hand.MAIN_HAND, sequence = it)
                    }
                }

                connection.sendPacket(HandSwingC2SPacket(Hand.MAIN_HAND))
                PlaceRender.renderBlocks[placeInfo.placedPos] = System.currentTimeMillis()
            }
        }
        onMainThreadSafe {
            val blockState = Blocks.OBSIDIAN.getPlacementState(
                ItemPlacementContext(
                    world,
                    player,
                    Hand.MAIN_HAND,
                    player.serverSideItem,
                    BlockHitResult(placeInfo.hitVec, placeInfo.side, placeInfo.pos, false)
                )
            )
            blockState?.let {
                val soundType = blockState.block.getSoundGroup(blockState)
                world.playSound(
                    player,
                    placeInfo.pos,
                    soundType.placeSound,
                    SoundCategory.BLOCKS,
                    (soundType.getVolume() + 1.0f) / 2.0f,
                    soundType.getPitch() * 0.8f
                )
            }
        }

        pendingPlacing[placeInfo.placedPos.asLong()] = System.currentTimeMillis() + 50L
    }

    private fun SafeClientEvent.getSlot(): HotbarSlot? {
        val slot = player.hotbarSlots.firstBlock(Blocks.OBSIDIAN)

        return if (slot == null) {
            ChatUtil.sendMessage("No obsidian in hotbar!")
            null
        } else {
            slot
        }
    }

    private fun SafeClientEvent.checkRotation(placeInfo: PlaceInfo): Boolean {
        if (!checkRotation.value) return true
        var eyeHeight = player.getEyeHeight(player.pose)
        if (!player.isSneaking) eyeHeight -= 0.08f
        return !rotation.value || Box(placeInfo.pos).isInSight(
            CrystalManager.position.add(
                0.0, eyeHeight.toDouble(), 0.0
            ), Vec2f(player.yaw, player.pitch)
        )
    }

    private enum class SurroundOffset(val offset: BlockPos) {
        DOWN(BlockPos(0, -1, 0)), NORTH(BlockPos(0, 0, -1)), EAST(BlockPos(1, 0, 0)), SOUTH(BlockPos(0, 0, 1)), WEST(
            BlockPos(-1, 0, 0)
        )
    }
}