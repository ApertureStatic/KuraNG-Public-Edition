package dev.dyzjct.kura.module.modules.movement

import base.utils.concurrent.threads.runSafe
import base.utils.entity.EntityUtils.getHealth
import com.mojang.authlib.GameProfile
import dev.dyzjct.kura.event.eventbus.safeEventListener
import dev.dyzjct.kura.event.events.PacketEvents
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import net.minecraft.client.network.OtherClientPlayerEntity
import net.minecraft.entity.Entity
import net.minecraft.network.packet.Packet
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import java.util.*

object Blink : Module(name = "Blink", langName = "瞬移", category = Category.MOVEMENT) {
    private val healthCheck by bsetting("HealthCheck", false)
    private val health by isetting("Health", 5, 1, 32).isTrue { healthCheck }

    var packets: Queue<Packet<*>> = LinkedList()
    private var clonedPlayer: OtherClientPlayerEntity? = null

    override fun onDisable() {
        runSafe {
            while (packets.isNotEmpty()) {
                connection.sendPacket(packets.poll())
            }
            clonedPlayer?.let {
                it.kill()
                it.setRemoved(Entity.RemovalReason.KILLED)
                it.onRemoved()
            }
            clonedPlayer = null
        }
    }

    override fun onEnable() {
        runSafe {
            clonedPlayer = OtherClientPlayerEntity(
                world,
                GameProfile(UUID.fromString("60569353-f22b-42da-b84b-d706a65c5ddf"), player.name.string)
            )
            clonedPlayer?.let { fakePlayer ->
                fakePlayer.copyPositionAndRotation(player)
                for (potionEffect in player.activeStatusEffects) {
                    fakePlayer.addStatusEffect(potionEffect.value)
                }
                fakePlayer.health = player.health
                fakePlayer.inventory.clone(player.inventory)
                fakePlayer.yaw = player.yaw
                world.addEntity(fakePlayer)
            }
        }
    }

    init {
        safeEventListener<PacketEvents.Send> {
            if (isEnabled && (it.packet is PlayerMoveC2SPacket)) {
                it.cancelled = true
                packets.add(it.packet)
            }
        }
        onMotion {
            if (healthCheck) {
                if (getHealth() <= health) disable()
            }
        }
    }

    override fun getHudInfo(): String {
        return packets.size.toString()
    }
}