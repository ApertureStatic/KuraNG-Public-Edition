package dev.dyzjct.kura.setting

import dev.dyzjct.kura.module.AbstractModule

class BooleanSetting(name: String, contain: dev.dyzjct.kura.module.AbstractModule?, defaultValue: Boolean) :
        Setting<Boolean>(name, contain, defaultValue), SettingVisibility<BooleanSetting>