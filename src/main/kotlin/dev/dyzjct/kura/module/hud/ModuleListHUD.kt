package dev.dyzjct.kura.module.hud

import dev.dyzjct.kura.gui.clickgui.guis.HUDEditorScreen
import dev.dyzjct.kura.gui.rewrite.gui.MelonHudEditor
import dev.dyzjct.kura.module.HUDModule
import dev.dyzjct.kura.module.ModuleManager
import dev.dyzjct.kura.module.modules.client.ClickGui
import dev.dyzjct.kura.module.modules.client.Colors
import dev.dyzjct.kura.utils.animations.Easing
import base.events.ModuleEvent
import base.events.TickEvent
import base.system.event.listener
import base.system.event.safeConcurrentListener
import base.system.event.safeEventListener
import base.system.render.graphic.Render2DEngine
import base.system.render.newfont.FontRenderers
import base.system.util.state.TimedFlag
import base.utils.chat.ChatUtil
import net.minecraft.client.gui.DrawContext
import java.awt.Color

object ModuleListHUD : HUDModule(name = "ModuleListHUD", langName = "功能列表") {
    private var drawBG = bsetting("BlurBackground", true)
    private var sideLine = bsetting("SideLine", false)
    private var sideLineWidth = dsetting("SideWidth", 1.0, 1.0, 5.0).isTrue(sideLine)
    private var sync = bsetting("SyncLineColor", false)
    private var sideColor = csetting("SideLineColor", Color(231, 13, 103)).isTrue(sideLine).isFalse(sync)
    private var mode = msetting("Mode", Mode.Rainbow)
    private var speed by isetting("Speed", 18, 2, 54).enumIs(mode, Mode.Rainbow)
    private var saturation by fsetting("Saturation", 0.65f, 0.0f, 1.0f).enumIs(mode, Mode.Rainbow)
    private var brightness by fsetting("Brightness", 1.0f, 0.0f, 1.0f).enumIs(mode, Mode.Rainbow)
    private var color = csetting("ListColor", Color(231, 13, 103)).enumIs(mode, Mode.Custom)

    private class ModuleRenderer(
        val module: dev.dyzjct.kura.module.AbstractModule
    ) : TimedFlag<Boolean>(module.isEnabled) {
        val moduleWidth: Float
            get() = getStringWidth(module.getArrayList())
        var y = -1f
        private var targetY = 0f

        val progress: Float
            get() = if (value) {
                Easing.OUT_CUBIC.inc(Easing.toDelta(lastUpdateTime, 300f))
            } else {
                Easing.IN_CUBIC.dec(Easing.toDelta(lastUpdateTime, 300f))
            }

        val isPlayDone: Boolean
            get() = (value && progress == 1f) || (!value && progress == 0f)

        fun update() {
            value = module.isEnabled && module.isVisible
        }

        fun forceSetY(y: Float) {
            this.y = y
        }

        fun onRender(context: DrawContext, x: Float, y1: Float) {
            if (targetY != y1) {
                targetY = y1
            }

            if (y != targetY) {
                y = if (y > targetY) {
                    (y - 1).coerceAtLeast(targetY)
                } else {
                    (y + 1).coerceAtMost(targetY)
                }
            }

            val animationXOffset =
                if (reverseX) moduleWidth - moduleWidth * progress else -moduleWidth + moduleWidth * progress
            val sideLinePadding = if (sideLine.value) sideLineWidth.value.toFloat() + 1f else 0f
            val xOffset =
                animationXOffset + (if (reverseX) width - sideLinePadding - moduleWidth else sideLinePadding).toFloat()
            val alphaProgress = progress.coerceIn(0.1f, 0.99f)

            if (drawBG.value) {
                Render2DEngine.drawRect(
                    context.matrices,
                    x - 1f + xOffset,
                    y,
                    moduleWidth + 2f,
                    10f,
                    Color(0, 0, 0, 70).setAlphaWithProgress(alphaProgress)
                )
            }

            if (sideLine.value) {
                val sidelineOffsetX = if (reverseX) {
                    moduleWidth + 1f
                } else {
                    -sideLineWidth.value.toFloat() - 1f
                }

                Render2DEngine.drawRect(
                    context.matrices,
                    x + sidelineOffsetX + xOffset,
                    y,
                    sideLineWidth.value.toFloat(),
                    10f,
                    if (sync.value) generateColor() else sideColor.value.setAlphaWithProgress(alphaProgress)
                )
            }

            val color = generateColor().setAlphaWithProgress(alphaProgress)
            if (ClickGui.chinese.value) {
                FontRenderers.cn.drawString(
                    context.matrices, module.getArrayList(), x + xOffset, y + 2, color.rgb, false
                )
            } else {
                FontRenderers.lexend.drawString(
                    context.matrices, module.getArrayList(), x + xOffset, y + 3, color.rgb, false
                )
            }
        }
    }

