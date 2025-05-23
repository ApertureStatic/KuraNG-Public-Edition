package dev.dyzjct.kura.module.modules.aura

import base.utils.chat.ChatUtil
import base.utils.combat.ExposureSample
import base.utils.concurrent.threads.runSafe
import base.utils.graphics.ESPRenderer
import base.utils.math.distanceSqTo
import base.utils.math.toBox
import base.utils.math.toVec3dCenter
import dev.dyzjct.kura.event.eventbus.SafeClientEvent
import dev.dyzjct.kura.event.eventbus.safeEventListener
import dev.dyzjct.kura.event.events.player.UpdateWalkingPlayerEvent
import dev.dyzjct.kura.manager.CrystalManager
import dev.dyzjct.kura.manager.EntityManager
import dev.dyzjct.kura.manager.FriendManager.isFriend
import dev.dyzjct.kura.manager.HotbarManager.spoofHotbarWithSetting
import dev.dyzjct.kura.manager.RotationManager.packetRotate
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.module.modules.aura.DamageCalculator.crystalDamage
import dev.dyzjct.kura.module.modules.client.CombatSystem
import dev.dyzjct.kura.module.modules.client.CombatSystem.swing
import dev.dyzjct.kura.module.modules.crystal.CrystalHelper.canMove
import dev.dyzjct.kura.module.modules.crystal.CrystalHelper.scaledHealth
import dev.dyzjct.kura.utils.TimerUtils
import dev.dyzjct.kura.utils.animations.Easing
import dev.dyzjct.kura.utils.animations.sq
import dev.dyzjct.kura.utils.block.BlockUtil.canSee
import dev.dyzjct.kura.utils.block.BlockUtil.findDirection
import dev.dyzjct.kura.utils.block.aroundBlock
import dev.dyzjct.kura.utils.extension.sendSequencedPacket
import dev.dyzjct.kura.utils.inventory.InventoryUtil.findItemInHotbar
import dev.dyzjct.kura.utils.inventory.InventoryUtil.getWeaponSlot
import dev.dyzjct.kura.utils.isWeaknessActive
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.decoration.EndCrystalEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Items
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import java.awt.Color
import java.util.concurrent.CopyOnWriteArrayList

