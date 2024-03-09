package dev.dyzjct.kura.gui.screen

import com.mojang.blaze3d.systems.RenderSystem
import dev.dyzjct.kura.module.modules.render.ToolTips
import melon.system.render.graphic.Render2DEngine
import melon.utils.concurrent.threads.runSafe
import net.minecraft.block.Block
import net.minecraft.block.EnderChestBlock
import net.minecraft.block.ShulkerBoxBlock
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.BlockItem
import net.minecraft.item.ItemStack
import net.minecraft.screen.ShulkerBoxScreenHandler
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import org.lwjgl.glfw.GLFW

class PeekScreen(
    handler: ShulkerBoxScreenHandler?,
    inventory: PlayerInventory?,
    title: Text?,
    private val block: Block
) : ShulkerBoxScreen(handler, inventory, title) {
    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button != GLFW.GLFW_MOUSE_BUTTON_MIDDLE) return false
        val focusedSlot = focusedSlot ?: return false

        runSafe {
            if (!focusedSlot.stack.isEmpty && player.playerScreenHandler.cursorStack.isEmpty) {
                val itemFocused = focusedSlot.stack

                if (ToolTips.hasItems(itemFocused) && ToolTips.middleClickOpen.value) {
                    val itemNbt = itemFocused.nbt ?: return false

                    if (!itemNbt.contains("BlockEntityTag")) return false
                    val nbtEntityTag = itemNbt.getCompound("BlockEntityTag")

                    if (!nbtEntityTag.contains("Items")) return false
                    val nbtItems = nbtEntityTag.getList("Items", 10)

                    val items = Array(27) { ItemStack.EMPTY }
                    for (i in nbtItems.indices) {
                        val nbtItem = nbtItems.getCompound(i)
                        items[nbtItem.getByte("Slot").toInt()] = ItemStack.fromNbt(nbtItem)
                    }

                    mc.setScreen(
                        PeekScreen(
                            ShulkerBoxScreenHandler(0, player.inventory, SimpleInventory(*items)),
                            player.inventory,
                            focusedSlot.stack.name,
                            (focusedSlot.stack.item as BlockItem).block
                        )
                    )
                    return true
                }
            }
        }
        return false
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        return false
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        when (block) {
            is ShulkerBoxBlock -> {
                val colors = block.color?.colorComponents ?: floatArrayOf(1f, 1f, 1f)
                RenderSystem.setShaderColor(colors[0], colors[1], colors[2], 1.0f)
            }

            is EnderChestBlock -> {
                RenderSystem.setShaderColor(0f, 50f / 255f, 50f / 255f, 1.0f)
            }

            else -> {
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
            }
        }
        val i = (this.width - this.backgroundWidth) / 2
        val j = (this.height - this.backgroundHeight) / 2
        Render2DEngine.drawTexture(context, TEXTURE, i + 2, j + 12, 176, 67)
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
    }

    companion object {
        private val TEXTURE = Identifier("textures/container.png")
    }
}