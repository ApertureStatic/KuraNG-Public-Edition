package dev.dyzjct.kura.module.modules.misc

import dev.dyzjct.kura.event.events.ConnectionEvent
import dev.dyzjct.kura.event.events.screen.GuiScreenEvent
import dev.dyzjct.kura.event.eventbus.listener
import base.utils.concurrent.threads.BackgroundScope
import base.utils.concurrent.threads.onMainThread
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraft.client.gui.screen.DisconnectedScreen
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen
import net.minecraft.client.network.ServerAddress
import net.minecraft.client.network.ServerInfo

object AutoReconnect :
    Module(name = "AutoReconnect", langName = "自动重连", description = "Auto reconnect", category = Category.MISC) {
    private var serverInfo: ServerInfo? = null
    private var serverAddress: ServerAddress? = null

    private val delay by isetting("Delay", 1000, 500, 10000)

    var isReconnecting = false
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

    fun getTime(): Long {
        return (startTime + delay - System.currentTimeMillis() + 1000) / 1000L
    }

    fun getText(): String {
        return "Reconnecting in ${(startTime + delay - System.currentTimeMillis() + 1000) / 1000}s"
    }
}