package dev.dyzjct.kura.module.hud

import dev.dyzjct.kura.KuraIdentifier
import base.utils.chat.ChatUtil
import com.mojang.blaze3d.systems.RenderSystem
import dev.dyzjct.kura.gui.clickgui.ClickGuiScreen
import dev.dyzjct.kura.gui.clickgui.HudEditorScreen
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.HUDModule
import dev.dyzjct.kura.module.modules.client.UiSetting
import dev.dyzjct.kura.module.modules.client.UiSetting.Theme
import dev.dyzjct.kura.module.modules.client.UiSetting.theme
import dev.dyzjct.kura.utils.animations.Easing
import net.minecraft.client.gui.DrawContext
import java.awt.Color

object Image : HUDModule(name = "Image", langName = "二次元小图片", category = Category.HUD, x = 150f, y = 150f) {
    val mode = msetting("Mode", Mode.Rimuru)
    private val scale by fsetting("Scale", 1.0f, 0.0f, 2.0f)
    private val animation by msetting("AnimationType", AnimationType.Center)

    var startTime = System.currentTimeMillis()

    override fun onRender(context: DrawContext) {
        val img = when (mode.value) {
            Mode.Rimuru -> rimuru
            Mode.Arona -> arona
            Mode.Roxy -> roxy
            Mode.Mahiru -> mahiru
            Mode.Gura -> gura
            Mode.Mikoto -> mikoto
            Mode.Miku -> miku
            Mode.Ayachinene -> ayachinene
            else -> mahiro
        }
        try {
            val progress =
                if (mc.currentScreen is ClickGuiScreen || mc.currentScreen is HudEditorScreen) Easing.IN_QUAD.inc(
                    Easing.toDelta(
                        startTime,
                        UiSetting.animationLength
                    )
                ) else Easing.IN_QUAD.dec(Easing.toDelta(startTime, UiSetting.animationLength))
            if (progress > 0.0f) {
                RenderSystem.disableBlend()
                width = 302f * scale
                height = 460f * scale
                val animationX =
                    if (animation == AnimationType.Center) x + ((302f - (302f * progress)) / 2) else if (animation == AnimationType.TopLeft || animation == AnimationType.DownLeft) x else x + (302f - (302f * progress))
                val animationY =
                    if (animation == AnimationType.Center) y + ((460f - (460f * progress)) / 2) else if (animation == AnimationType.TopLeft || animation == AnimationType.TopRight) y else y + (460f - (460f * progress))
                context.drawTexture(
                    img,
                    animationX.toInt(),
                    animationY.toInt(),
                    0F,
                    0F,
                    (302f * progress * scale).toInt(),
                    (460f * progress * scale).toInt(),
                    (302 * progress * scale).toInt(),
                    (460 * progress * scale).toInt()
                )
                RenderSystem.enableBlend()
            }
        } catch (e: Exception) {
            ChatUtil.sendNoSpamMessage("Image failed!!")
        }
    }

    private val rimuru = KuraIdentifier("textures/rimuru.png")
    private val arona = KuraIdentifier("textures/arona.png")
    private val mahiro = KuraIdentifier("textures/mahiro.png")
    private val mahiru = KuraIdentifier("textures/mahiru.png")
    private val roxy = KuraIdentifier("textures/roxy.png")
    private val gura = KuraIdentifier("textures/gura.png")
    private val mikoto = KuraIdentifier("textures/mikoto.png")
    private val ayachinene = KuraIdentifier("textures/ayachinene.png")
    private val miku = KuraIdentifier("textures/miku.png")

    @Suppress("UNUSED")
    enum class Mode {
        Rimuru, Arona, Mahiro, Roxy, Mahiru, Gura, Mikoto, Miku, Ayachinene
    }

    @Suppress("UNUSED")
    enum class AnimationType {
        TopLeft, TopRight, DownLeft, DownRight, Center
    }
}