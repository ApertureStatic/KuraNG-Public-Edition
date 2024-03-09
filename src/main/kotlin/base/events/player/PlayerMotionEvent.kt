package base.events.player

import base.system.event.*
import net.minecraft.client.MinecraftClient
import net.minecraft.util.math.BlockPos

class PlayerMotionEvent(
    var stageType: StageType,
    var x: Double,
    var y: Double,
    var z: Double,
    var yaw: Float,
    var pitch: Float,
    var isOnGround: Boolean
) : Event, ICancellable by Cancellable(), IEventPosting by Companion {
    inline operator fun <T> invoke(block: PlayerMotionEvent.() -> T) = run(block)

    constructor(stage: StageType, event: PlayerMotionEvent) : this(
        stage,
        event.x,
        event.y,
        event.z,
        event.yaw,
        event.pitch,
        event.isOnGround
    )

    companion object : EventBus()

    fun setRotation(yaw: Float, pitch: Float) {
        MinecraftClient.getInstance().player?.let {
            it.headYaw = yaw
            it.bodyYaw = yaw
        }
        this.yaw = yaw
        this.pitch = pitch
    }

    fun setPosition(x: Double, y: Double, z: Double, onGround: Boolean) {
        this.x = x
        this.y = y
        this.z = z
        isOnGround = onGround
    }

    fun setPosition(pos: BlockPos, onGround: Boolean) {
        x = pos.x.toDouble()
        y = pos.y.toDouble()
        z = pos.z.toDouble()
        isOnGround = onGround
    }

    fun setPostion(x: Double, y: Double, z: Double, yaw: Float, pitch: Float, onGround: Boolean) {
        this.x = x
        this.y = y
        this.z = z
        this.yaw = yaw
        this.pitch = pitch
        isOnGround = onGround
    }

    fun setYaw(yaw: Double) {
        this.yaw = yaw.toFloat()
    }

    fun setPitch(pitch: Double) {
        this.pitch = pitch.toFloat()
    }
}