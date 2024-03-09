package melon.events.player

import melon.system.event.*
import net.minecraft.client.MinecraftClient
import net.minecraft.entity.MovementType
import net.minecraft.util.math.Vec3d
import kotlin.math.cos
import kotlin.math.sin

class PlayerMoveEvent(var type: MovementType, var vec: Vec3d) : Event,
    ICancellable by Cancellable(), IEventPosting by Companion {
    fun setSpeed(speed: Double) {
        val player = MinecraftClient.getInstance().player ?: return
        var yaw = player.yaw
        player.input?.let {
            var forward = it.movementForward.toDouble()
            var strafe = it.movementSideways.toDouble()
            if (forward == 0.0 && strafe == 0.0) {
                vec.x = 0.0
                vec.z = 0.0
            } else {
                if (forward != 0.0) {
                    if (strafe > 0) {
                        yaw += (if (forward > 0) -45 else 45).toFloat()
                    } else if (strafe < 0) {
                        yaw += (if (forward > 0) 45 else -45).toFloat()
                    }
                    strafe = 0.0
                    forward = if (forward > 0) {
                        1.0
                    } else {
                        -1.0
                    }
                }
                val cos = cos(Math.toRadians((yaw + 90).toDouble()))
                val sin = sin(Math.toRadians((yaw + 90).toDouble()))
                vec.x = (forward * speed * cos + strafe * speed * sin)
                vec.z = (forward * speed * sin - strafe * speed * cos)
            }
        }
    }

    companion object : NamedProfilerEventBus("melonPlayerMove")
}