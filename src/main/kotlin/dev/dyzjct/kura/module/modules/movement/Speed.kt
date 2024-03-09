package dev.dyzjct.kura.module.modules.movement

import dev.dyzjct.kura.Kura
import dev.dyzjct.kura.manager.MovementManager
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import base.events.player.PlayerMoveEvent
import base.system.event.safeEventListener
import base.utils.entity.EntityUtils
import base.utils.entity.EntityUtils.baseMoveSpeed
import base.utils.entity.EntityUtils.isInBurrow
import base.utils.entity.EntityUtils.jumpSpeed
import net.minecraft.entity.effect.StatusEffects

object Speed :
    Module(name = "Speed", langName = "移动加速", category = Category.MOVEMENT, description = "Faster Move.") {
    private var damageBoost by bsetting("DamageBoost", true)
    private var strict by bsetting("Strict", true)
    private var useTimer by bsetting("UseTimer", true)
    private var burrowDetect by bsetting("BurrowDetect", true)
    private var baseSpeed = 0.0
    private var stage = 0
    private var ticks = 0

    override fun onEnable() {
        stage = 1
        ticks = 0
        baseSpeed = 0.2873
    }

    init {

        safeEventListener<PlayerMoveEvent> { event ->
            if (player.abilities.flying) return@safeEventListener
            if (player.isFallFlying) return@safeEventListener
            if (player.hungerManager.foodLevel <= 6) return@safeEventListener
            if (event.cancelled) return@safeEventListener
            event.cancelled = true
            if (EntityUtils.isMoving()) {
                Kura.TICK_TIMER = if (useTimer) 1.088f else 1f
                if (stage == 1 && player.isOnGround) {
                    player.setVelocity(
                        player.velocity.x,
                        jumpSpeed,
                        player.velocity.z
                    )
                    event.vec.y = jumpSpeed
                    baseSpeed *= 2.149
                    stage = 2
                } else if (stage == 2) {
                    baseSpeed =
                        MovementManager.currentPlayerSpeed - 0.66 * (MovementManager.currentPlayerSpeed - baseMoveSpeed)
                    stage = 3
                } else {
                    if (world.getBlockCollisions(
                            player,
                            player.boundingBox.offset(0.0, player.velocity.getY(), 0.0)
                        ).iterator().hasNext() || player.verticalCollision
                    ) stage = 1
                    baseSpeed =
                        MovementManager.currentPlayerSpeed - MovementManager.currentPlayerSpeed / 159.0
                }
                baseSpeed = baseSpeed.coerceAtLeast(baseMoveSpeed)
                var baseStrictSpeed =
                    if (strict || player.input.movementForward < 1) 0.465 else 0.576
                var baseRestrictedSpeed =
                    if (strict || player.input.movementForward < 1) 0.44 else 0.57
                if (player.hasStatusEffect(StatusEffects.SPEED)) {
                    val amplifier = player.getStatusEffect(StatusEffects.SPEED)!!.amplifier.toDouble()
                    baseStrictSpeed *= 1 + 0.2 * (amplifier + 1)
                    baseRestrictedSpeed *= 1 + 0.2 * (amplifier + 1)
                }
                if (player.hasStatusEffect(StatusEffects.SLOWNESS)) {
                    val amplifier = player.getStatusEffect(StatusEffects.SLOWNESS)!!.amplifier.toDouble()
                    baseStrictSpeed /= 1 + 0.2 * (amplifier + 1)
                    baseRestrictedSpeed /= 1 + 0.2 * (amplifier + 1)
                }
                baseSpeed = baseSpeed.coerceAtMost(if (ticks > 25) baseStrictSpeed else baseRestrictedSpeed)
                if (damageBoost && MovementManager.boostSpeed != 0.0) {
                    baseSpeed += MovementManager.boostSpeed
                    MovementManager.boostReset()
                }
                if (ticks++ > 50) ticks = 0
                if (burrowDetect && isInBurrow()) {
                    baseSpeed = 0.2873 * 0.1f
                }
                event.setSpeed(baseSpeed)
            } else {
                Kura.TICK_TIMER = 1f
                event.setSpeed(0.0)
            }
        }
    }
}