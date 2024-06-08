//package dev.dyzjct.kura.module.modules.combat
//
//import base.system.event.SafeClientEvent
//import base.utils.combat.getTarget
//import dev.dyzjct.kura.module.Category
//import dev.dyzjct.kura.module.Module
//import dev.dyzjct.kura.utils.animations.Easing
//import dev.dyzjct.kura.utils.math.RotationUtils.getRotationToEntity
//import net.minecraft.entity.Entity
//
//object AimAssist : Module(
//    name = "AimAssist",
//    langName = "自动瞄准",
//    category = Category.COMBAT,
//    type = Type.Both
//) {
//    private val range by dsetting("Range", 5.0, 1.0, 8.0)
//    private val speed by isetting("Speed", 400, 0, 1000)
//
//    private var currentYaw: Float? = null
//    private var prevYaw: Float? = null
//    private var lastUpdateTime = 0L
//
//    init {
//        onRender3D {
//            getTarget(range)?.let { target ->
//                if (!target.isAlive) {
//                    return@onRender3D
//                }
//                currentYaw = getYawToEntityNew(target)
//                lastUpdateTime = System.currentTimeMillis()
//                currentYaw?.let { current ->
//                    prevYaw?.let { prev ->
//                        player.setYaw(
//                            prev + ((current - prev) * Easing.OUT_QUART.inc(
//                                Easing.toDelta(
//                                    lastUpdateTime, speed
//                                )
//                            ))
//                        )
//                        prevYaw = prev + ((current - prev) * Easing.OUT_QUART.inc(
//                            Easing.toDelta(
//                                lastUpdateTime, speed
//                            )
//                        ))
//                    } ?: run {
//                        prevYaw =
//                            getYawToEntityNew(target) + ((current - getYawToEntityNew(target)) * Easing.OUT_QUART.inc(
//                                Easing.toDelta(
//                                    lastUpdateTime, speed
//                                )
//                            ))
//                    }
//                }
//            } ?: kotlin.run {
//                currentYaw = null
//                prevYaw = null
//            }
//        }
//    }
//
//    private fun SafeClientEvent.getYawToEntityNew(entity: Entity): Float {
//        return getRotationToEntity(entity).x
//    }
//}