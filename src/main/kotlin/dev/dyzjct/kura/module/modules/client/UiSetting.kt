package dev.dyzjct.kura.module.modules.client

import dev.dyzjct.kura.gui.clickgui.AlphaAnimationDrawDelegate
import dev.dyzjct.kura.gui.clickgui.ClickGuiScreen
import dev.dyzjct.kura.gui.clickgui.HudEditorScreen
import dev.dyzjct.kura.gui.clickgui.animation.AnimationStrategy
import dev.dyzjct.kura.gui.clickgui.animation.NonAnimationStrategy
import dev.dyzjct.kura.gui.clickgui.animation.impl.AlphaAnimationStrategy
import dev.dyzjct.kura.gui.clickgui.animation.impl.ScalaAnimationStrategy
import dev.dyzjct.kura.gui.clickgui.render.Alignment
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.module.hud.Image
import dev.dyzjct.kura.setting.BooleanSetting
import java.awt.Color
import java.util.*


object UiSetting : Module(
    "NewUISetting",
    langName = "界面设置",
    description = "Color settings",
    category = Category.CLIENT,
    alwaysEnable = true
) {
    @JvmStatic
    val disableSearch by bsetting("DisableSearch", false)

    //    Theme type
    val theme by msetting("Theme", Theme.Rimuru)
    private val splashMode by msetting(
        "SplashMode",
        Splash.Rimuru
    ).isTrue { theme == Theme.Custom || theme == Theme.Ayachinene }


    //    Ui colors
    private val primaryColor by csetting("Primary", Color(240, 100, 255, 200)).enumIs(theme, Theme.Custom)
    private val secondaryColor by csetting("Secondary", Color(25, 25, 25, 200)).enumIs(theme, Theme.Custom)
    private val settingPanelColor by csetting("SettingPanel", Color(10, 10, 10, 200)).enumIs(theme, Theme.Custom)
    private val fillPanelTitle by bsetting("FillPanelTitle", true).enumIs(theme, Theme.Custom)
    private val panelBorder by bsetting("PanelBorder", true).enumIs(theme, Theme.Custom)

    //    SytRender Type
    private val sytRender by bsetting("SytRender", false).enumIs(theme, Theme.Custom)
    private val sytMode by msetting("SytMode", SytMode.Down).enumIs(theme, Theme.Custom)
    private val sytColor by csetting("SytColor", Color(255, 255, 255, 150)).enumIs(theme, Theme.Custom)

    //    Particle Type
    private val particle by bsetting("Particle", true).enumIs(theme, Theme.Custom)
    private val particleRainbow by bsetting("ParticleRainbow", true).isTrue { particle }.enumIs(theme, Theme.Custom)
    private val particleColor by csetting("ParticleColor", Color(255, 255, 255)).isTrue { particle }
        .isFalse { particleRainbow }
        .enumIs(theme, Theme.Custom)

    //    Animation type
    private val type0 = msetting("Type", AnimationType.NONE)
    val type by type0
    val animationLength by isetting("AnimationTime", 150, 100, 1000).enumIsNot(type0, AnimationType.NONE)
    val scalaDirection by msetting("Direction", Alignment.CENTER).isTrue { type == AnimationType.SCALA }
    val searchDebug by bsetting("Search.Debug", false).isFalse { disableSearch }

    init {
        type0.onChange<BooleanSetting> { value: Enum<*> ->
            ClickGuiScreen.animationStrategy = (value as AnimationType).createInstance()
            HudEditorScreen.animationStrategy = value.createInstance()
        }
    }

    fun getThemeSetting(): ThemesSetting {
        return newThemeSetting()
    }

    private fun newThemeSetting(): ThemesSetting {
        when (theme) {
            Theme.Rimuru -> {
                Image.mode.setValueByString("Rimuru")
                return ThemesSetting(
                    Color(76, 179, 208, 250),
                    Color(25, 25, 25, 200),
                    Color(76, 179, 208, 200),
                    fillPanelTitle = true,
                    panelBorder = true,
                    syt = true,
                    sytMode = SytMode.Down,
                    sytColor = Color(183, 245, 255, 55),
                    particle = true,
                    pRainbow = false,
                    pColor = Color(116, 245, 255, 100)
                )
            }

            Theme.Arona -> {
                Image.mode.setValueByString("Arona")
                return ThemesSetting(
                    Color(144, 204, 236, 250),
                    Color(213, 236, 252, 140),
                    Color(91, 128, 185, 200),
                    fillPanelTitle = true,
                    panelBorder = false,
                    syt = false,
                    sytMode = SytMode.Down,
                    sytColor = sytColor,
                    particle = particle,
                    pRainbow = particleRainbow,
                    pColor = particleColor
                )
            }

            Theme.Mahiro -> {
                Image.mode.setValueByString("Mahiro")
                return ThemesSetting(
                    Color(245, 176, 166, 250),
                    Color(253, 235, 241, 200),
                    Color(241, 219, 206, 200),
                    fillPanelTitle = true,
                    panelBorder = false,
                    syt = false,
                    sytMode = SytMode.Down,
                    sytColor = sytColor,
                    particle = particle,
                    pRainbow = particleRainbow,
                    pColor = particleColor
                )
            }

            Theme.Roxy -> {
                Image.mode.setValueByString("Roxy")
                return ThemesSetting(
                    Color(117, 106, 171, 250),
                    Color(89, 77, 89, 200),
                    Color(48, 39, 42, 200),
                    fillPanelTitle = true,
                    panelBorder = true,
                    syt = false,
                    sytMode = SytMode.Down,
                    sytColor = sytColor,
                    particle = particle,
                    pRainbow = particleRainbow,
                    pColor = particleColor
                )
            }

            Theme.Mahiru -> {
                Image.mode.setValueByString("Mahiru")
                return ThemesSetting(
                    Color(223, 194, 152, 250),
                    Color(254, 220, 189, 200),
                    Color(218, 165, 32, 200),
                    fillPanelTitle = true,
                    panelBorder = true,
                    syt = false,
                    sytMode = SytMode.Down,
                    sytColor = sytColor,
                    particle = particle,
                    pRainbow = particleRainbow,
                    pColor = particleColor
                )
            }

            Theme.Gura -> {
                Image.mode.setValueByString("Gura")
                return ThemesSetting(
                    Color(0, 128, 240, 250),
                    Color(51, 153, 189, 200),
                    Color(204, 255, 255, 200),
                    fillPanelTitle = true,
                    panelBorder = true,
                    syt = false,
                    sytMode = SytMode.Down,
                    sytColor = sytColor,
                    particle = particle,
                    pRainbow = particleRainbow,
                    pColor = particleColor
                )
            }

            Theme.Mikoto -> {
                Image.mode.setValueByString("Mikoto")
                return ThemesSetting(
                    Color(109, 68, 55, 250),
                    Color(124, 75, 71, 200),
                    Color(255, 255, 255, 150),
                    fillPanelTitle = true,
                    panelBorder = true,
                    syt = false,
                    sytMode = SytMode.Down,
                    sytColor = sytColor,
                    particle = particle,
                    pRainbow = particleRainbow,
                    pColor = particleColor
                )
            }

            Theme.Miku -> {
                Image.mode.setValueByString("Miku")
                return ThemesSetting(
                    Color(228, 142, 151, 250),
                    Color(187, 209, 248, 200),
                    Color(222, 126, 234, 150),
                    fillPanelTitle = true,
                    panelBorder = true,
                    syt = false,
                    sytMode = SytMode.Down,
                    sytColor = sytColor,
                    particle = particle,
                    pRainbow = particleRainbow,
                    pColor = particleColor
                )
            }

            Theme.Ayachinene -> {
                Image.mode.setValueByString("Ayachinene")
                return ThemesSetting(
                    Color(64, 68, 93, 250),
                    Color(165, 157, 178, 200),
                    Color(255, 255, 255, 200),
                    fillPanelTitle = true,
                    panelBorder = true,
                    syt = false,
                    sytMode = SytMode.Down,
                    sytColor = sytColor,
                    particle = particle,
                    pRainbow = particleRainbow,
                    pColor = particleColor
                )
            }


            else -> {
                return ThemesSetting(
                    primaryColor,
                    secondaryColor,
                    settingPanelColor,
                    fillPanelTitle,
                    panelBorder,
                    sytRender,
                    sytMode as SytMode,
                    sytColor,
                    particle,
                    particleRainbow,
                    particleColor
                )
            }
        }
    }

    @JvmStatic
    fun splashImg(): String {
        if (theme == Theme.Custom || theme == Theme.Ayachinene) return splashMode.name.lowercase(Locale.getDefault())
        return theme.name.lowercase(Locale.getDefault())
    }

    @JvmStatic
    fun getSlashText(): String {
        return when (theme) {
            Theme.Mikoto -> "Mikoto~"
            Theme.Miku -> "Miku~"
            Theme.Gura -> "Guwr Gura!"
            Theme.Mahiro -> "Mahiro~"
            Theme.Arona -> "Arona~"
            Theme.Rimuru -> "Rimuru Sama~"
            Theme.Roxy -> "Roxy KAMI!"
            Theme.Ayachinene -> "Ayachinene!"
            else -> "Kura Client!"
        }
    }

    data class ThemesSetting(
        val primary: Color,
        val secondary: Color,
        val setting: Color,
        val fillPanelTitle: Boolean,
        val panelBorder: Boolean,
        val syt: Boolean,
        val sytMode: SytMode,
        val sytColor: Color,
        val particle: Boolean,
        val pRainbow: Boolean,
        val pColor: Color
    )

    enum class Theme {
        Custom, Rimuru, Mahiro, Arona, Roxy, Mahiru, Gura, Mikoto, Miku, Ayachinene
    }

    @Suppress("UNUSED")
    enum class Splash {
        Custom, Rimuru, Mahiro, Arona, Roxy, Mahiru, Gura, Mikoto, Miku
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
