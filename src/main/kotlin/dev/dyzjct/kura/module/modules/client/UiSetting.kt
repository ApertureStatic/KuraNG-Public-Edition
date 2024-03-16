package dev.dyzjct.kura.module.modules.client

import dev.dyzjct.kura.gui.clickgui.AlphaAnimationDrawDelegate
import dev.dyzjct.kura.gui.clickgui.HudEditorScreen
import dev.dyzjct.kura.gui.clickgui.animation.AnimationStrategy
import dev.dyzjct.kura.gui.clickgui.animation.NonAnimationStrategy
import dev.dyzjct.kura.gui.clickgui.animation.impl.AlphaAnimationStrategy
import dev.dyzjct.kura.gui.clickgui.animation.impl.ScalaAnimationStrategy
import dev.dyzjct.kura.gui.clickgui.render.Alignment
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.setting.BooleanSetting
import dev.dyzjct.kura.setting.Setting
import java.awt.Color

object UiSetting : Module(
    "NewUISetting",
    langName = "界面设置",
    description = "Color settings",
    category = Category.CLIENT,
    alwaysEnable = true
) {
    val disableSearch by bsetting("DisableSearch", false)

    //    Theme type
    private val theme = msetting("Theme", Theme.Custom)
    private var themeSettings: ThemesSetting? = null

    //    Ui colors
    private val primaryColor = csetting("Primary", Color(240, 100, 255, 200)).enumIs(theme, Theme.Custom)
    private val secondaryColor = csetting("Secondary", Color(25, 25, 25, 200)).enumIs(theme, Theme.Custom)
    private val settingPanelColor = csetting("SettingPanel", Color(10, 10, 10, 200)).enumIs(theme, Theme.Custom)
    private val fillPanelTitle = bsetting("FillPanelTitle", true).enumIs(theme, Theme.Custom)
    private val panelBorder = bsetting("PanelBorder", true).enumIs(theme, Theme.Custom)
    private val rounded = bsetting("Rounded", true).enumIs(theme, Theme.Custom)

    //    SytRender Type
    val sytRender by bsetting("SytRender", false).enumIs(theme, Theme.Custom)
    val sytMode = msetting("SytMode", SytMode.Down).enumIs(theme, Theme.Custom)
    val sytColor by csetting("SytColor", Color(255, 255, 255, 150)).enumIs(theme, Theme.Custom)

    //    Particle Type
    val particle by bsetting("Particle", true).enumIs(theme, Theme.Custom)
    val particleRainbow by bsetting("ParticleRainbow", true).isTrue { particle }.enumIs(theme, Theme.Custom)
    val particleColor by csetting("ParticleColor", Color(255, 255, 255)).isTrue { particle }.isFalse { particleRainbow }
        .enumIs(theme, Theme.Custom)

    //    Animation type
    private val type0 = msetting("Type", AnimationType.NONE)
    val type by type0
    val animationLength by isetting("AnimationTime", 150, 100, 1000).enumIsNot(type0, AnimationType.NONE)

    val scalaDirection by msetting("Direction", Alignment.CENTER).isTrue { type == AnimationType.SCALA }

    init {
        type0.onChange<BooleanSetting> { value: Enum<*> ->
            dev.dyzjct.kura.gui.clickgui.ClickGuiScreen.animationStrategy = (value as AnimationType).createInstance()
            HudEditorScreen.animationStrategy = value.createInstance()
        }

        onAnySettingChange(
            listOf(
                theme,
                primaryColor,
                secondaryColor,
                settingPanelColor,
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
            Theme.Rimuru -> {
                ThemesSetting(
                    Color(76, 179, 208, 250),
                    Color(25, 25, 25, 200),
                    Color(10, 10, 10, 200),
                    fillPanelTitle = true,
                    panelBorder = true,
                    rounded = false,
                    syt = true,
                    sytMode = SytMode.Down,
                    sytColor = Color(33, 97, 108, 100),
                    particle = true,
                    pRainbow = false,
                    pColor = Color(137, 185, 189, 100),
                )
            }

            Theme.Arona -> {
                ThemesSetting(
                    Color(144, 204, 236, 250),
                    Color(213, 236, 252, 140),
                    Color(91, 128, 185, 200),
                    fillPanelTitle = true,
                    panelBorder = false,
                    rounded = false,
                    syt = false,
                    sytMode = SytMode.Down,
                    sytColor = sytColor,
                    particle = particle,
                    pRainbow = particleRainbow,
                    pColor = particleColor,
                )
            }

            Theme.Mahiro -> {
                ThemesSetting(
                    Color(245, 176, 166, 250),
                    Color(253, 235, 241, 200),
                    Color(241, 219, 206, 200),
                    fillPanelTitle = true,
                    panelBorder = false,
                    rounded = false,
                    syt = false,
                    sytMode = SytMode.Down,
                    sytColor = sytColor,
                    particle = particle,
                    pRainbow = particleRainbow,
                    pColor = particleColor,
                )
            }

            Theme.Roxy -> {
                ThemesSetting(
                    Color(117, 106, 171, 250),
                    Color(89, 77, 89, 200),
                    Color(48, 39, 42, 200),
                    fillPanelTitle = true,
                    panelBorder = true,
                    rounded = false,
                    syt = false,
                    sytMode = SytMode.Down,
                    sytColor = sytColor,
                    particle = particle,
                    pRainbow = particleRainbow,
                    pColor = particleColor,
                )
            }

            else -> {
                ThemesSetting(
                    primaryColor.value,
                    secondaryColor.value,
                    settingPanelColor.value,
                    fillPanelTitle.value,
                    panelBorder.value,
                    rounded.value,
                    sytRender,
                    SytMode.Down,
                    sytColor,
                    particle,
                    particleRainbow,
                    particleColor,
                )
            }
        }
    }

    data class ThemesSetting(
        val primary: Color,
        val secondary: Color,
        val setting: Color,
        val fillPanelTitle: Boolean,
        val panelBorder: Boolean,
        val rounded: Boolean,
        val syt: Boolean,
        val sytMode: SytMode,
        val sytColor: Color,
        val particle: Boolean,
        val pRainbow: Boolean,
        val pColor: Color
    )

    enum class Theme {
        Custom, Rimuru, Mahiro, Arona, Roxy
    }

    enum class SytMode {
        Top, Down
    }

    @Suppress("UNUSED")
    enum class AnimationType(
        val createInstance: () -> AnimationStrategy
    ) {
        NONE({ NonAnimationStrategy }), EASE({ AlphaAnimationStrategy(AlphaAnimationDrawDelegate()) }), SCALA(
            { ScalaAnimationStrategy() })
    }
}