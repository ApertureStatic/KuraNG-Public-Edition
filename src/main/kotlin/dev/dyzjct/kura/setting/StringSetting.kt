package dev.dyzjct.kura.setting

import dev.dyzjct.kura.module.AbstractModule

class StringSetting(name: String, contain: AbstractModule?, defaultValue: String) : Setting<String>(name, contain, defaultValue),
    SettingVisibility<StringSetting>
