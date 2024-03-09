package melon.utils

import dev.dyzjct.kura.utils.animations.toRadian
import dev.dyzjct.kura.utils.extension.toDegree
import melon.utils.player.RotationUtils
import net.minecraft.client.input.Input
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.client.option.GameOptions
import kotlin.math.atan2

object MovementUtils {
    fun ClientPlayerEntity.calcMoveYaw(): Double {
        return calcMoveYaw(
            yaw = yaw,
            moveForward = input.movementForward,
            moveStrafe = input.movementSideways
        )
    }

    fun calcMoveYaw(
        yaw: Float,
        moveForward: Float,
        moveStrafe: Float
    ): Double {
        val moveYaw = if (moveForward == 0.0f && moveStrafe == 0.0f) 0.0
        else atan2(moveForward, moveStrafe).toDegree() - 90.0
        return RotationUtils.normalizeAngle(yaw + moveYaw).toRadian()
    }

    fun GameOptions.resetMove() {
        forwardKey.isPressed = false
        backKey.isPressed = false
        leftKey.isPressed = false
        rightKey.isPressed = false
    }

    fun Input.resetMove() {
        if (pressingForward) pressingForward = false
        if (pressingBack) pressingBack = false
        if (pressingLeft) pressingLeft = false
        if (pressingRight) pressingRight = false
    }
}