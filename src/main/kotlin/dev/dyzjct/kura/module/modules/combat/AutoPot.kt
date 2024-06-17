package dev.dyzjct.kura.module.modules.combat

import dev.dyzjct.kura.event.eventbus.SafeClientEvent
import base.utils.chat.ChatUtil
import base.utils.combat.getPredictedTarget
import base.utils.concurrent.threads.runSafe
import base.utils.extension.sendSequencedPacket
import base.utils.math.distanceSqTo
import base.utils.math.toBlockPos
import dev.dyzjct.kura.manager.HotbarManager.inventorySwap
import dev.dyzjct.kura.manager.HotbarManager.resetHotbar
import dev.dyzjct.kura.manager.HotbarManager.spoofHotbar
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.module.modules.client.CombatSystem
import dev.dyzjct.kura.utils.TimerUtils
import dev.dyzjct.kura.utils.inventory.InventoryUtil.findPotInventorySlot
import net.minecraft.block.Blocks
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.item.*
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.util.Hand
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.world.RaycastContext
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object AutoPot : Module(
    name = "AutoPot",
    langName = "自动喷药",
    description = "Auto use pot.",
    category = Category.COMBAT,
    type = Type.StrongOnly
) {
    private val rotate by bsetting("Rotate", true)
    private val pitch by isetting("Pitch", 86, 80, 90).isTrue { rotate }
    private val delay by isetting("Delay", 1050, 0, 2000)
    private val healthCheck by bsetting("HealthCheck", false)
    private val health by isetting("Health", 20, 0, 36).isTrue { healthCheck }
    private val effectRange by dsetting("EffectRange", 3.0, 0.0, 6.0, 0.1)
    private val predictTicks by isetting("Predict", 2, 0, 10)
    private val mode by msetting("Mode", Mode.RESISTANCE)

    private val timer = TimerUtils()

    override fun getHudInfo(): String? {
        runSafe {
            return mode.name
        }
        return null
    }

    init {
        onMotion {
            if (!timer.passedMs(delay.toLong())) {
                return@onMotion
            }
            if (healthCheck && player.health + player.absorptionAmount >= health) {
                return@onMotion
            }
//            if (player.pos.add(CombatUtil.getMotionVec(mc.player, predictTicks.getValueInt(), true))
            calcTrajectory(
                Items.SPLASH_POTION, player.yaw, player.pitch
            )?.let { calcedTrajectory ->
                if (getPredictedTarget(player, predictTicks).blockPos.distanceSqTo(
                        calcedTrajectory.toBlockPos()
                    ) > effectRange
                ) {
                    return@onMotion
                }
            }

            val effects: List<StatusEffectInstance> = ArrayList(player.statusEffects)
            for (potionEffect in effects) {
                if (potionEffect.effectType === StatusEffects.RESISTANCE && (potionEffect.amplifier + 1) > 1) {
                    return@onMotion
                }
            }
            doPot()
        }
    }

    private fun SafeClientEvent.doPot() {
        val slot = findPot()
        if (slot == null) {
            ChatUtil.sendNoSpamMessage("§c[!] No Potion found")
            return
        }
        timer.reset()
        if (CombatSystem.spoofMode.value != CombatSystem.SpoofMode.Normal) {
            inventorySwap(slot)
        } else {
            spoofHotbar(slot)
        }
        if (rotate) {
            /*if(Nullpoint.SPEED.getSpeedKpH() < 8){
                EntityUtil.sendYawAndPitch(Nullpoint.ROTATE.rotateYaw, -90);
            } else {
                EntityUtil.sendYawAndPitch(Nullpoint.ROTATE.rotateYaw, 88);
            }*/
            connection.sendPacket(PlayerMoveC2SPacket.LookAndOnGround(player.yaw, pitch.toFloat(), player.onGround))
        }

        sendSequencedPacket(world) { id ->
            PlayerInteractItemC2SPacket(
                Hand.MAIN_HAND,
                id
            )
        }
        if (CombatSystem.spoofMode.value != CombatSystem.SpoofMode.Normal) {
            inventorySwap(slot)
        } else {
            resetHotbar()
        }
    }

    private fun SafeClientEvent.findPot(): Int? {
        return when (mode) {
            Mode.RESISTANCE -> {
                findPotInventorySlot(StatusEffects.RESISTANCE)
            }

            Mode.INSTANT_HEALTH -> {
                findPotInventorySlot(StatusEffects.INSTANT_HEALTH)
            }

            else -> {
                null
            }
        }
    }


    private fun SafeClientEvent.calcTrajectory(item: Item, yaw: Float, pitch: Float): Vec3d? {
        var x: Double = interpolate(player.prevX, player.x, mc.tickDelta)
        var y: Double = interpolate(player.prevY, player.y, mc.tickDelta)
        var z: Double = interpolate(player.prevZ, player.z, mc.tickDelta)

        y = y + player.getEyeHeight(player.pose) - 0.1000000014901161

        x = x - cos(yaw / 180.0f * 3.1415927f) * 0.16f
        z = z - sin(yaw / 180.0f * 3.1415927f) * 0.16f

        val maxDist = getDistance(item)
        var motionX =
            (-sin(yaw / 180.0f * 3.1415927f) * cos(pitch / 180.0f * 3.1415927f) * maxDist).toDouble()
        var motionY = (-sin((pitch - getThrowPitch(item)) / 180.0f * 3.141593f) * maxDist).toDouble()
        var motionZ =
            (cos(yaw / 180.0f * 3.1415927f) * cos(pitch / 180.0f * 3.1415927f) * maxDist).toDouble()
        var power = player.itemUseTime / 20.0f
        power = (power * power + power * 2.0f) / 3.0f
        if (power > 1.0f) {
            power = 1.0f
        }
        val distance = sqrt((motionX * motionX + motionY * motionY + motionZ * motionZ).toFloat())
        motionX /= distance.toDouble()
        motionY /= distance.toDouble()
        motionZ /= distance.toDouble()

        val pow =
            (if (item is BowItem) (power * 2.0f) else if (item is CrossbowItem) (2.2f) else 1.0f) * getThrowVelocity(
                item
            )

        motionX *= pow.toDouble()
        motionY *= pow.toDouble()
        motionZ *= pow.toDouble()
        if (!player.isOnGround) motionY += player.velocity.getY()


        var lastPos: Vec3d?
        for (i in 0..299) {
            lastPos = Vec3d(x, y, z)
            x += motionX
            y += motionY
            z += motionZ
            if (world.getBlockState(BlockPos(x.toInt(), y.toInt(), z.toInt())).block === Blocks.WATER) {
                motionX *= 0.8
                motionY *= 0.8
                motionZ *= 0.8
            } else {
                motionX *= 0.99
                motionY *= 0.99
                motionZ *= 0.99
            }

            motionY -= if (item is BowItem) 0.05000000074505806
            else if (player.mainHandStack.item is CrossbowItem) 0.05000000074505806
            else 0.03


            val pos = Vec3d(x, y, z)


            val bhr = world.raycast(
                RaycastContext(
                    lastPos,
                    pos,
                    RaycastContext.ShapeType.OUTLINE,
                    RaycastContext.FluidHandling.NONE,
                    mc.player
                )
            )
            if (bhr != null && bhr.type == HitResult.Type.BLOCK) {
                return bhr.pos
            }
        }
        return null
    }

    private fun getDistance(item: Item): Float {
        return if (item is BowItem) 1.0f else 0.4f
    }

    private fun getThrowVelocity(item: Item): Float {
        if (item is SplashPotionItem || item is LingeringPotionItem) return 0.5f
        if (item is ExperienceBottleItem) return 0.59f
        if (item is TridentItem) return 2f
        return 1.5f
    }

    private fun getThrowPitch(item: Item): Int {
        if (item is SplashPotionItem || item is LingeringPotionItem || item is ExperienceBottleItem) return 20
        return 0
    }

    fun interpolate(previous: Double, current: Double, delta: Float): Double {
        return previous + (current - previous) * delta.toDouble()
    }


    enum class Mode {
        RESISTANCE, INSTANT_HEALTH
    }
}
