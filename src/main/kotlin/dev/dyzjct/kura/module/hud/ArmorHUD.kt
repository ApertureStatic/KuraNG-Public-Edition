package dev.dyzjct.kura.module.hud

import com.mojang.blaze3d.platform.GlStateManager
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.HUDModule
import base.system.render.newfont.FontRenderers
import net.minecraft.client.gui.DrawContext
import net.minecraft.item.ItemStack

object ArmorHUD : HUDModule(name = "ArmorHUD", langName = "装备渲染", category = Category.HUD) {
    private var damage = bsetting("RenderDamage", true)

    override var width = 80f
    override var height
        get() = if (damage.value) 25f else 15f
        set(value) {}

    override fun onRender(context: DrawContext) {
        var iteration = 0
        for (item in mc.player?.inventory?.armor ?: listOf<ItemStack>()) {
            ++iteration
            if (item.isEmpty) continue
            val xPos = x - 90 + (9 - iteration) * 20 - 8

            val offsetY = if (damage.value) 10 else 0

            GlStateManager._enableDepthTest()
            context.drawItem(item, xPos.toInt(), (y + offsetY).toInt())
            context.drawItemInSlot(mc.textRenderer, item, xPos.toInt(), (y + offsetY).toInt())
            GlStateManager._disableDepthTest()
            val itemCount = item.count.toString()
            FontRenderers.lexend.drawString(
                context.matrices, itemCount,
                (xPos + 18 - FontRenderers.lexend.getStringWidth(itemCount)),
                (y + offsetY + 9),
                16777215,
                false

            )
            if (damage.value) {
                drawDamage(context, item, xPos.toInt(), y.toInt())
            }
        }
    }

    private fun drawDamage(context: DrawContext, itemstack: ItemStack, x: Int, y: Int) {
        val green: Float = (itemstack.maxDamage.toFloat() - itemstack.damage.toFloat()) / itemstack.maxDamage.toFloat()
        val red = 1.0f - green
        val dmg = 100 - (red * 100.0f).toInt()
        FontRenderers.lexend.drawString(
            context.matrices, dmg.toString(),
            (x + 8 - FontRenderers.lexend.getStringWidth(dmg.toString()) / 2),
            (y + 3).toFloat(),
            toHex((red * 255.0f).toInt(), (green * 255.0f).toInt(), 0),
            false

        )
    }

    private fun toHex(r: Int, g: Int, b: Int): Int {
        return -0x1000000 or (r and 0xFF shl 16) or (g and 0xFF shl 8) or (b and 0xFF)
    }
}
