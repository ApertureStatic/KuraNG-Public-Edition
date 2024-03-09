package dev.dyzjct.kura.module.modules.movement

import dev.dyzjct.kura.Kura
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.utils.TimerUtils
import base.utils.concurrent.threads.runSafe
import net.minecraft.entity.Entity
import net.minecraft.entity.passive.HorseEntity
import net.minecraft.entity.passive.LlamaEntity
import net.minecraft.entity.passive.MuleEntity
import net.minecraft.entity.passive.PigEntity
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround

object Step : Module(
    name = "Step",
    "上台阶",
    category = Category.MOVEMENT,
    description = "Allows you to walk up full blocks instantly"
) {
    private var mode = msetting("Mode", Mode.VANILLA)
    private var strict = bsetting("Strict", false)
    private var useTimer = bsetting("UseTimer", true)
    private var entityStep = bsetting("EntityStep", false)
    private var stepDelay = isetting("StepDelay", 200, 0, 1000)
    private var stepHeight0 = fsetting("Height", 2.0f, 1.0f, 5.0f)
    private var entityRiding: Entity? = null
    private var stepTimer = TimerUtils()
    private var timer = false

    init {
        onMotion {
            if (timer && player.isOnGround) {
                Kura.TICK_TIMER = 1f
                timer = false
            }
            if (player.isOnGround && stepTimer.passed(stepDelay.value)) {
                if (player.isRiding) {
                    player.controllingVehicle?.let {
                        entityRiding = it
                        if (entityStep.value) {
                            it.stepHeight = stepHeight0.value
                        }
                    }
                } else if (player.controllingVehicle == null) {
                    player.stepHeight = stepHeight0.value
                }
            } else {
                if (player.isRiding && player.controllingVehicle != null) {
                    entityRiding = player.controllingVehicle
                    entityRiding?.let { entityRiding ->
                        if (entityRiding is HorseEntity || entityRiding is LlamaEntity || entityRiding is MuleEntity || entityRiding is PigEntity && player.controllingVehicle === entityRiding && entityRiding.canBeSaddled()) {
                            entityRiding.stepHeight = 1f
                        } else {
                            entityRiding.stepHeight = 0.5f
                        }
                    }
                } else {
                    player.stepHeight = 0.6f
                }
            }
            when (mode.value) {
                Mode.VANILLA -> {
                    player.stepHeight = stepHeight0.value
                }

                Mode.PACKET -> {
                    val stepHeight = player.y - player.prevY

                    if (stepHeight <= 0.75 || stepHeight > stepHeight0.value || strict.value && stepHeight > 1) {
                        return@onMotion
                    }

                    val offsets = getOffset(stepHeight)
                    if (offsets != null && offsets.size > 1) {
                        if (useTimer.value) {
                            Kura.TICK_TIMER = 1f / offsets.size
                            timer = true
                        }
                        for (offset in offsets) {
                            player.networkHandler.sendPacket(
                                PositionAndOnGround(
                                    player.prevX,
                                    player.prevY + offset,
                                    player.prevZ,
                                    false
                                )
                            )
                        }
                    }
                }
            }
        }
    }


    private fun getOffset(height: Double): DoubleArray? {
        return when (height) {
            0.75 -> {
                if (strict.value) {
                    doubleArrayOf(0.42, 0.753, 0.75)
                } else {
                    doubleArrayOf(0.42, 0.753)
                }
            }

            0.8125 -> {
                if (strict.value) {
                    doubleArrayOf(0.39, 0.7, 0.8125)
                } else {
                    doubleArrayOf(0.39, 0.7)
                }
            }

            0.875 -> {
                if (strict.value) {
                    doubleArrayOf(0.39, 0.7, 0.875)
                } else {
                    doubleArrayOf(0.39, 0.7)
                }
            }

            1.0 -> {
                if (strict.value) {
                    doubleArrayOf(0.42, 0.753, 1.0)
                } else {
                    doubleArrayOf(0.42, 0.753)
                }
            }

            1.5 -> {
                doubleArrayOf(0.42, 0.75, 1.0, 1.16, 1.23, 1.2)
            }

            2.0 -> {
                doubleArrayOf(0.42, 0.78, 0.63, 0.51, 0.9, 1.21, 1.45, 1.43)
            }

            2.5 -> {
                doubleArrayOf(0.425, 0.821, 0.699, 0.599, 1.022, 1.372, 1.652, 1.869, 2.019, 1.907)
            }

            else -> {
                null
            }
        }
    }

    override fun onDisable() {
        runSafe {
            player.stepHeight = 0.5f
        }
    }

    enum class Mode {
        VANILLA, PACKET
    }
}