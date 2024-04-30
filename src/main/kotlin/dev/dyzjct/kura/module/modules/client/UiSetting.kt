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
import dev.dyzjct.kura.setting.ModeSetting
import java.awt.Color


object UiSetting : Module(
    "NewUISetting",
    langName = "界面设置",
    description = "Color settings",
    category = Category.CLIENT,
    alwaysEnable = true
) {
    @kotlin.jvm.JvmStatic
    val disableSearch by bsetting("DisableSearch", false)

    //    Theme type
    val theme = msetting("Theme", Theme.Mikoto)


    //    Ui colors
    private val primaryColor = csetting("Primary", Color(240, 100, 255, 200)).enumIs(theme, Theme.Custom)
    private val secondaryColor = csetting("Secondary", Color(25, 25, 25, 200)).enumIs(theme, Theme.Custom)
    private val settingPanelColor = csetting("SettingPanel", Color(10, 10, 10, 200)).enumIs(theme, Theme.Custom)
    private val fillPanelTitle = bsetting("FillPanelTitle", true).enumIs(theme, Theme.Custom)
    private val panelBorder = bsetting("PanelBorder", true).enumIs(theme, Theme.Custom)
    private val targetcolor = csetting("TargetColor", Color(76, 179, 208, 150)).enumIs(theme, Theme.Custom)
    private val targethealthcolor = csetting("TargetHealthColor", Color(117, 39, 198,255)).enumIs(theme, Theme.Custom)

    //    SytRender Type
    private val sytRender by bsetting("SytRender", false).enumIs(theme, Theme.Custom)
    private val sytMode = msetting("SytMode", SytMode.Down).enumIs(theme, Theme.Custom)
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
    val searchDebug by bsetting("Search.Debug",false).isFalse { disableSearch }

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
        when (theme.value) {
            Theme.Rimuru -> {
                Image.mode.setValueByString("Rimuru")
                splashimg.equals("rimuru")
                splashtext.equals("Rimuru!!!")
                return ThemesSetting(
                    Color(76, 179, 208, 250),
                    Color(76, 179, 208, 250),
                    Color(10, 10, 10, 200),
                    fillPanelTitle = true,
                    panelBorder = true,
                    syt = true,
                    sytMode = SytMode.Down,
                    sytColor = Color(86, 190, 208, 140),
                    particle = true,
                    pRainbow = false,
                    pColor = Color(137, 185, 189, 100),
                    targetcolor =  Color(76, 179, 208, 250),
                    targethealthcolor =  Color(76, 179, 208, 250),
                )
            }

            Theme.Arona -> {
                Image.mode.setValueByString("Arona")
                splashimg.equals("arona")
                splashtext.equals("Arona!!!")
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
                    pColor = particleColor,
                    targetcolor =  Color(144, 204, 236, 250),
                    targethealthcolor =  Color(213, 236, 252, 140),
                )
            }

            Theme.Mahiro -> {
                Image.mode.setValueByString("Mahiro")
                splashimg.equals("mahiro")
                splashtext.equals("Mahiro!!!")
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
                    pColor = particleColor,
                    targetcolor = Color(245, 176, 166, 250),
                    targethealthcolor = Color(253, 235, 241, 200),
                )
            }

            Theme.Roxy -> {
                Image.mode.setValueByString("Roxy")
                splashimg.equals("roxy")
                splashtext.equals("Roxy!!!")
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
                    pColor = particleColor,
                    targetcolor = Color(117, 106, 171, 250),
                    targethealthcolor =  Color(89, 77, 89, 200),
                )
            }
            Theme.Mahiru -> {
                Image.mode.setValueByString("Mahiru")
                splashimg.equals("mahiru")
                splashtext.equals("Mahiru!!!")
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
                    pColor = particleColor,
                    Color(218, 165, 32, 200),
                    Color(254, 220, 189, 200),
                )
            }
            Theme.Gura -> {
            Image.mode.setValueByString("Gura")
            splashimg.equals("Gura")
            splashtext.equals("Gura!!!")
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
                pColor = particleColor,
                Color(51, 153, 189, 200),
                Color(204, 255, 255, 200),
            )
        }
            Theme.Mikoto -> {
                Image.mode.setValueByString("Mikoto")
                splashimg.equals("Mikoto")
                splashtext.equals("Mikoto!!!")
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
                    pColor = particleColor,
                    Color(109, 68, 55, 250),
                    Color(255, 255, 255, 150),
                )
            }
            Theme.Miku -> {
                Image.mode.setValueByString("Miku")
                splashimg.equals("Miku")
                splashtext.equals("Miku!!!")
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
                    pColor = particleColor,
                    Color(228, 142, 151, 250),
                    Color(187, 209, 248, 200),
                )
            }




            else -> {
                return ThemesSetting(
                    primaryColor.value,
                    secondaryColor.value,
                    settingPanelColor.value,
                    fillPanelTitle.value,
                    panelBorder.value,
                    sytRender,
                    sytMode.value as SytMode,
                    sytColor,
                    particle,
                    particleRainbow,
                    particleColor,
                    targetcolor.value,
                    targethealthcolor.value,
                )
            }
        }
    }

    val splashimg: String
        get() = theme.value.name.toLowerCase()

    val splashtext : String  get()= theme.value.name.capitalize()

    @JvmStatic
    fun splashimg(): String {
return splashimg
    }
    @JvmStatic
    fun splashtext(): String {
        return splashtext+"!!!"
    }
    @JvmStatic
    fun getsb(): ModeSetting<*> {
        return theme
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
        val pColor: Color,
        val targetcolor: Color,
        val targethealthcolor: Color,
    )

    enum class Theme {
        Custom, Rimuru, Mahiro, Arona, Roxy, Mahiru, Gura ,Mikoto ,Miku
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
