package dev.dyzjct.kura.module.modules.movement

import dev.dyzjct.kura.manager.MovementManager
import dev.dyzjct.kura.manager.MovementManager.boostSpeed
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.module.modules.player.FreeCam
import dev.dyzjct.kura.event.events.player.PlayerMoveEvent
import dev.dyzjct.kura.event.eventbus.safeEventListener
import base.utils.entity.EntityUtils
import base.utils.entity.EntityUtils.baseMoveSpeed
import base.utils.entity.EntityUtils.isInBurrow
import base.utils.entity.EntityUtils.isInWeb
import net.minecraft.entity.effect.StatusEffects
import kotlin.math.cos
import kotlin.math.sin

object Strafe :
    Module(name = "Strafe", langName = "灵活动作", category = Category.MOVEMENT, description = "Better Move.") {
    private val mode = msetting("Mode", Mode.NORMAL)
    private var boost = bsetting("DamageBoost", false)
    private var eatingCheck = bsetting("EatingCheck", true)
    private var burrowDetect = bsetting("BurrowDetect", true)
    private var bMode = msetting("BMode", BMode.Slow).isTrue(burrowDetect)
    private var stage = 1
    private var moveSpeed = 0.0

    init {
        safeEventListener<PlayerMoveEvent> { event ->
            if (player.isFallFlying || isInWeb(player) || (player.isSneaking && (!isInBurrow() || !burrowDetect.value))) return@safeEventListener
            if (player.isUsingItem && eatingCheck.value) return@safeEventListener
            if (burrowDetect.value && bMode.value == BMode.Cancel && isInBurrow()) return@safeEventListener

            if (shouldReturn() && !player.isTouchingWater && !player.isInLava) {
                if (player.onGround) {
                    stage = 2
                }
                when (stage) {
                    0 -> {
                        ++stage
                        MovementManager.currentPlayerSpeed = 0.0
                    }

                    2 -> {
                        if (player.onGround && mc.options.jumpKey.isPressed) {
                            if (player.getStatusEffect(StatusEffects.SPEED) != null) {
                                event.vec.y = player.velocity.y
                                moveSpeed *= if (mode.value == Mode.NORMAL) 1.7 else 2.149
                            }
                        }
                    }

                    3 -> {
                        moveSpeed =
                            MovementManager.currentPlayerSpeed - (if (mode.value == Mode.NORMAL) 0.6901 else 0.795) * (MovementManager.currentPlayerSpeed - baseMoveSpeed)
                    }

                    else -> {
                        if (world.getBlockCollisions(
                                player, player.boundingBox.offset(0.0, player.velocity.getY(), 0.0)
                            ).iterator().hasNext() || player.verticalCollision
                        ) {
                            stage =
                                if (player.input.movementForward != 0.0f || player.input.movementSideways != 0.0f) 1 else 0
                        }
                        moveSpeed =
                            MovementManager.currentPlayerSpeed - 0.66 * (MovementManager.currentPlayerSpeed - baseMoveSpeed)
                    }
                }
                if (boost.value && boostSpeed != 0.0 && EntityUtils.isMoving()) {
                    moveSpeed = boostSpeed
                    MovementManager.boostReset()
                }
                moveSpeed = if (!mc.options.jumpKey.isPressed && player.onGround) {
                    baseMoveSpeed
                } else {
                    moveSpeed.coerceAtLeast(baseMoveSpeed)
                }
                if (burrowDetect.value && bMode.value == BMode.Slow && isInBurrow()) {
                    if (FreeCam.isDisabled) {
                        moveSpeed = 0.2873 * 0.1f
                    }
                }
                if (player.input.movementForward.toDouble() == 0.0 && player.input.movementSideways.toDouble() == 0.0) {
                    event.setSpeed(0.0)
                } else if (player.input.movementForward.toDouble() != 0.0 && player.input.movementSideways.toDouble() != 0.0) {
                    player.input.movementForward *= sin(0.7853981633974483).toFloat()
                    player.input.movementSideways *= cos(0.7853981633974483).toFloat()
                }
                event.vec.x = (player.input.movementForward * moveSpeed * -sin(
                    Math.toRadians(player.yaw.toDouble())
                ) + player.input.movementSideways * moveSpeed * cos(
                    Math.toRadians(
                        player.yaw.toDouble()
                    )
                )) * if (mode.value == Mode.NORMAL) 0.993 else 0.99
                event.vec.z = (player.input.movementForward * moveSpeed * cos(
                    Math.toRadians(player.yaw.toDouble())
                ) - player.input.movementSideways * moveSpeed * -sin(
                    Math.toRadians(
                        player.yaw.toDouble()
                    )
                )) * if (mode.value == Mode.NORMAL) 0.993 else 0.99
                //event.setSpeed(moveSpeed * if (mode.valueSetting == Mode.NORMAL) 0.993 else 0.99)
                ++stage
            }
        }
    }

    override fun getHudInfo(): String {
        return if (mode.value == Mode.NORMAL) "Normal" else "Strict"
    }

    private fun shouldReturn(): Boolean {
        return Speed.isDisabled
    }

    @Suppress("unused")
    enum class Mode {
        STRICT, NORMAL
    }

    enum class BMode {
        Cancel, Slow
    }

}