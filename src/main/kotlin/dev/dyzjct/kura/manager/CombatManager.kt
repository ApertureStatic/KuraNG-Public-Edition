package dev.dyzjct.kura.manager

import com.google.common.collect.MapMaker
import dev.dyzjct.kura.module.modules.crystal.CrystalDamage
import dev.dyzjct.kura.module.modules.crystal.CrystalHelper.setAndAdd
import dev.dyzjct.kura.module.modules.crystal.KuraAura
import dev.dyzjct.kura.utils.animations.fastFloor
import it.unimi.dsi.fastutil.ints.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import base.events.*
import base.events.entity.EntityEvent
import base.system.event.*
import base.utils.TickTimer
import base.utils.combat.CalcContext
import base.utils.combat.CrystalUtils.canPlaceCrystal
import base.utils.combat.DamageReduction
import base.utils.combat.MotionTracker
import base.utils.concurrent.threads.defaultScope
import base.utils.concurrent.threads.isActiveOrFalse
import base.utils.concurrent.threads.onMainThreadSafe
import base.utils.entity.EntityUtils.eyePosition
import base.utils.player.updateController
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.decoration.EndCrystalEntity
import net.minecraft.network.packet.s2c.play.*
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import base.utils.math.distanceSqTo
import base.utils.math.toBlockPos
import base.utils.math.toVec3d
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.sqrt

object CombatManager : AlwaysListening {

    private val damageReductionTimer = TickTimer()
    private val damageReductions = MapMaker().weakKeys().makeMap<LivingEntity, DamageReduction>()
    private val hurtTimeMap = Int2LongMaps.synchronize(Int2LongOpenHashMap()).apply { defaultReturnValue(-1L) }
    private val healthMap = Int2FloatMaps.synchronize(Int2FloatOpenHashMap()).apply { defaultReturnValue(Float.NaN) }

    var target: LivingEntity? = null
        get() {
            if (field?.isAlive == false) {
                field = null
            }

            return field
        }
        set(value) {
            if (value != field) {
                CombatEvent.UpdateTarget(field, value).post()
                field = value
            }
        }
    var targetList = emptySet<LivingEntity>()

    var trackerSelf: MotionTracker? = null; private set
    var trackerTarget: MotionTracker? = null

    var contextSelf: CalcContext? = null; private set
    var contextTarget: CalcContext? = null; private set

    private val crystalTimer = TickTimer()
    private val removeTimer = TickTimer()

    private var placeJob: Job? = null
    private var crystalJob: Job? = null

    private val placeMap0 = ConcurrentHashMap<BlockPos, CrystalDamage>()
    private val crystalMap0 = MapMaker().weakKeys().makeMap<EndCrystalEntity, CrystalDamage>()
    val placeMap: Map<BlockPos, CrystalDamage>
        get() = placeMap0
    val crystalMap: Map<EndCrystalEntity, CrystalDamage>
        get() = crystalMap0

    var placeList = emptyList<CrystalDamage>(); private set
    var crystalList = emptyList<Pair<EndCrystalEntity, CrystalDamage>>(); private set

    private const val PLACE_RANGE = 6
    private const val PLACE_RANGE_SQ = 36.0
    private const val CRYSTAL_RANGE_SQ = 144.0

    private const val MAX_RANGE_SQ = 256.0

