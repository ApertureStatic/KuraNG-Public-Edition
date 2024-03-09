package melon.utils.player

import melon.system.util.interfaces.MinecraftWrapper.Companion.mc
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import kotlin.math.hypot

val currentTps: Float = mc.renderTickCounter.tickTime / 1000.0f

fun getTargetSpeed(target: PlayerEntity): Double {
    return hypot((target.x - target.prevX), (target.z - target.prevZ)) / currentTps * 3.6 //用LagCompensator会有问题
    //return (sqrt(target.x - target.lastRenderX) * (sqrt(target.x - target.lastRenderX) + (target.z - target.lastRenderZ) * (target.z - target.lastRenderZ)) / LagCompensator.tickRate *
}
fun getTargetSpeed(target: LivingEntity): Double {
    return hypot((target.x - target.prevX), (target.z - target.prevZ)) / currentTps * 3.6
    //return (sqrt(target.x - target.lastRenderX) * (sqrt(target.x - target.lastRenderX) + (target.z - target.lastRenderZ) * (target.z - target.lastRenderZ)) / LagCompensator.tickRate *
}
fun getTargetSpeed(target: Entity): Double {
    return hypot((target.x - target.prevX), (target.z - target.prevZ)) / currentTps * 3.6
    //return (sqrt(target.x - target.lastRenderX) * (sqrt(target.x - target.lastRenderX) + (target.z - target.lastRenderZ) * (target.z - target.lastRenderZ)) / LagCompensator.tickRate *
}