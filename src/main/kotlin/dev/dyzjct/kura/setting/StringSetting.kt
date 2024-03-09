package dev.dyzjct.kura.setting

import dev.dyzjct.kura.module.AbstractModule

class StringSetting(name: String, contain: dev.dyzjct.kura.module.AbstractModule?, defaultValue: String) : Setting<String>(name, contain, defaultValue),
    SettingVisibility<StringSetting>
