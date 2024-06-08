package dev.dyzjct.kura.module.modules.misc

import base.utils.concurrent.threads.runSafe
import com.mojang.authlib.GameProfile
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import net.minecraft.client.network.OtherClientPlayerEntity
import net.minecraft.entity.Entity
import java.util.*

object FakePlayer : Module(
    name = "FakePlayer",
    langName = "假人",
    category = Category.MISC,
    description = "Spawns a fake Player",
    type = Type.Both
) {
    private var health by isetting("Health", 12, 0, 36)
    private var fpName by ssetting("Name", "Ab_noJB")
    private var fakePlayer: OtherClientPlayerEntity? = null

    override fun getHudInfo(): String {
        return fpName
    }

    override fun onEnable() {
        runSafe {
            fakePlayer = OtherClientPlayerEntity(
                world,
                GameProfile(UUID.fromString("60569353-f22b-42da-b84b-d706a65c5ddf"), fpName)
            )
            fakePlayer?.let { fakePlayer ->
                fakePlayer.copyPositionAndRotation(player)
                for (potionEffect in player.activeStatusEffects) {
                    fakePlayer.addStatusEffect(potionEffect.value)
                }
                fakePlayer.health = health.toFloat()
                fakePlayer.inventory.clone(player.inventory)
                fakePlayer.yaw = player.yaw
                world.addEntity(-114514, fakePlayer)
            }
        }
    }

    override fun onDisable() {
        runSafe {
            fakePlayer?.let {
                it.kill()
                it.setRemoved(Entity.RemovalReason.KILLED)
                it.onRemoved()
            }
        }
    }
}