package dev.dyzjct.kura.manager

import com.mojang.blaze3d.platform.GlStateManager
import dev.dyzjct.kura.module.ModuleManager
import dev.dyzjct.kura.event.events.ConnectionEvent
import dev.dyzjct.kura.event.events.PacketEvents
import dev.dyzjct.kura.event.events.TickEvent
import dev.dyzjct.kura.event.events.screen.ResolutionUpdateEvent
import dev.dyzjct.kura.event.eventbus.ListenerOwner
import dev.dyzjct.kura.event.eventbus.listener
import dev.dyzjct.kura.event.eventbus.safeEventListener
import net.minecraft.client.MinecraftClient
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket

internal object EventListenerManager : ListenerOwner() {
    private var displayWidth = 0
    private var displayHeight = 0

    fun call() {
        listener<TickEvent.Post>(true) {
            val mc = MinecraftClient.getInstance()
            if (mc.window.width != displayWidth || mc.window.height != displayHeight) {
                displayWidth = mc.window.width
                displayHeight = mc.window.height
                ResolutionUpdateEvent(mc.window.width, mc.window.height).post()
                GlStateManager._glUseProgram(0)
            }
        }

        safeEventListener<PacketEvents.Receive>(true) { event ->
            when (event.packet) {
                is DisconnectS2CPacket -> {
                    ModuleManager.onLogout()
                    ConnectionEvent.Disconnect.post()
                    ModuleManager.getToggleList().forEach { module ->
                        module.safeDisable()
                    }
                }
            }
        }
    }
}