    private val moduleList = mutableListOf<ModuleRenderer>()

    private var reverseX = false
    private var reverseY = false
    private var initCheck = false

    override var height: Float = 20f

    override var width: Float = 60f

    init {
        fun tryAddModule(module: dev.dyzjct.kura.module.AbstractModule) {
            if (module.isEnabled) {
                if (!module.isVisible || module.isHidden()) {
                    return
                }

                if (!moduleList.any { it.module == module }) {
                    moduleList.add(ModuleRenderer(module))
                }
            }
        }

        listener<ModuleEvent.Toggle> {
            tryAddModule(it.module)
        }

        safeEventListener<ModuleEvent.VisibleChange> {
            tryAddModule(it.module)
        }

        safeConcurrentListener<TickEvent.Pre> {
            if (moduleList.isNotEmpty() && !initCheck) {
                runCatching {
                    for (i in moduleList.indices) {
                        if (i - 1 < 0) continue
                        if (moduleList[i - 1].module.getArrayList() == moduleList[i].module.getArrayList()) {
                            ChatUtil.sendWarnMessage("[ModuleListHUD] ${moduleList[i].module.moduleName} Has Been Removed Duplicate In ArrayList.")
                            moduleList.removeAt(i)
                        }
                    }
                }
                initCheck = true
            }
        }
    }

    private var firstToRun = true

    override fun onRender(context: DrawContext) {
        if (firstToRun) {
            ModuleManager.moduleList.filter { it.isVisible && !it.isHidden() && it.isEnabled }
                .forEach { moduleList.add(ModuleRenderer(it)) }
            firstToRun = false
        }

        moduleList.forEach(ModuleRenderer::update)

        if (mc.currentScreen is HUDEditorScreen || mc.currentScreen is MelonHudEditor || !reverseX || !reverseY) {
            reverseX = x > mc.window.scaledWidth / 2
            reverseY = y > mc.window.scaledHeight / 2
        }

        moduleList.sortBy {
            if (reverseY) {
                it.moduleWidth
            } else {
                it.moduleWidth * -1
            }
        }

        moduleList.forEachIndexed { index, moduleRenderer ->
            var y1 = y + index * 10f
            if (reverseY) {
                y1 -= moduleList.size * 10f - 20f
            }
            if (moduleRenderer.y == -1f || dragging) {
                moduleRenderer.forceSetY(y1)
            }
            moduleRenderer.onRender(context, x, y1)
        }

        moduleList.removeIf { it.isPlayDone && !it.value }
    }

    override fun rearrange() {
        moduleList.forEachIndexed { index, moduleRenderer ->
            var y1 = y + index * 10f
            if (reverseY) {
                y1 -= moduleList.size * 10f - 20f
            }
            moduleRenderer.forceSetY(y1)
        }
    }

    private fun getStringWidth(text: String): Float {
        return if (ClickGui.chinese.value) {
            mc.textRenderer.getWidth(text).toFloat()
        } else {
            FontRenderers.lexend.getStringWidth(text)
        }
    }

    private fun generateColor(): Color {
        val fontColor = Colors.hudColor.value
        when (mode.value) {
            Mode.Rainbow -> return Render2DEngine.rainbow(speed, 0, saturation, brightness, 1f)
            Mode.GuiSync -> return fontColor
            Mode.Custom -> return color.value
        }
        return Color.WHITE
    }

    private fun Color.setAlphaWithProgress(progress: Float): Color {
        return Color(red, green, blue, (alpha * progress).toInt())
    }

    enum class Mode {
        Rainbow, GuiSync, Custom
    }
}