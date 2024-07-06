package dev.dyzjct.kura.module.modules.render

import base.utils.math.toBox
import dev.dyzjct.kura.manager.BlockFinderManager
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.system.render.graphic.Render3DEngine
import net.minecraft.block.Blocks
import java.awt.Color

object ChestESP : Module(
    name = "ChestESP",
    langName = "箱子透视",
    description = "Displays the location of chests in the world",
    category = Category.RENDER
) {
    val distance by isetting("Distance", 25, 1, 80)
    private val chest by bsetting("Chset", false)
    private val shunlker by bsetting("ShulkerBox", false)
    private val ender by bsetting("EnderChest", false)
    val wmBypass = bsetting("WhiteMysteryBypass", false)
    val rotate by bsetting("Rotate", false).isTrue(wmBypass)
    val clickDelay by isetting("ClickDelay", 1000, 0, 1000).isTrue(wmBypass)
    private var lineWidth by fsetting("LineWidth", 1.5f, 0f, 5f)
    private val lineAlpha by isetting("LineAlpha", 200, 1, 255)
    private val fillAlpha by isetting("FillAlpha", 200, 1, 255)

    init {
        onRender3D { event ->
            BlockFinderManager.espBlockList.forEach { blockPos ->
                // 我靠！
                val color = when (world.getBlockState(blockPos).block) {
                    Blocks.CHEST -> if (chest) Color(255, 165, 0, 200) else null
                    Blocks.SHULKER_BOX -> if (shunlker) Color(199, 21, 133, 200) else null
                    Blocks.RED_SHULKER_BOX -> if (shunlker) Color(199, 21, 133, 200) else null
                    Blocks.BLACK_SHULKER_BOX -> if (shunlker) Color(199, 21, 133, 200) else null
                    Blocks.WHITE_SHULKER_BOX -> if (shunlker) Color(199, 21, 133, 200) else null
                    Blocks.BLUE_SHULKER_BOX -> if (shunlker) Color(199, 21, 133, 200) else null
                    Blocks.BROWN_SHULKER_BOX -> if (shunlker) Color(199, 21, 133, 200) else null
                    Blocks.CYAN_SHULKER_BOX -> if (shunlker) Color(199, 21, 133, 200) else null
                    Blocks.GRAY_SHULKER_BOX -> if (shunlker) Color(199, 21, 133, 200) else null
                    Blocks.GREEN_SHULKER_BOX -> if (shunlker) Color(199, 21, 133, 200) else null
                    Blocks.LIGHT_BLUE_SHULKER_BOX -> if (shunlker) Color(199, 21, 133, 200) else null
                    Blocks.LIGHT_GRAY_SHULKER_BOX -> if (shunlker) Color(199, 21, 133, 200) else null
                    Blocks.LIME_SHULKER_BOX -> if (shunlker) Color(199, 21, 133, 200) else null
                    Blocks.MAGENTA_SHULKER_BOX -> if (shunlker) Color(199, 21, 133, 200) else null
                    Blocks.ORANGE_SHULKER_BOX -> if (shunlker) Color(199, 21, 133, 200) else null
                    Blocks.PINK_SHULKER_BOX -> if (shunlker) Color(199, 21, 133, 200) else null
                    Blocks.PURPLE_SHULKER_BOX -> if (shunlker) Color(199, 21, 133, 200) else null
                    Blocks.YELLOW_SHULKER_BOX -> if (shunlker) Color(199, 21, 133, 200) else null
                    Blocks.RED_SHULKER_BOX -> if (shunlker) Color(199, 21, 133, 200) else null
                    Blocks.BLACK_SHULKER_BOX -> if (shunlker) Color(199, 21, 133, 200) else null
                    Blocks.BLUE_SHULKER_BOX -> if (shunlker) Color(199, 21, 133, 200) else null
                    Blocks.BROWN_SHULKER_BOX -> if (shunlker) Color(199, 21, 133, 200) else null
                    Blocks.CYAN_SHULKER_BOX -> if (shunlker) Color(199, 21, 133, 200) else null
                    Blocks.GRAY_SHULKER_BOX -> if (shunlker) Color(199, 21, 133, 200) else null
                    Blocks.GREEN_SHULKER_BOX -> if (shunlker) Color(199, 21, 133, 200) else null
                    Blocks.LIGHT_BLUE_SHULKER_BOX -> if (shunlker) Color(199, 21, 133, 200) else null
                    Blocks.LIGHT_GRAY_SHULKER_BOX -> if (shunlker) Color(199, 21, 133, 200) else null
                    Blocks.LIME_SHULKER_BOX -> if (shunlker) Color(199, 21, 133, 200) else null
                    Blocks.MAGENTA_SHULKER_BOX -> if (shunlker) Color(199, 21, 133, 200) else null
                    Blocks.ORANGE_SHULKER_BOX -> if (shunlker) Color(199, 21, 133, 200) else null
                    Blocks.PINK_SHULKER_BOX -> if (shunlker) Color(199, 21, 133, 200) else null
                    Blocks.PURPLE_SHULKER_BOX -> if (shunlker) Color(199, 21, 133, 200) else null
                    Blocks.RED_SHULKER_BOX -> if (shunlker) Color(199, 21, 133, 200) else null
                    Blocks.YELLOW_SHULKER_BOX -> if (shunlker) Color(199, 21, 133, 200) else null
                    Blocks.ENDER_CHEST -> if (ender) Color(75, 83, 32, 200) else null
                    else -> null
                }
                color?.let {
                    Render3DEngine.drawFilledBox(
                        event.matrices, blockPos.toBox(), Color(it.red, it.green, it.blue, fillAlpha)

                    )
                    Render3DEngine.drawBoxOutline(
                        blockPos.toBox(), Color(
                            it.red, it.green, it.blue, lineAlpha
                        ), lineWidth
                    )
                }
            }
        }
    }
}