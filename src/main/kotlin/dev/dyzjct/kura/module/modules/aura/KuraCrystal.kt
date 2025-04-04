package dev.dyzjct.kura.module.modules.aura

import base.utils.combat.ExposureSample
import base.utils.math.distanceSqTo
import dev.dyzjct.kura.event.eventbus.SafeClientEvent
import dev.dyzjct.kura.manager.CrystalManager
import dev.dyzjct.kura.manager.EntityManager
import dev.dyzjct.kura.manager.FriendManager.isFriend
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.module.modules.client.CombatSystem
import dev.dyzjct.kura.module.modules.client.UiSetting
import dev.dyzjct.kura.module.modules.client.UiSetting.theme
import dev.dyzjct.kura.module.modules.crystal.CrystalHelper.canMove
import dev.dyzjct.kura.module.modules.crystal.CrystalHelper.scaledHealth
import dev.dyzjct.kura.utils.animations.sq
import dev.dyzjct.kura.utils.extension.synchronized
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import java.awt.Color

object KuraCrystal : Module(
    name = "KuraCrystal",
    langName = "KuraCrystal",
    description = "Strong Crystal Aura.",
    category = Category.AURA
) {
    private var page = msetting("Page", Page.GENERAL)

    //TODO Page GENERAL
    private var ghost_hand by bsetting("GhostHand", true).enumIs(page, Page.GENERAL)
    private var rotate by bsetting("Rotate", true).enumIs(page, Page.GENERAL)
    private var packet_place by bsetting("PacketPlace", true).enumIs(page, Page.GENERAL)
    private var place_delay by isetting("PlaceDelay", 45, 0, 1000).enumIs(page, Page.GENERAL)
    private var attack_delay by isetting("ExplodeDelay", 45, 0, 1000).enumIs(page, Page.GENERAL)

    //TODO Page Calculation
    private var place_min_dmg by dsetting("PlaceMinDmg", 4.0, 0.0, 36.0).enumIs(page, Page.CALCULATION)
    private var place_max_self by isetting("PlaceMaxSelfDmg", 10, 0, 36).enumIs(page, Page.CALCULATION)
    private var attack_min_dmg by dsetting("ExplodeMinDmg", 4.0, 0.0, 36.0).enumIs(page, Page.CALCULATION)
    private var attack_max_self by isetting("ExplodeMaxSelfDmg", 10, 0, 36).enumIs(page, Page.CALCULATION)
    private var noSuicide by fsetting("NoSuicide", 2f, 0f, 20f).enumIs(page, Page.CALCULATION)

    //TODO RENDER
    private var renderDamage by bsetting("RenderDamage", true).enumIs(page, Page.RENDER)
    private var fillColor by
    csetting("FillColor", Color(20, 225, 219, 50)).enumIs(page, Page.RENDER)
    private var outlineColor by
    csetting("LineColor", Color(20, 225, 219, 200)).enumIs(page, Page.RENDER)
        .isTrue { theme == UiSetting.Theme.Custom }

    //TODO Page Develop
    private var debug by bsetting("Debug", true).enumIs(page, Page.DEVELOP)

    private val target_entity: PlayerEntity? = null

    init {
        onMotion {
            targetFinder().first().let { target ->

            }
        }
    }

    private fun SafeClientEvent.targetFinder(): Sequence<TargetInfo> {
        val list = ObjectArrayList<TargetInfo>().synchronized()
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
            }).take(CombatSystem.maxTargets)
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
        val motion = Vec3d(targetBox.minX - entityBox.minX, targetBox.minY - entityBox.minY, targetBox.minZ - entityBox.minZ)
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

    data class TargetInfo(
        val entity: Entity,
        val pos: Vec3d,
        val box: Box,
        val currentPos: Vec3d,
        val predictMotion: Vec3d,
        val exposureSample: ExposureSample
    )

    enum class Page {
        GENERAL, CALCULATION, RENDER, DEVELOP
    }
}