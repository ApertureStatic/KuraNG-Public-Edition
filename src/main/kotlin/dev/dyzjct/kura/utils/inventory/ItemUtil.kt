package dev.dyzjct.kura.utils.inventory

import net.minecraft.block.Block
import net.minecraft.item.BlockItem
import net.minecraft.item.Item
import net.minecraft.item.ItemStack

//  would suffice probably
object ItemUtil {
    fun areSame(block1: Block, block2: Block): Boolean {
        return Block.getRawIdFromState(block1.defaultState) == Block.getRawIdFromState(block2.defaultState)
    }

    fun areSame(item1: Item?, item2: Item?): Boolean {
        return Item.getRawId(item1) == Item.getRawId(item2)
    }

    fun areSame(block: Block, item: Item?): Boolean {
        return item is BlockItem && areSame(block, item.block)
    }

    fun areSame(stack: ItemStack?, block: Block): Boolean {
        return stack != null && areSame(block, stack.item)
    }

    fun areSame(stack: ItemStack?, item: Item?): Boolean {
        return stack != null && areSame(stack.item, item)
    }
}
