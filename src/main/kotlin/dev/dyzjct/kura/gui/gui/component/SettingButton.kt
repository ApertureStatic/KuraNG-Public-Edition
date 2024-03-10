package dev.dyzjct.kura.gui.gui.component

import dev.dyzjct.kura.setting.ModeSetting
import dev.dyzjct.kura.setting.Setting

abstract class SettingButton<T: Any> : Component() {
    lateinit var value: Setting<T>
    val asModeValue: ModeSetting<*> get() = value as ModeSetting<*>
}