    fun onInit() {
        safeEventListener<PacketEvents.Receive>(114514) { event ->
            when (event.packet) {
                is PlaySoundS2CPacket -> {
                    if (event.packet.category != SoundCategory.BLOCKS) return@safeEventListener
                    if (event.packet.sound != SoundEvents.ENTITY_GENERIC_EXPLODE) return@safeEventListener
                    val list = crystalList.asSequence()
                        .map(Pair<EndCrystalEntity, CrystalDamage>::first)
                        .filter { it.distanceSqTo(event.packet.x, event.packet.y, event.packet.z) <= 144.0 }
                        .onEach(EndCrystalEntity::kill)
                        .also { e -> e.forEach { it.setRemoved(Entity.RemovalReason.KILLED) } }
                        .toList()

                    if (list.isNotEmpty()) {
                        onMainThreadSafe {
                            list.forEach {
                                world.removeEntity(it.id, Entity.RemovalReason.DISCARDED)
                            }
                        }
                    }
                    CrystalSetDeadEvent(event.packet.x, event.packet.y, event.packet.z, list).post()
                }

                is EntityAnimationS2CPacket -> {
                    if (event.packet.animationId == 1) {
                        val entityID = event.packet.id
                        val time = System.currentTimeMillis()
                        hurtTimeMap[entityID] = time
                    }
                }

                is EntityStatusS2CPacket -> {
                    when (event.packet.status.toInt()) {
                        3 -> {
                            (event.packet.getEntity(world) as? LivingEntity)?.let {
                                EntityEvent.Death(it).post()
                            }
                            if (event.packet.id == target?.id) {
                                target = null
                            }
                        }

                        2, 33, 36, 37 -> {
                            hurtTimeMap[event.packet.id] = System.currentTimeMillis()
                        }
                    }
                }

                is EntitiesDestroyS2CPacket -> {
                    event.packet.entityIds.forEach {
                        if (it == target?.id) target = null
                        hurtTimeMap.remove(it)
                        healthMap.remove(it)
                    }
                }
            }
        }

        listener<ConnectionEvent.Disconnect> {
            damageReductions.clear()
            hurtTimeMap.clear()
            healthMap.clear()

            target = null
            targetList = emptySet()

            trackerSelf = null
            trackerTarget = null

            contextSelf = null
            contextTarget = null

            placeMap0.clear()
            crystalMap0.clear()

            placeList = emptyList()
            crystalList = emptyList()
        }

        safeEventListener<WorldEvent.Entity.Add> { event ->
            when (event.entity) {
                is ClientPlayerEntity -> {
                    damageReductions[event.entity] = DamageReduction(event.entity)
                }

                is EndCrystalEntity -> {
                    val distSq = event.entity.distanceSqTo(player.eyePosition)
                    if (distSq > CRYSTAL_RANGE_SQ) return@safeEventListener
                    val crystalPos = BlockPos(
                        event.entity.x.fastFloor(),
                        event.entity.y.fastFloor() - 1,
                        event.entity.z.fastFloor()
                    )
                    getCrystalDamage(crystalPos)?.let {
                        CrystalSpawnEvent(event.entity.id, it).post()
                    }
                    val contextSelf = contextSelf ?: return@safeEventListener
                    val contextTarget = contextTarget

                    defaultScope.launch {
                        val blockPos = event.entity.blockPos
                        val mutableBlockPos = BlockPos.Mutable()
                        val crystalDamage = placeMap0.computeIfAbsent(blockPos) {
                            calculateDamage(
                                contextSelf,
                                contextTarget,
                                mutableBlockPos,
                                blockPos.toVec3d(0.5, 1.0, 0.5),
                                blockPos,
                                sqrt(distSq)
                            )
                        }

                        crystalMap0[event.entity] = crystalDamage
                    }
                }
            }
        }

        listener<WorldEvent.Entity.Remove> {
            when (it.entity) {
                is LivingEntity -> {
                    damageReductions.remove(it.entity)
                }

                is EndCrystalEntity -> {
                    crystalMap0.remove(it.entity)
                }
            }

            if (it.entity === target) target = null
            hurtTimeMap.remove(it.entity.id)
            healthMap.remove(it.entity.id)
        }

        safeParallelListener<TickEvent.Post> {
            val trackerSelf = trackerSelf?.takeIf { it.entity === player }
                ?: MotionTracker(player)

            trackerSelf.tick()
            CombatManager.trackerSelf = trackerSelf

            trackerTarget?.tick()
        }

        safeEventListener<RunGameLoopEvent.Tick>(Int.MAX_VALUE) {
            val flag1 = damageReductionTimer.tickAndReset(2)
            val flag2 = crystalTimer.tickAndReset(2)

            if (flag1 || flag2) {
                playerController.updateController()

                if (flag1) {
                    EntityManager.players.forEach {
                        damageReductions[it] = DamageReduction(it)
                    }
                    target?.let {
                        damageReductions[it] = DamageReduction(it)
                    }
                }

                if (flag2) {
                    updateCrystalDamage()
                }
            }
        }
    }

