package dev.dyzjct.kura.module.modules.misc

import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import melon.events.ConnectionEvent
import melon.events.screen.GuiScreenEvent
import melon.system.event.listener
import melon.utils.concurrent.threads.BackgroundScope
import melon.utils.concurrent.threads.onMainThread
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ConnectScreen
import net.minecraft.client.gui.screen.DisconnectedScreen
import net.minecraft.client.network.ServerAddress
import net.minecraft.client.network.ServerInfo

object AutoReconnect :
    Module(name = "AutoReconnect", langName = "自动重连", description = "Auto reconnect", category = Category.MISC) {
    private var serverInfo: ServerInfo? = null
    private var serverAddress: ServerAddress? = null

    private val delay by isetting("Delay", 1000, 500, 10000)

    private var isReconnecting = false
    private var startTime = 0L

    init {
        listener<ConnectionEvent.Join> {
            serverInfo = it.serverInfo
            serverAddress = it.serverAddress
        }

        listener<GuiScreenEvent.Displayed> {
            if (it.screen is DisconnectedScreen && serverAddress != null && serverInfo != null) {
                isReconnecting = true
                startTime = System.currentTimeMillis()

                BackgroundScope.launch {
                    delay(delay.toLong())
                    onMainThread {
                        ConnectScreen.connect(mc.currentScreen, mc, serverAddress, serverInfo, false)
                    }
                }
            }
        }

        listener<GuiScreenEvent.Close> {
            if (it.screen is DisconnectedScreen) {
                isReconnecting = false
            }
        }
    }

    fun render(context: DrawContext, mc: MinecraftClient) {
        if (isReconnecting) {
            val text = "Reconnecting in ${(startTime + delay - System.currentTimeMillis() + 1000) / 1000}s"
            context.drawTextWithShadow(
                mc.textRenderer, text, 2, 2, 16777215
            )
        }
    }
}