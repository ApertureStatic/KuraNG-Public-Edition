package melon.utils.interfaces

import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayNetworkHandler
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.client.network.ClientPlayerInteractionManager
import net.minecraft.client.world.ClientWorld

interface Helper {
    val mc: MinecraftClient
        get() = MinecraftClient.getInstance()

    val world: ClientWorld?
        get() = mc.world

    val player: ClientPlayerEntity?
        get() = mc.player

    val playerController: ClientPlayerInteractionManager?
        get() = mc.interactionManager

    val connection: ClientPlayNetworkHandler?
        get() = mc.networkHandler
}