    fun getCrystalDamage(crystal: EndCrystalEntity) =
        crystalMap0[crystal] ?: getCrystalDamage(crystal.blockPos)

    fun getCrystalDamage(blockPos: BlockPos) =
        contextSelf?.let { contextSelf ->
            placeMap0.computeIfAbsent(blockPos) {
                val crystalPos = blockPos.toVec3d(0.5, 1.0, 0.5)
                val dist = contextSelf.entity.eyePosition.distanceTo(crystalPos)
                calculateDamage(contextSelf, contextTarget, BlockPos.Mutable(), crystalPos, it, dist)
            }
        }

    fun getDamageReduction(entity: LivingEntity) =
        damageReductions[entity]

    fun getHurtTime(entity: LivingEntity): Long {
        synchronized(hurtTimeMap) {
            var hurtTime = hurtTimeMap[entity.id]

            if (hurtTime == -1L) {
                val hurtTimeTicks = entity.hurtTime
                if (hurtTimeTicks != 0) {
                    hurtTime = System.currentTimeMillis() - hurtTimeTicks * 50L
                    hurtTimeMap[entity.id] = hurtTime
                }
            }

            return hurtTime
        }
    }

    private fun SafeClientEvent.updateCrystalDamage() {
        val flag1 = !placeJob.isActiveOrFalse
        val flag2 = !crystalJob.isActiveOrFalse

        if (flag1 || flag2) {
            val predictPosSelf = trackerSelf?.calcPosAhead(KuraAura.ownPredictTicks.value) ?: player.pos
            val contextSelf = CalcContext(this, player, predictPosSelf)

            val target = target
            val contextTarget = target?.let {
                val predictPos = trackerTarget?.calcPosAhead(8) ?: it.pos
                CalcContext(this, it, predictPos)
            }

            val remove = removeTimer.tickAndReset(100)

            CombatManager.contextSelf = contextSelf
            CombatManager.contextTarget = contextTarget

            if (flag1) {
                placeJob = defaultScope.launch {
                    updatePlaceMap(contextSelf, contextTarget, remove)
                    updatePlaceList()
                }
            }
            if (flag2) {
                crystalJob = defaultScope.launch {
                    updateCrystalMap(contextSelf, contextTarget, remove)
                    updateCrystalList()
                }
            }
        }

        damageReductionTimer.reset(2 / -4)
    }

