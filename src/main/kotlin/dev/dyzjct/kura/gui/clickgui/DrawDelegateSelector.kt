package dev.dyzjct.kura.gui.clickgui

import dev.dyzjct.kura.gui.clickgui.render.DrawDelegate

object DrawDelegateSelector {
    private val listeners = mutableListOf<(DrawDelegate) -> Unit>()

    var currentDrawDelegate: DrawDelegate = DefaultDrawDelegate
        private set(value) {
            field = value
            listeners.forEach { it(value) }
        }

    fun changeDrawDelegate(drawDelegate: DrawDelegate) {
        currentDrawDelegate = drawDelegate
    }

    fun listenerChange(callback: (DrawDelegate) -> Unit) {
        listeners.add(callback)
    }
}