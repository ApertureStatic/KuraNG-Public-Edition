package dev.dyzjct.kura.command.impl

import dev.dyzjct.kura.command.Command
import dev.dyzjct.kura.gui.rewrite.gui.MelonClickGui
import dev.dyzjct.kura.gui.rewrite.gui.MelonHudEditor
import melon.utils.chat.ChatUtil

object ResetUiCommand : Command("resetui", arrayOf("reui"), "Reset ClickGui component positions") {
    init {
        builder.executor {
            MelonClickGui.resetUiComponentPositions()
            MelonHudEditor.resetUiComponentPositions()
            ChatUtil.sendMessage("Reset position successfully")
        }
    }
}