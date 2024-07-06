package dev.dyzjct.kura.module.modules.render

import base.utils.chat.ChatUtil
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module


object AnimationRemover : Module(name = "AnimationRemover", langName = "移除一些动画", category = Category.RENDER) {
    val fakeSneak by bsetting("FakeSneak", false)
    val arm by bsetting("Arm", false)
    val leg by bsetting("Leg", false)
    val removeSelf by bsetting("RemoveSelf", false)
    val debug by bsetting("Debug", false)

    init {
        onLoop {
            if (fakeSneak) {
                if (debug) {
                    ChatUtil.sendNoSpamMessage("[player.prevCapeX]" + player.capeX.toString())
                    ChatUtil.sendNoSpamMessage("[player.prevCapeY]" + player.capeY.toString())
                    ChatUtil.sendNoSpamMessage("[player.prevCapeZ]" + player.capeZ.toString())
                }
            }
        }
    }
}