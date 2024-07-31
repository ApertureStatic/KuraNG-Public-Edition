package dev.dyzjct.kura.module.modules.misc

import dev.dyzjct.kura.event.eventbus.SafeClientEvent
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.module.modules.movement.HoleSnap
import dev.dyzjct.kura.module.modules.movement.Speed
import dev.dyzjct.kura.module.modules.movement.Step
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.util.math.Box
import net.minecraft.util.math.MathHelper
import net.minecraft.util.shape.VoxelShape
import kotlin.math.floor

//Thanks For asphyxia1337(Alpha432)
object Clip : Module(
    name = "Clip",
    langName = "卡墙",
    category = Category.MISC,
    description = "Phases into blocks nearby to prevent crystal damage."
) {
    private val timeout = isetting("Timeout", 5, 1, 10)
    private val collision = isetting("CollisionSize", 2, -20, 2)
    private var packets = 0
    override fun onDisable() {
        packets = 0
    }

    override fun getHudInfo(): String {
        return packets.toString()
    }

    init {
        onMotion {
            if (Speed.isEnabled || Step.isEnabled || HoleSnap.isEnabled) {
                disable()
                return@onMotion
            }
            if (hasFewerCollisions(player, collision.value)) {
                player.setPosition(
                    roundToClosest(player.x, floor(player.x) + 0.301, floor(player.x) + 0.699),
                    player.y,
                    roundToClosest(
                        player.z, floor(player.z) + 0.301, floor(player.z) + 0.699
                    )
                )
                packets = 0
            } else if (player.age % timeout.value == 0) {
                player.setPosition(
                    player.x + MathHelper.clamp(
                        roundToClosest(
                            player.x, floor(player.x) + 0.241, floor(
                                player.x
                            ) + 0.759
                        ) - player.x, -0.03, 0.03
                    ), player.y, player.z + MathHelper.clamp(
                        roundToClosest(
                            player.z, floor(player.z) + 0.241, floor(player.z) + 0.759
                        ) - player.z, -0.03, 0.03
                    )
                )
                connection.sendPacket(
                    PlayerMoveC2SPacket.PositionAndOnGround(
                        player.x, player.y, player.z, true
                    )
                )
                connection.sendPacket(
                    PlayerMoveC2SPacket.PositionAndOnGround(
                        roundToClosest(
                            player.x, floor(player.x) + 0.23, floor(
                                player.x
                            ) + 0.77
                        ), player.y, roundToClosest(
                            player.z, floor(player.z) + 0.23, floor(
                                player.z
                            ) + 0.77
                        ), true
                    )
                )
                packets++
            }
        }
    }

    private fun roundToClosest(num: Double, low: Double, high: Double): Double {
        val d1 = num - low
        val d2 = high - num
        return if (d2 > d1) {
            low
        } else {
            high
        }
    }

    fun SafeClientEvent.hasFewerCollisions(player: PlayerEntity, collisionValue: Int): Boolean {
        // 扩展玩家的碰撞盒子
        val expandedBox: Box = player.boundingBox.expand(0.01, 0.0, 0.01)

        // 获取与扩展后的碰撞盒子相交的所有碰撞盒子
        val collisionBoxes: List<VoxelShape> = world.getBlockCollisions(player, expandedBox).toList()

        // 检查碰撞盒子的数量是否小于指定的值
        return collisionBoxes.size < collisionValue
    }
}