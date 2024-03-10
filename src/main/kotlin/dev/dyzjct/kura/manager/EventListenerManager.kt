package dev.dyzjct.kura.manager

import com.mojang.blaze3d.platform.GlStateManager
import dev.dyzjct.kura.module.ModuleManager
import base.events.ConnectionEvent
import base.events.PacketEvents
import base.events.TickEvent
import base.events.screen.ResolutionUpdateEvent
import base.system.event.ListenerOwner
import base.system.event.listener
import base.system.event.safeEventListener
import net.minecraft.client.MinecraftClient
import net.minecraft.network.packet.s2c.play.DisconnectS2CPacket

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