package dev.dyzjct.kura.module.modules.client

import dev.dyzjct.kura.gui.rewrite.gui.AlphaAnimationDrawDelegate
import dev.dyzjct.kura.gui.rewrite.gui.MelonClickGui
import dev.dyzjct.kura.gui.rewrite.gui.MelonHudEditor
import dev.dyzjct.kura.gui.rewrite.gui.animation.AnimationStrategy
import dev.dyzjct.kura.gui.rewrite.gui.animation.NonAnimationStrategy
import dev.dyzjct.kura.gui.rewrite.gui.animation.impl.AlphaAnimationStrategy
import dev.dyzjct.kura.gui.rewrite.gui.animation.impl.ScalaAnimationStrategy
import dev.dyzjct.kura.gui.rewrite.gui.render.Alignment
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.setting.BooleanSetting
import dev.dyzjct.kura.setting.Setting
import java.awt.Color

object UiSetting : Module(
    "NewUISetting",
    langName = "新的界面设置",
    description = "Color settings",
    category = Category.CLIENT,
    alwaysEnable = true
) {
    val enableNewUi by bsetting("BetaGUI", false)
    val disableSearch by bsetting("DisableSearch", false)

    //    Theme type
    val theme = msetting("Theme", Theme.Custom)
    private var themeSettings: ThemesSetting? = null

    //    Ui colors
    private val primaryColor = csetting("Primary", Color(240, 100, 255, 200)).enumIs(theme, Theme.Custom)
    private val secondaryColor = csetting("Secondary", Color(25, 25, 25, 200)).enumIs(theme, Theme.Custom)
    private val settingPanelColor = csetting("SettingPanel", Color(10, 10, 10, 200)).enumIs(theme, Theme.Custom)
    private val rectBlur = bsetting("RectBlur", false).enumIs(theme, Theme.Custom)
    private val fillPanelTitle = bsetting("FillPanelTitle", true).enumIs(theme, Theme.Custom)
    private val panelBorder = bsetting("PanelBorder", true).enumIs(theme, Theme.Custom)
    private val rounded = bsetting("Rounded", true).enumIs(theme, Theme.Custom)

    //    Animation type
    private val type0 = msetting("Type", AnimationType.NONE)
    val type by type0
    val animationLength by isetting("AnimationTime", 150, 100, 1000).enumIsNot(type0, AnimationType.NONE)

    val scalaDirection by msetting("Direction", Alignment.CENTER).isTrue { type == AnimationType.SCALA }

    enum class AnimationType(
        val createInstance: () -> AnimationStrategy
    ) {
        NONE({ NonAnimationStrategy }), EASE({ AlphaAnimationStrategy(AlphaAnimationDrawDelegate()) }), SCALA({ ScalaAnimationStrategy() })
    }

    init {
        type0.onChange<BooleanSetting> { value: Enum<*> ->
            MelonClickGui.animationStrategy = (value as AnimationType).createInstance()
            MelonHudEditor.animationStrategy = value.createInstance()
        }

        onAnySettingChange(
            listOf(
                theme,
                primaryColor,
                secondaryColor,
                settingPanelColor,
                rectBlur,
                fillPanelTitle,
                panelBorder,
                rounded
            )
        ) {
            themeSettings = newThemeSetting()
        }
    }

    private fun onAnySettingChange(settings: List<Setting<*>>, block: () -> Unit) {
        settings.forEach {
            it.onChange<Setting<*>> { _ ->
                block()
            }
        }
    }

    fun getThemeSetting(): ThemesSetting {
        if (themeSettings == null) {
            themeSettings = newThemeSetting()
        }

        return themeSettings!!
    }

    private fun newThemeSetting(): ThemesSetting {
        return when (theme.value) {
            Theme.Arona -> {
                ThemesSetting(
                    Color(144, 204, 236, 250),
                    Color(213, 236, 252, 140),
                    rect = true,
                    Color(91, 128, 185, 200),
                    fillPanelTitle = true,
                    panelBorder = false,
                    rounded = false
                )
            }

            Theme.Mahiro -> {
                ThemesSetting(
                    Color(245, 176, 166, 250),
                    Color(253, 235, 241, 200),
                    true,
                    Color(241, 219, 206, 200),
                    fillPanelTitle = true,
                    panelBorder = false,
                    rounded = false
                )
            }

            Theme.Roxy -> {
                ThemesSetting(
                    Color(117, 106, 171, 250),
                    Color(89, 77, 89, 200),
                    true,
                    Color(48, 39, 42, 200),
                    fillPanelTitle = true,
                    panelBorder = true,
                    rounded = false
                )
            }

            else -> {
                ThemesSetting(
                    primaryColor.value,
                    secondaryColor.value,
                    rectBlur.value,
                    settingPanelColor.value,
                    fillPanelTitle.value,
                    panelBorder.value,
                    rounded.value
                )
            }
        }
    }

    data class ThemesSetting(
        val primary: Color,
        val secondary: Color,
        val rect: Boolean,
        val setting: Color,
        val fillPanelTitle: Boolean,
        val panelBorder: Boolean,
        val rounded: Boolean
    )

    enum class Theme {
        Custom, Mahiro, Arona, Roxy
    }
}