package melon.utils

import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayNetworkHandler
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.world.World

object Wrapper {
    @JvmStatic
    val minecraft: MinecraftClient
        get() = MinecraftClient.getInstance()

    @JvmStatic
    val player: ClientPlayerEntity?
        get() = minecraft.player

    @JvmStatic
    val world: World?
        get() = minecraft.world

    @JvmStatic
    val connection: ClientPlayNetworkHandler?
        get() = minecraft.networkHandler
}