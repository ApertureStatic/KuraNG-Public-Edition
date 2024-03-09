package dev.dyzjct.kura.command.impl

import dev.dyzjct.kura.command.Command
import dev.dyzjct.kura.module.modules.misc.FakePlayer
import melon.utils.chat.ChatUtil

object FakePlayerCommand : Command("fakeplayer", arrayOf("fp"), "Fast Toggle FakePlayer Module") {
    init {
        builder.literal {
            executor {
                FakePlayer.toggle()
                ChatUtil.sendNoSpamMessage(
                    "Module FakePlayer Has Been ${
                        if (FakePlayer.isEnabled) {
                            "Enabled"
                        } else {
                            "Disabled"
                        }
                    } !"
                )
            }
        }
    }
}