object KuraCrystal : Module(
    name = "KuraCrystal",
    langName = "KuraCrystal",
    description = "Strong Crystal Aura.",
    category = Category.AURA
) {
    private var page = msetting("Page", Page.GENERAL)

    //TODO Page GENERAL
    private var auto_switch by bsetting("AutoSwitch", false).enumIs(page, Page.GENERAL)
    private var spoof by bsetting("Spoof", true).enumIs(page, Page.GENERAL)
    private var place_delay by isetting("PlaceDelay", 45, 0, 1000).enumIs(page, Page.GENERAL)
    private var attack_delay by isetting("ExplodeDelay", 45, 0, 1000).enumIs(page, Page.GENERAL)
    private var anti_weak by bsetting("AntiWeak", true).enumIs(page, Page.GENERAL)

    //TODO Page Calculation
    private var place_range by dsetting("PlaceRange", 5.0, 0.0, 10.0).enumIs(page, Page.CALCULATION)
    private var attack_range by dsetting("ExplodeRange", 5.0, 0.0, 10.0).enumIs(page, Page.CALCULATION)
    private var place_min_dmg by dsetting("PlaceMinDmg", 4.0, 0.0, 36.0).enumIs(page, Page.CALCULATION)
    private var place_max_self by isetting("PlaceMaxSelfDmg", 10, 0, 36).enumIs(page, Page.CALCULATION)
    private var attack_min_dmg by dsetting("ExplodeMinDmg", 4.0, 0.0, 36.0).enumIs(page, Page.CALCULATION)
    private var attack_max_self by isetting("ExplodeMaxSelfDmg", 10, 0, 36).enumIs(page, Page.CALCULATION)
    private var balance by dsetting("Balance", 3.0, -5.0, 5.0).enumIs(page, Page.CALCULATION)
    private var noSuicide by bsetting("NoSuicide", true).enumIs(page, Page.CALCULATION)

    //TODO Page Rotate
    private var rotate by bsetting("Rotate", true).enumIs(page, Page.ROTATE)
    private val rotate_on_attack by bsetting("OnAttack", false).enumIs(page, Page.ROTATE)

    //TODO Page RENDER
    private var render_damage by bsetting("RenderDamage", true).enumIs(page, Page.RENDER)
    private var render_break by bsetting("RenderBreak", false).enumIs(page, Page.RENDER)
    private var break_alpha_fill by isetting("BreakAlpha", 50, 0, 255).enumIs(page, Page.RENDER).isTrue { render_break }
    private var break_alpha_outline by isetting("BreakLine", 200, 0, 255).enumIs(page, Page.RENDER)
        .isTrue { render_break }
    private var render_mode = msetting("RenderMode", RenderMode.Motion).enumIs(page, Page.RENDER)
    private var fill_color by
    csetting("FillColor", Color(20, 225, 219, 50)).enumIs(page, Page.RENDER)
    private var outline_color by
    csetting("LineColor", Color(20, 225, 219, 200)).enumIs(page, Page.RENDER)
    private val movingLength by isetting("MovingLength", 400, 0, 1000).enumIs(page, Page.RENDER)
    private val fadeLength by isetting("FadeLength", 200, 0, 1000).enumIs(page, Page.RENDER)

    //TODO Page Develop
    private var debug by bsetting("Debug", true).enumIs(page, Page.DEVELOP)

    private var place_list: List<PlaceInfo>? = null
    private var attack_list: List<EndCrystalEntity>? = null
    private var target_entity: Entity? = null

    private var directionVec: Vec3d? = null

    private var lastRenderPos: Vec3d? = null
    private var prevPos: Vec3d? = null
    private var currentPos: Vec3d? = null
    private var damage = 0.0

    private var attacking_position: Vec3d? = null

    private var lastUpdateTime = 0L
    private var startTime = 0L
    private var scale = 0.0f

    private var place_timer = TimerUtils()
    private var attack_timer = TimerUtils()

    private var prefix = "[${ChatUtil.DARK_PURPLE}KURA ${ChatUtil.DARK_RED}CRYSTAL${ChatUtil.WHITE}]: "

    init {
        safeEventListener<UpdateWalkingPlayerEvent.Post> {
            if (targetFinder().isEmpty()) {
                if (debug) ChatUtil.sendNoSpamMessage("${prefix}${ChatUtil.RED}Can't find the target entity!")
                attacking_position = null
                return@safeEventListener
            }
            targetFinder().first().let { target ->
                runCatching {
                    update(target)
                    place(target)
                    attack(target)
                }
                target_entity = target.entity
                place_list = null
                attack_list = null
            }
        }
        onRender3D { render3DEvent ->
            val renderer = ESPRenderer()
            scale = if (place_list != null) {
                Easing.OUT_CUBIC.inc(Easing.toDelta(startTime, fadeLength))
            } else {
                Easing.IN_CUBIC.dec(Easing.toDelta(startTime, fadeLength))
            }

            if (render_break) {
                attacking_position?.let { pos ->
                    renderer.aFilled = break_alpha_fill
                    renderer.aOutline = break_alpha_outline
                    renderer.add(pos.toBox(), fill_color, outline_color)
                    renderer.render(render3DEvent.matrices, false)
                }
            }

            prevPos?.let { prevPos ->
                currentPos?.let { currentPos ->
                    val multiplier = Easing.OUT_QUART.inc(Easing.toDelta(lastUpdateTime, movingLength))
                    val motionRenderPos =
                        prevPos.add(currentPos.subtract(prevPos).multiply(multiplier.toDouble()))
                    val staticRenderPos = currentPos

                    val finalPos = if (render_mode.value == RenderMode.Motion) motionRenderPos else staticRenderPos
                    val box = toRenderBox(finalPos, if (render_mode.value == RenderMode.Motion) scale else 1f)

                    renderer.aFilled = (fill_color.alpha * scale).toInt()
                    renderer.aOutline = (outline_color.alpha * scale).toInt()
                    renderer.add(box, fill_color, outline_color)
                    renderer.render(render3DEvent.matrices, false)
                    lastRenderPos = motionRenderPos
                }
            }

        }
    }

    override fun onDisable() {
        runSafe {
            prevPos = null
            currentPos = null
            lastRenderPos = null
        }
    }

    private fun SafeClientEvent.update(target: TargetInfo) {
        place_list = findPlaceList(target)
        attack_list = findCrystalsList(target)
        currentPos = place_list?.first()?.crystalPos?.down()?.toCenterPos() ?: currentPos
        prevPos = lastRenderPos ?: currentPos
        lastUpdateTime = System.currentTimeMillis()
        startTime = System.currentTimeMillis()
    }

    private fun SafeClientEvent.place(target: TargetInfo) {
        if (place_list?.isEmpty() != false) return
        place_list?.let { placeList ->
            val placePos = placeList.first()
            val found = attack_list?.isNotEmpty() ?: false
            if (!found || (placePos.targetDmg > crystalDamage(
                    target.entity as LivingEntity,
                    target.currentPos,
                    target.box,
                    attack_list!!.first().blockPos
                ))
            ) {
                currentPos = placePos.crystalPos.down().toCenterPos() ?: currentPos
                prevPos = lastRenderPos ?: currentPos
                lastUpdateTime = System.currentTimeMillis()
                startTime = System.currentTimeMillis()
                if (rotate) {
                    packetRotate(placePos.crystalPos)
                }
                if (place_timer.tickAndReset(place_delay)) {
                    if (auto_switch) findItemInHotbar(Items.END_CRYSTAL)?.let { slot ->
                        player.inventory.selectedSlot = slot
                    }
                    if (spoof) {
                        spoofHotbarWithSetting(Items.END_CRYSTAL) {
                            sendSequencedPacket(world) {
                                placePacket(placePos, Hand.MAIN_HAND, it)
                            }
                            swing()
                        }
                    } else if (player.inventory.mainHandStack.item == Items.END_CRYSTAL || player.offHandStack.item == Items.END_CRYSTAL) {
                        sendSequencedPacket(world) {
                            placePacket(placePos, Hand.MAIN_HAND, it)
                        }
                        swing()
                    }
                }
            }
        }
    }

    private fun SafeClientEvent.attack(target: TargetInfo) {
        if (findCrystalsList(target).none()) {
            attacking_position = null
            return
        }
        val foundCrystal = findCrystalsList(target).first()
        if (rotate && rotate_on_attack) {
            packetRotate(foundCrystal.pos)
        }
        if (attack_timer.tickAndReset(attack_delay)) {
            attacking_position = foundCrystal.pos.add(-0.5, -1.0, -0.5)
            if (anti_weak && player.isWeaknessActive()) {
                getWeaponSlot()?.let { stack ->
                    spoofHotbarWithSetting(stack.item) {
                        connection.sendPacket(PlayerInteractEntityC2SPacket.attack(foundCrystal, player.isSneaking))
                        swing()
                    }
                }

            } else {
                connection.sendPacket(PlayerInteractEntityC2SPacket.attack(foundCrystal, player.isSneaking))
                swing()
            }
        }
    }

    private fun SafeClientEvent.findPlaceList(target: TargetInfo): List<PlaceInfo> {
        val list = CopyOnWriteArrayList<PlaceInfo>()
        val blocks = player.blockPos.aroundBlock(10)
            .filter { player.squaredDistanceTo(it.toCenterPos()) <= place_range.sq }
            .filter { canPlaceCrystal(it) }

        for (block in blocks) {
            val crystalPos = block.up()
            val selfDamage = crystalDamage(
                player,
                player.pos,
                player.boundingBox,
                crystalPos
            )
            if (selfDamage <= place_max_self) {
                if (noSuicide && selfDamage >= player.health + player.absorptionAmount + 5) continue

                if (!world.isAir(crystalPos)) continue

                val tgtDmg =
                    crystalDamage(
                        target.entity as LivingEntity,
                        target.currentPos,
                        target.box,
                        crystalPos
                    )

                if (tgtDmg < place_min_dmg) continue

                if (selfDamage > place_max_self) continue
                if (tgtDmg - selfDamage < balance) continue
                if (selfDamage >= player.health + player.absorptionAmount) continue
                list.add(
                    PlaceInfo(
                        target.entity,
                        crystalPos,
                        tgtDmg,
                        selfDamage,
                        if (CombatSystem.strictDirection) findDirection(crystalPos)
                            ?: Direction.UP else Direction.UP
                    )
                )
            }
        }
        list.sortByDescending { it.targetDmg }
        return list
    }

    private fun SafeClientEvent.findCrystalsList(target: TargetInfo): List<EndCrystalEntity> {
        val list = CopyOnWriteArrayList<EndCrystalEntity>()
        for (crystal in world.entities.filterIsInstance<EndCrystalEntity>()) {
            if (player.distanceSqTo(crystal.pos) > attack_range.sq) continue
            if (crystalDamage(
                    target.entity as LivingEntity,
                    target.currentPos,
                    target.box,
                    crystal.blockPos
                ) < attack_min_dmg
            ) continue
            if (crystalDamage(
                    player,
                    player.pos,
                    player.boundingBox,
                    crystal.blockPos
                ) >= attack_max_self
            ) continue
            if (!noSuicide || crystalDamage(
                    player,
                    player.pos,
                    player.boundingBox,
                    crystal.blockPos
                ) >= player.health + player.absorptionAmount
            ) continue
            list.add(crystal)
        }
        list.sortedByDescending {
            crystalDamage(
                target.entity as LivingEntity,
                target.currentPos,
                target.box,
                it.blockPos
            )
        }
        if (debug) ChatUtil.sendNoSpamMessage("${prefix}${ChatUtil.GREEN}success to return crystal list.")
        return list
    }


    private fun SafeClientEvent.targetFinder(): List<TargetInfo> {
        val list = ObjectArrayList<TargetInfo>()
        val eyePos = CrystalManager.eyePosition

        for (target in EntityManager.players) {
            if (target == player) continue
            if (!target.isAlive) continue
            if (target.distanceSqTo(eyePos) > CombatSystem.targetRange.sq) continue
            if (isFriend(target.name.string)) continue

            list.add(getPredictedTarget(target, CombatSystem.predictTicks))
        }

        return list.asSequence().filter { it.entity.isAlive }
            .sortedWith(compareByDescending<TargetInfo> { (it.entity as LivingEntity).scaledHealth }.thenBy {
                player.distanceSqTo(
                    it.predictMotion
                )
            }).take(CombatSystem.maxTargets).toList()
    }

    private fun SafeClientEvent.getPredictedTarget(entity: PlayerEntity, ticks: Int): TargetInfo {
        val entityBox = entity.boundingBox
        var targetBox = entityBox

        for (tick in 0..ticks) {
            targetBox =
                canMove(
                    targetBox,
                    (entity.x - entity.lastRenderX).coerceIn(-0.6, 0.6),
                    (entity.y - entity.lastRenderY).coerceIn(-0.5, 0.5),
                    (entity.z - entity.lastRenderZ).coerceIn(-0.6, 0.6)
                ) ?: canMove(
                    targetBox,
                    (entity.x - entity.lastRenderX).coerceIn(-0.6, 0.6),
                    0.0,
                    (entity.z - entity.lastRenderZ).coerceIn(-0.6, 0.6)
                ) ?: canMove(
                    targetBox, 0.0, (entity.y - entity.lastRenderY).coerceIn(-0.5, 0.5), 0.0
                ) ?: break
        }
        val motion =
            Vec3d(targetBox.minX - entityBox.minX, targetBox.minY - entityBox.minY, targetBox.minZ - entityBox.minZ)
        val pos = entity.pos

        return TargetInfo(
            entity,
            pos.add(motion),
            targetBox,
            pos,
            motion,
            ExposureSample.getExposureSample(entity.width, entity.height)
        )
    }

    private fun SafeClientEvent.placePacket(
        placeInfo: PlaceInfo,
        hand: Hand,
        sequence: Int
    ): PlayerInteractBlockC2SPacket {
        return PlayerInteractBlockC2SPacket(
            hand,
            BlockHitResult(
                placeInfo.crystalPos.down().toVec3dCenter(),
                placeInfo.direction,
                placeInfo.crystalPos.down(),
                false
            ),
            sequence
        )
    }

    private fun SafeClientEvent.canPlaceCrystal(pos: BlockPos): Boolean {
        for (target in world.entities) {
            if (target is EndCrystalEntity) continue
            if (getCrystalPlacingBB(pos).intersects(target.boundingBox)) return false
        }
        if (CombatSystem.oldVersion && player.distanceSqTo(pos.up()) > CombatSystem.wallRange.sq && !canSee(
                pos.x.toDouble(),
                pos.y.toDouble(),
                pos.z.toDouble()
            )
        ) return false
        return canPlaceCrystalOn(pos) && hasValidSpaceForCrystal(pos, CombatSystem.oldVersion)
    }

    private fun getCrystalPlacingBB(pos: BlockPos): Box = getCrystalPlacingBB(pos.x, pos.y, pos.z)

    private fun getCrystalPlacingBB(x: Int, y: Int, z: Int): Box =
        Box(
            x + 0.0001, y + 1.0, z + 0.0001,
            x + 0.9999, y + 3.0, z + 0.9999
        )

    private fun SafeClientEvent.canPlaceCrystalOn(pos: BlockPos): Boolean {
        val block = world.getBlockState(pos).block
        return block == Blocks.BEDROCK || block == Blocks.OBSIDIAN
    }

    private fun SafeClientEvent.hasValidSpaceForCrystal(pos: BlockPos, biggerCrystal: Boolean): Boolean {
        val mutableBlockPos = BlockPos.Mutable()
        return if (!biggerCrystal) {
            (isValidMaterial(
                world.getBlockState(mutableBlockPos.set(pos).add(0, 1, 0))
            )
                    && isValidMaterial(world.getBlockState(mutableBlockPos.add(0, 1, 0))))
        } else {
            (isValidMaterial(world.getBlockState(mutableBlockPos.set(pos).add(0, 1, 0)))
                    && isValidMaterial(world.getBlockState(mutableBlockPos.add(0, 1, 0)))
                    && isValidMaterial(world.getBlockState(mutableBlockPos.set(pos).add(0, 2, 0)))
                    && isValidMaterial(world.getBlockState(mutableBlockPos.add(0, 2, 0))))
        }
    }

    @Suppress("DEPRECATION")
    private fun isValidMaterial(blockState: BlockState): Boolean {
        return !blockState.isLiquid && blockState.isReplaceable
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

    data class PlaceInfo(
        val target: LivingEntity,
        val crystalPos: BlockPos,
        val targetDmg: Double,
        val selfDmg: Double,
        val direction: Direction
    )

    data class TargetInfo(
        val entity: Entity,
        val pos: Vec3d,
        val box: Box,
        val currentPos: Vec3d,
        val predictMotion: Vec3d,
        val exposureSample: ExposureSample
    )

    @Suppress("UNUSED")
    enum class RenderMode {
        Normal, Motion, Fade
    }

    enum class Page {
        GENERAL, CALCULATION, ROTATE, RENDER, DEVELOP
    }
}