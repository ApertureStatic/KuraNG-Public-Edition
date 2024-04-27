package dev.dyzjct.kura.command.impl

import base.utils.chat.ChatUtil
import dev.dyzjct.kura.command.Command
import dev.dyzjct.kura.gui.clickgui.ClickGuiScreen
import dev.dyzjct.kura.gui.clickgui.HudEditorScreen

object ResetUiCommand : Command("resetui", arrayOf("reui"), "Reset ClickGui component positions") {
    init {
        builder.executor {
            ClickGuiScreen.resetUiComponentPositions()
            HudEditorScreen.resetUiComponentPositions()
            ChatUtil.sendMessage("Reset position successfully")
        }
    }
}