package dev.dyzjct.kura.module.modules.client

import dev.dyzjct.kura.event.eventbus.SafeClientEvent
import dev.dyzjct.kura.event.events.input.KeyboardTickEvent
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import net.minecraft.util.math.MathHelper
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

object MovementFix : Module(
    name = "MovementFix",
    category = Category.CLIENT,
    description = "Fix yor movement Input.",
) {
    fun fixMovement(event: KeyboardTickEvent, packetYaw: Float) {
        val player = mc.player ?: return
        val input = player.input ?: return

        val forward = input.movementForward
        val strafe = input.movementSideways

        // 如果没有按任何键盘走路键，直接返回
        if (forward == 0f && strafe == 0f) return

        // 🌟【自定义判断位点 1】：可以自行决定何时跳过该按键转换
        val SHOULD_SKIP_FIX = false
        if (SHOULD_SKIP_FIX) return

        // 计算当前真实的屏幕视角与即将发包的虚拟假视角之间的差值
        val clientYaw = player.yaw
        val angleDiff = Math.toRadians((clientYaw - packetYaw).toDouble())

        val cos = cos(angleDiff).toFloat()
        val sin = sin(angleDiff).toFloat()

        // 💡 仅对 W/A/S/D 的按键输入分量进行重新分配，完全不改动绝对位移或爆炸动量
        val correctedForward = forward * cos - strafe * sin
        val correctedStrafe = strafe * cos + forward * sin

        input.movementForward = correctedForward
        input.movementSideways = correctedStrafe

        // 重新对齐原版按键布尔状态，防止疾跑或跳跃判定失效
        if (correctedForward != 0f) {
            input.pressingForward = correctedForward > 0f
            input.pressingBack = correctedForward < 0f
        }
        if (correctedStrafe != 0f) {
            input.pressingLeft = correctedStrafe > 0f
            input.pressingRight = correctedStrafe < 0f
        }
    }
}

