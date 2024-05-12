package dev.dyzjct.kura.module.modules.render

import dev.dyzjct.kura.manager.BlockFinderManager
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import base.system.render.graphic.Render3DEngine
import net.minecraft.block.Blocks
import base.utils.math.toBox
import java.awt.Color

object Xray : Module(
    name = "Xray", langName = "矿物透视", description = "AutoSearch For Ores!", category = Category.RENDER, type = Type.Both
) {
    val distance by isetting("Distance", 25, 1, 80)
    private val coal by bsetting("Coal", false)
    private val iron by bsetting("Iron", false)
    private val gold by bsetting("Gold", false)
    private val lapis by bsetting("Lapis", false)
    private val redStone by bsetting("RedStone", false)
    private val diamond by bsetting("Diamond", false)
    private val greenStone by bsetting("GreenStone", false)
    private val netherGold by bsetting("NetherGold", false)
    private val quartz by bsetting("Quartz", false)
    private val ancient by bsetting("Ancient", false)
    val wmBypass = bsetting("WhiteMysteryBypass", false)
    val rotate by bsetting("Rotate", false).isTrue(wmBypass)
    val clickDelay by isetting("ClickDelay", 1000, 0, 1000).isTrue(wmBypass)
    private var lineWidth by fsetting("LineWidth", 1.5f, 0f, 5f)
    private val lineAlpha by isetting("LineAlpha", 200, 1, 255)
    private val fillAlpha by isetting("FillAlpha", 200, 1, 255)

    init {
        onRender3D { event ->
            BlockFinderManager.oreBlockList.forEach { blockPos ->
                val color = when (world.getBlockState(blockPos).block) {
                    Blocks.COAL_ORE -> if (coal) Color(0, 0, 0) else null
                    Blocks.DEEPSLATE_COAL_ORE -> if (coal) Color(0, 0, 0) else null
                    Blocks.IRON_ORE -> if (iron) Color(203, 155, 149) else null
                    Blocks.DEEPSLATE_IRON_ORE -> if (iron) Color(203, 155, 149) else null
                    Blocks.GOLD_ORE -> if (gold) Color(255, 255, 0) else null
                    Blocks.DEEPSLATE_GOLD_ORE -> if (gold) Color(255, 255, 0) else null
                    Blocks.LAPIS_ORE -> if (lapis) Color(0, 0, 255) else null
                    Blocks.DEEPSLATE_LAPIS_ORE -> if (lapis) Color(0, 0, 255) else null
                    Blocks.REDSTONE_ORE -> if (redStone) Color(255, 0, 0) else null
                    Blocks.DEEPSLATE_REDSTONE_ORE -> if (redStone) Color(255, 0, 0) else null
                    Blocks.DIAMOND_ORE -> if (diamond) Color(144, 204, 239) else null
                    Blocks.DEEPSLATE_DIAMOND_ORE -> if (diamond) Color(144, 204, 239) else null
                    Blocks.EMERALD_ORE -> if (greenStone) Color(0, 255, 0) else null
                    Blocks.DEEPSLATE_EMERALD_ORE -> if (greenStone) Color(0, 255, 0) else null
                    Blocks.NETHER_GOLD_ORE -> if (netherGold) Color(255, 255, 0) else null
                    Blocks.NETHER_QUARTZ_ORE -> if (quartz) Color(200, 200, 200) else null
                    Blocks.ANCIENT_DEBRIS -> if (ancient) Color(128, 78, 65) else null
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