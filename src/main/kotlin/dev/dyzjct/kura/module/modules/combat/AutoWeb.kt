package dev.dyzjct.kura.module.modules.combat

import base.utils.block.BlockUtil.getNeighbor
import base.utils.chat.ChatUtil
import base.utils.combat.getPredictedTarget
import base.utils.combat.getTarget
import base.utils.concurrent.threads.onMainThread
import base.utils.concurrent.threads.runSafe
import base.utils.entity.EntityUtils.isInWeb
import base.utils.entity.EntityUtils.spoofSneak
import base.utils.extension.fastPos
import base.utils.hole.SurroundUtils
import base.utils.hole.SurroundUtils.checkHole
import base.utils.math.distanceSqTo
import base.utils.math.sq
import base.utils.math.toBlockPos
import base.utils.player.getTargetSpeed
import dev.dyzjct.kura.event.eventbus.SafeClientEvent
import dev.dyzjct.kura.manager.HotbarManager.spoofHotbarWithSetting
import dev.dyzjct.kura.manager.RotationManager
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.module.modules.client.CombatSystem
import dev.dyzjct.kura.module.modules.combat.HolePush.doHolePush
import dev.dyzjct.kura.module.modules.player.AntiMinePlace
import dev.dyzjct.kura.module.modules.player.PacketMine
import dev.dyzjct.kura.utils.TimerUtils
import net.minecraft.block.CobwebBlock
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Items
import net.minecraft.util.math.BlockPos

object AutoWeb : Module(
    name = "AutoWeb",
    langName = "自动蜘蛛网",
    category = Category.COMBAT,
    description = "Auto Place Web to stick target."
) {
    private var spoofRotations = bsetting("Rotate", false)
    private var holeCheck = bsetting("HoleCheck", true)
    private var betterPush = bsetting("BetterPush", true)
    private var betterAnchor = bsetting("BetterAnchor", true)
    private var facePlace = bsetting("FacePlace", false)
    private var multiPlace = bsetting("MultiPlace", true)
    private var webCheck = bsetting("WebCheck", true)
    private var ground = bsetting("OnlyGround", true)
    private var inside = bsetting("Inside", false)
    private var air = bsetting("AirPlace", false)
    private var smartDelay = bsetting("SmartDelay", false)
    private var delay = isetting("minDelay", 25, 0, 500)
    private var maxDelay = isetting("MaxDelay", 400, 0, 1000).isTrue(smartDelay)
    private var debug = bsetting("Debug", false)
    private var timerDelay = TimerUtils()

    var target: PlayerEntity? = null

    override fun onEnable() {
        runSafe {
            timerDelay.reset()
        }
    }

    override fun getHudInfo(): String {
        target?.let {
            return "${it.name.string} ${getTargetSpeed(it) > 20.0}"
        } ?: return "Waiting..."
    }

    init {
        onLoop {
            if (!player.isOnGround && ground.value) return@onLoop
            if (CombatSystem.eating && player.isUsingItem) return@onLoop
            if (!spoofHotbarWithSetting(Items.COBWEB, true) {}) {
                return@onLoop
            }
            target = getTarget(CombatSystem.targetRange)
            if (AnchorAura.isEnabled && AnchorAura.placeInfo != null && (!CombatSystem.smartAura || CombatSystem.isBestAura(
                    CombatSystem.AuraType.Anchor
                )) && betterAnchor.value
            ) return@onLoop
            target?.let {
                val targetDistance = getPredictedTarget(it, CombatSystem.predictTicks).blockPos
                if (doHolePush(
                        it.blockPos.up(),
                        check = true, test = true
                    ) != null && betterPush.value && HolePush.isEnabled
                ) return@onLoop
                fun SafeClientEvent.place(delay: Long) {
                    onPacket@ fun packet(pos: BlockPos, checkDown: Boolean = false) {
                        if (checkDown && world.getBlockState(pos.down()).block is CobwebBlock) return
                        PacketMine.blockData?.let { data ->
                            if (data.blockPos == pos) return@onPacket
                        }

                        AntiMinePlace.mineMap[pos]?.let { mine ->
                            if (System.currentTimeMillis() - mine.start >= mine.mine) return@onPacket
                        }

                        if (isInWeb(it) && webCheck.value) return

                        if (world.isAir(pos) && (getNeighbor(
                                pos
                            ) != null || air.value) && player.distanceSqTo(pos) < CombatSystem.placeRange.sq
                        ) {
                            if (timerDelay.tickAndReset(delay)) {
                                if (spoofRotations.value) {
                                    RotationManager.addRotations(pos)
                                }
                                spoofHotbarWithSetting(Items.COBWEB) {
                                    player.spoofSneak {
                                        connection.sendPacket(
                                            fastPos(pos)
                                        )
                                    }
                                }
                                if (debug.value) ChatUtil.sendMessage("Placing")
                            }
                        }
                    }

                    getNeighbor(targetDistance.up())?.let {
                        if (facePlace.value) {
                            packet(targetDistance.up(), true)
                        }
                    }

                    getNeighbor(targetDistance)?.let {
                        if (inside.value) {
                            packet(targetDistance)
                        }
                    }

                    getNeighbor(targetDistance.down())?.let {
                        packet(targetDistance.down())
                    }

                    if (multiPlace.value) {
                        packet(it.pos.add(0.3, 0.3, 0.3).toBlockPos())
                        packet(it.pos.add(-0.3, 0.3, -0.3).toBlockPos())
                        packet(it.pos.add(-0.3, 0.3, 0.3).toBlockPos())
                        packet(it.pos.add(0.3, 0.3, -0.3).toBlockPos())
                        packet(it.pos.add(0.3, 0.3, 0.3).toBlockPos().down())
                        packet(it.pos.add(-0.3, 0.3, -0.3).toBlockPos().down())
                        packet(it.pos.add(-0.3, 0.3, 0.3).toBlockPos().down())
                        packet(it.pos.add(0.3, 0.3, -0.3).toBlockPos().down())
                        if (facePlace.value) {
                            packet(it.pos.add(0.3, 0.3, 0.3).toBlockPos().up(), true)
                            packet(it.pos.add(-0.3, 0.3, -0.3).toBlockPos().up(), true)
                            packet(it.pos.add(-0.3, 0.3, 0.3).toBlockPos().up(), true)
                            packet(it.pos.add(0.3, 0.3, -0.3).toBlockPos().up(), true)
                        }
                    }
                }

                if (checkHole(it) != SurroundUtils.HoleType.NONE && it.onGround && holeCheck.value) return@onLoop

                val useDelay = if (smartDelay.value) {
                    if (getTargetSpeed(it) < 20.0) maxDelay.value else delay.value
                } else {
                    delay.value
                }


                onMainThread {
                    place(useDelay.toLong())
                }
            }
        }
    }
}