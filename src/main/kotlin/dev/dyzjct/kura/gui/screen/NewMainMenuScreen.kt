package dev.dyzjct.kura.gui.screen

import com.mojang.blaze3d.systems.RenderSystem
import dev.dyzjct.kura.Kura
import dev.dyzjct.kura.KuraIdentifier
import dev.dyzjct.kura.module.modules.client.MainMenu
import dev.dyzjct.kura.system.render.graphic.Render2DEngine
import dev.dyzjct.kura.system.render.newfont.FontRenderers
import dev.dyzjct.kura.utils.BlurRenderer
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen
import net.minecraft.client.gui.screen.option.OptionsScreen
import net.minecraft.client.gui.screen.world.SelectWorldScreen
import net.minecraft.text.Text
import net.minecraft.util.math.MathHelper
import java.awt.Color
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class NewMainMenuScreen : Screen(Text.literal(Kura.MOD_NAME + "-menu")) {
    private val mc = MinecraftClient.getInstance()

    private var buttonWidth = 80
    private var buttonHeight = 16

    private var lastCharTime: Long = 0
    private var pauseStartTime: Long = 0

    // Animation
    private val buttonAnimations: MutableMap<String, ButtonAnimation> = HashMap()
    private var isFirstRender = true
    private var titleAlpha = 0.0f
    private var openTime: Long = 0

    private var blurRenderer: BlurRenderer

    private class ButtonAnimation(var targetX: Float, var targetY: Float, var startX: Float, var startY: Float) {
        var hoverAlpha: Float = 0.0f
        var lastHoverTime: Long = System.currentTimeMillis()
        var isHovering: Boolean = false
    }

    init {
        blurRenderer = BlurRenderer()
    }

    override fun init() {
        super.init()

        openTime = System.currentTimeMillis()
        titleAlpha = 0.0f
        lastCharTime = System.currentTimeMillis()
        pauseStartTime = System.currentTimeMillis()

        buttonHeight = height / 9

        buttonAnimations["Singleplayer"] =
            ButtonAnimation(
                width * 0.235f,
                (height / 2f - 1.5f * buttonHeight),
                0f,
                height.toFloat()
            )
        buttonAnimations["Multiplayer"] =
            ButtonAnimation(
                width * 0.235f,
                (height / 2f - 0.5f * buttonHeight),
                0f,
                height.toFloat()
            )
        buttonAnimations["Game Settings"] =
            ButtonAnimation(
                width * 0.235f,
                (height / 2f + 0.5f * buttonHeight),
                0f,
                height.toFloat()
            )
        buttonAnimations["Quit Game"] =
            ButtonAnimation(
                width * 0.235f,
                (height / 2f + 1.5f * buttonHeight),
                0f,
                height.toFloat()
            )
    }

    override fun shouldPause(): Boolean {
        return false
    }

    override fun shouldCloseOnEsc(): Boolean {
        return false
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        if (isFirstRender) {
            openTime = System.currentTimeMillis()
            isFirstRender = false
        }

        val matrices = context.matrices
        val animProgress = animationProgress

        titleAlpha = min(1.0, (animProgress * 2.0f).toDouble()).toFloat()
        val textAlpha = min(1.0, (animProgress * 1.5f).toDouble()).toFloat()
        // 背景
        context.matrices.push()
        context.setShaderColor(1.0F, 1.0F, 1.0F, min(0.3f, animProgress))
        if (MainMenu.background.value == MainMenu.KuraBackground.Kura) {
            context.drawTexture(KuraIdentifier("background/background.png"), 0, 0, 0f, 0f, width, height, width, height)
        } else {
            context.drawTexture(KuraIdentifier("background/shuna_bg.png"), 0, 0, 0f, 0f, width, height, width, height)
        }
        context.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F)
        context.matrices.pop()

        for (i in 0..<width) {
            val color: Color = getRainbow(2L, 0.7f, 1.0f, 255, i * 5L)
            Render2DEngine.renderQuad(matrices, i.toFloat(), 0F, i + 1F, 1F, color.rgb, color.rgb, color.rgb, color.rgb)
        }

        val titleColor: Color = getRainbow(2L, 0.5f, 1.0f, (255 * titleAlpha).toInt(), width / 2 * 5L)

        context.matrices.push()
        fun draw() {
            context.setShaderColor(1.0F, 1.0F, 1.0F, min(0.3f, animProgress))
            Render2DEngine.drawRound(
                context.matrices,
                width * 0.1f,
                height * 0.15f,
                width / 3.7f,
                height.toFloat() * 0.67f,
                10f,
                Color(0, 0, 0, 160)
            )
            Render2DEngine.drawRectBlurredShadow(
                context.matrices,
                width * 0.1f,
                height * 0.15f,
                width / 3.7f,
                height.toFloat() * 0.67f,
                20,
                Color(0, 0, 0, 200)
            )
            context.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F)
        }
        draw()
        context.matrices.pop()
