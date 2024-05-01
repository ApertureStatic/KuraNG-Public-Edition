package dev.dyzjct.kura.module.modules.misc

import com.mojang.authlib.GameProfile
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.setting.Setting
import base.utils.concurrent.threads.runSafe
import net.minecraft.client.network.OtherClientPlayerEntity
import net.minecraft.entity.Entity
import java.util.*

object FakePlayer : Module(name = "FakePlayer", langName = "假人" , category = Category.MISC, description = "Spawns a fake Player") {
    private var health: Setting<Int> = isetting("Health", 12, 0, 36)
    private var fpName: Setting<String> = addStringSetting("Name", "Ab_noJB")
    private var fakePlayer: OtherClientPlayerEntity? = null

    override fun getHudInfo(): String {
        return fpName.value
    }

    override fun onEnable() {
        runSafe {
            fakePlayer = OtherClientPlayerEntity(
                world,
                GameProfile(UUID.fromString("60569353-f22b-42da-b84b-d706a65c5ddf"), fpName.value)
            )
            fakePlayer?.let { fakePlayer ->
                fakePlayer.copyPositionAndRotation(player)
                for (potionEffect in player.activeStatusEffects) {
                    fakePlayer.addStatusEffect(potionEffect.value)
                }
                fakePlayer.health = health.value.toFloat()
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