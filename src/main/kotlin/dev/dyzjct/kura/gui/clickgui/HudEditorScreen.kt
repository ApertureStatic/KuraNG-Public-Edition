package dev.dyzjct.kura.gui.clickgui

import dev.dyzjct.kura.gui.clickgui.component.Panel
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.ModuleManager
import dev.dyzjct.kura.module.modules.client.HUDEditor

object HudEditorScreen : GuiScreen() {
    init {
        elements.add(
            Panel(
                ModuleManager.hUDModules,
                Category.HUD,
                this,
                5f,
                5f,
                105f,
                15f,
                DrawDelegateSelector.currentDrawDelegate
            )
        )

        container.addAll(elements)
    }

    override fun close() {
        super.close()
        HUDEditor.disable()
    }
}