//TODO LOGO DRAWER
        //todo 必须根据同意变量计算 防止出现偏移
        context.matrices.push()
        context.setShaderColor(1.0F, 1.0F, 1.0F, min(0.3f, animProgress))
        context.drawTexture(
            KuraIdentifier("logo/logo.png"),
            (width * 0.143).toInt(), (height / 2f - 3.1f * buttonHeight).toInt(),
            0f, 0f,
            (width / 10), (width / 10),
            (width / 10), (width / 10)
        )
        context.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F)
        context.matrices.pop()

        context.matrices.push()
        drawTextEpsilon(
            context,
            "ura",
            (width * 0.2256f),
            (width / 7.9f),
            width * 0.0014f,
            titleColor
        )

        val date = SimpleDateFormat("MM/dd/yy").format(Date()) + " " + SimpleDateFormat("hh:mm a", Locale.US).format(
            Date()
        )
        val timeColor = Color(Color.BLACK.red, Color.BLACK.green, Color.BLACK.blue, (255 * textAlpha).toInt())
        drawText(context, date, width / 2f - FontRenderers.default.getStringWidth(date) / 2f, 6f, 1f, timeColor)

        val versionColor = Color(Color.BLACK.red, Color.BLACK.green, Color.BLACK.blue, (255 * textAlpha).toInt())
        drawText(
            context,
            Kura.MOD_NAME + " " + Kura.VERSION + "-mc" + "1.20.4" + ".",
            2f,
            height - FontRenderers.default.getFontHeight("Kura") - 1,
            1f,
            versionColor
        )

        drawAnimatedButton(context, "Singleplayer", mouseX, mouseY, animProgress)
        drawAnimatedButton(context, "Multiplayer", mouseX, mouseY, animProgress)
        drawAnimatedButton(context, "Game Settings", mouseX, mouseY, animProgress)
        drawAnimatedButton(context, "Quit Game", mouseX, mouseY, animProgress)

        drawClientStatus(context, textAlpha)
        updateHoverStates(mouseX, mouseY)

        if (MainMenu.rimuru) {
            RenderSystem.disableBlend()
            val imgWidth = width / 4
            val imgHeight = imgWidth * 1.52
            val animationX = (width) - (imgWidth * animProgress).toInt()
            val animationY = (height) - (imgHeight * animProgress).toInt()
            context.drawTexture(
                rimuru,
                animationX,
                animationY,
                0F,
                0F,
                (imgWidth * animProgress).toInt(),
                (imgHeight * animProgress).toInt(),
                (imgWidth * animProgress).toInt(),
                (imgHeight * animProgress).toInt()
            )
            RenderSystem.enableBlend()
        }
    }

    private val animationProgress: Float
        get() {
            val elapsed = (System.currentTimeMillis() - openTime).toFloat()
            return min(1.0, (elapsed / ANIMATION_DURATION).toDouble()).toFloat()
        }

    private fun drawAnimatedButton(context: DrawContext, text: String, mouseX: Int, mouseY: Int, animProgress: Float) {
        val anim = buttonAnimations[text] ?: return

        val progress = easeOutCubic(animProgress)
        val currentX = anim.startX + (anim.targetX - anim.startX) * progress
        val currentY = anim.startY + (anim.targetY - anim.startY) * progress

        val currentTime = System.currentTimeMillis()
        val hoverDelta = (currentTime - anim.lastHoverTime) / 200.0f // 200ms transition
        anim.lastHoverTime = currentTime

        if (anim.isHovering) {
            anim.hoverAlpha = min(1.0, (anim.hoverAlpha + hoverDelta).toDouble()).toFloat()
        } else {
            anim.hoverAlpha = max(0.0, (anim.hoverAlpha - hoverDelta).toDouble()).toFloat()
        }

        val bgColor = Color(255, 255, 255, 100 + (100 * anim.hoverAlpha).toInt())
        val textColor = Color(
            0,
            0,
            0,
            200 + (30 * anim.hoverAlpha).toInt()
        )

        buttonWidth = width / 5
        buttonHeight = height / 9

        Render2DEngine.renderQuad(
            context.matrices,
            currentX - buttonWidth / 2f,
            currentY,
            currentX + buttonWidth / 2f,
            currentY + buttonHeight,
            bgColor.rgb,
            bgColor.rgb,
            bgColor.rgb,
            bgColor.rgb
        )
        drawText(
            context,
            text,
            currentX - FontRenderers.default.getStringWidth(text) / 2f,
            currentY + buttonHeight / 2.1f,
            width / mc.window.scaledWidth.toFloat(),
            textColor
        )
    }

    private fun updateHoverStates(mouseX: Int, mouseY: Int) {
        for ((_, anim) in buttonAnimations) {
            anim.isHovering =
                isHoveringButton(anim.targetX.toDouble(), anim.targetY.toDouble(), mouseX.toDouble(), mouseY.toDouble())
        }
    }

    private fun easeOutCubic(x: Float): Float {
        return 1 - (1 - x).toDouble().pow(3.0).toFloat()
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == 0) {
            val singleplayerAnim = buttonAnimations["Singleplayer"]
            if (singleplayerAnim != null && isHoveringButton(
                    singleplayerAnim.targetX.toDouble(),
                    singleplayerAnim.targetY.toDouble(),
                    mouseX,
                    mouseY
                )
            ) {
                mc.setScreen(SelectWorldScreen(this))
                playClickSound()
            }

            val multiplayerAnim = buttonAnimations["Multiplayer"]
            if (multiplayerAnim != null && isHoveringButton(
                    multiplayerAnim.targetX.toDouble(),
                    multiplayerAnim.targetY.toDouble(),
                    mouseX,
                    mouseY
                )
            ) {
                mc.setScreen(MultiplayerScreen(this))
                playClickSound()
            }

            val settingsAnim = buttonAnimations["Game Settings"]
            if (settingsAnim != null && isHoveringButton(
                    settingsAnim.targetX.toDouble(),
                    settingsAnim.targetY.toDouble(),
                    mouseX,
                    mouseY
                )
            ) {
                mc.setScreen(OptionsScreen(this, mc.options))
                playClickSound()
            }

            val quitAnim = buttonAnimations["Quit Game"]
            if (quitAnim != null && isHoveringButton(
                    quitAnim.targetX.toDouble(),
                    quitAnim.targetY.toDouble(),
                    mouseX,
                    mouseY
                )
            ) {
                mc.scheduleStop()
                playClickSound()
            }
        }

        return super.mouseClicked(mouseX, mouseY, button)
    }

    private fun drawClientStatus(context: DrawContext, alpha: Float) {
        var primaryText = ""
        var secondaryText = ""
        var baseColor = Color.WHITE

        if (primaryText.isEmpty()) return

        val color = Color(baseColor.red, baseColor.green, baseColor.blue, (baseColor.alpha * alpha).toInt())

        if (!secondaryText.isEmpty()) drawText(
            context,
            secondaryText,
            width / 2f - FontRenderers.default.getStringWidth(secondaryText) / 2f,
            (height - 30).toFloat(),
            1f,
            color
        )
        drawText(
            context,
            primaryText,
            width / 2f - FontRenderers.default.getStringWidth(primaryText) / 2f,
            (height - 20).toFloat(),
            1f,
            color
        )
    }

    fun isHoveringButton(x: Double, y: Double, mouseX: Double, mouseY: Double): Boolean {
        return x - buttonWidth / 2f <= mouseX && y <= mouseY && x + buttonWidth / 2f > mouseX && y + buttonHeight > mouseY
    }

    private fun drawText(context: DrawContext, text: String, x: Float, y: Float, scale: Float, color: Color) {
        context.matrices.push()
        context.matrices.translate(x, y, 0f)
        context.matrices.scale(scale, scale, 0f)
        FontRenderers.default.drawString(context.matrices, text, 0.0, 0.0, color.rgb)
        context.matrices.pop()
    }

    private fun drawTextEpsilon(context: DrawContext, text: String, x: Float, y: Float, scale: Float, color: Color) {
        context.matrices.push()
        context.matrices.translate(x, y, 0f)
        context.matrices.scale(scale, scale, 0f)
        FontRenderers.epsilon.drawString(context.matrices, text, 0.0, 0.0, color.rgb)
        context.matrices.pop()
    }

    private fun playClickSound() {
        blurRenderer.close()
//        mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvent.UI_BUTTON_CLICK, 1.0f))
    }

    companion object {
        private const val ANIMATION_DURATION = 800.0f
    }

    fun getRainbow(speed: Long, saturation: Float, brightness: Float, alpha: Int, index: Long): Color {
        var speed = speed
        speed = MathHelper.clamp(speed, 1, 20)

        val hue =
            ((System.currentTimeMillis() + index) % (10500 - (500 * speed))) / (10500.0f - (500.0f * speed.toFloat()))
        val color = Color(
            Color.HSBtoRGB(
                MathHelper.clamp(hue, 0.0f, 1.0f),
                MathHelper.clamp(saturation, 0.0f, 1.0f),
                MathHelper.clamp(brightness, 0.0f, 1.0f)
            )
        )

        return Color(color.red, color.green, color.blue, alpha)
    }

    private val rimuru = KuraIdentifier("textures/rimuru.png")
}