    private fun SafeClientEvent.updatePlaceMap(contextSelf: CalcContext, contextTarget: CalcContext?, remove: Boolean) {
        val eyePos = player.eyePosition
        val flooredPos = player.pos
        val mutableBlockPos = BlockPos.Mutable()

        placeMap0.values.removeIf { crystalDamage ->
            remove && (crystalDamage.crystalPos.squaredDistanceTo(eyePos) > MAX_RANGE_SQ
                    || !canPlaceCrystal(crystalDamage.blockPos, null)
                    || contextTarget != null
                    && (crystalDamage.crystalPos.squaredDistanceTo(contextTarget.predictPos) > MAX_RANGE_SQ
                    || !contextTarget.checkColliding(crystalDamage.crystalPos)))
        }

        placeMap0.replaceAll { blockPos, crystalDamage ->
            calculateDamage(
                contextSelf,
                contextTarget,
                mutableBlockPos,
                blockPos.toVec3d(0.5, 1.0, 0.5),
                blockPos,
                eyePos.distanceTo(crystalDamage.crystalPos)
            )
        }

        val blockPos = BlockPos.Mutable()

        for (x in -PLACE_RANGE..PLACE_RANGE) {
            for (y in -PLACE_RANGE..PLACE_RANGE) {
                for (z in -PLACE_RANGE..PLACE_RANGE) {
                    blockPos.setAndAdd(flooredPos.toBlockPos(), x, y, z)
                    if (blockPos.y !in 0..255) continue

                    val crystalX = blockPos.x + 0.5
                    val crystalY = blockPos.y + 1.0
                    val crystalZ = blockPos.z + 0.5

                    val distSq = eyePos.distanceSqTo(crystalX, crystalY, crystalZ)
                    if (distSq > PLACE_RANGE_SQ) continue
                    if (placeMap0.containsKey(blockPos)) continue
                    if (!canPlaceCrystal(blockPos, null)) continue

                    val crystalPos = Vec3d(crystalX, crystalY, crystalZ)
                    if (contextTarget != null) {
                        if (contextTarget.predictPos.squaredDistanceTo(crystalPos) > CRYSTAL_RANGE_SQ) continue
                        if (!contextTarget.checkColliding(crystalPos)) continue
                    }

                    val immutablePos = blockPos.toImmutable()
                    placeMap0[immutablePos] = calculateDamage(
                        contextSelf,
                        contextTarget,
                        mutableBlockPos,
                        crystalPos,
                        immutablePos,
                        sqrt(distSq)
                    )
                }
            }
        }
    }

    private fun SafeClientEvent.updateCrystalMap(
        contextSelf: CalcContext,
        contextTarget: CalcContext?,
        remove: Boolean
    ) {
        val eyePos = player.eyePosition
        val mutableBlockPos = BlockPos.Mutable()

        if (remove) {
            crystalMap0.keys.removeIf {
                it.distanceSqTo(eyePos) > MAX_RANGE_SQ
            }
        }

        crystalMap0.replaceAll { _, crystalDamage ->
            placeMap0.computeIfAbsent(crystalDamage.blockPos) {
                calculateDamage(
                    contextSelf,
                    contextTarget,
                    mutableBlockPos,
                    it.toVec3d(0.5, 1.0, 0.5),
                    it,
                    eyePos.distanceTo(crystalDamage.crystalPos)
                )
            }
        }

        for (entity in EntityManager.entity) {
            if (!entity.isAlive) continue
            if (entity !is EndCrystalEntity) continue

            val distSq = entity.distanceSqTo(eyePos)
            if (distSq > CRYSTAL_RANGE_SQ) continue

            crystalMap0.computeIfAbsent(entity) {
                placeMap0.computeIfAbsent(entity.blockPos) {
                    calculateDamage(
                        contextSelf,
                        contextTarget,
                        mutableBlockPos,
                        it.toVec3d(0.5, 1.0, 0.5),
                        it,
                        sqrt(distSq)
                    )
                }
            }
        }
    }

    private fun calculateDamage(
        contextSelf: CalcContext,
        contextTarget: CalcContext?,
        mutableBlockPos: BlockPos.Mutable,
        crystalPos: Vec3d,
        blockPos: BlockPos,
        distance: Double
    ): CrystalDamage {
        val selfDamage = max(
            contextSelf.calcDamage(crystalPos, true, mutableBlockPos),
            contextSelf.calcDamage(crystalPos, false, mutableBlockPos)
        )
        val targetDamage = contextTarget?.calcDamage(crystalPos, true, mutableBlockPos) ?: 0.0f
        return CrystalDamage(
            crystalPos,
            blockPos,
            selfDamage,
            targetDamage,
            distance,
            contextSelf.currentPos.distanceTo(crystalPos)
        )
    }

    private fun updatePlaceList() {
        placeList = placeMap.values
            .sortedByDescending { it.targetDamage }
    }

    private fun updateCrystalList() {
        crystalList = crystalMap.entries
            .map { it.toPair() }
            .sortedByDescending { it.second.targetDamage }
    }
}