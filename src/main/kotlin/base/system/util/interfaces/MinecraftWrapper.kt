package base.system.util.interfaces

import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.client.world.ClientWorld

interface MinecraftWrapper {
    val mc: MinecraftClient get() = Companion.mc

    val minecraft: MinecraftClient get() = Companion.mc

    val player: ClientPlayerEntity? get() = Companion.mc.player

    val world: ClientWorld? get() = Companion.mc.world

    companion object {
        val mc: MinecraftClient get() = MinecraftClient.getInstance()

        val minecraft: MinecraftClient get() = mc

        val player: ClientPlayerEntity? get() = mc.player

        val world: ClientWorld? get() = mc.world
    }
}