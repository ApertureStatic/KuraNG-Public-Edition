package dev.dyzjct.kura.module.modules.render

import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import net.minecraft.item.ItemStack

object ToolTips : Module(name = "ToolTips", langName = "工具提示", category = Category.RENDER, safeModule = true) {
    var middleClickOpen = bsetting("MiddleClickOpen", true)
    var storage = bsetting("Storage", true)
    var maps = bsetting("Maps", true)
    var shulkerRegear = bsetting("ShulkerRegear", true)

    fun hasItems(itemStack: ItemStack): Boolean {
        val compoundTag = itemStack.getSubNbt("BlockEntityTag")
        return compoundTag != null && compoundTag.contains("Items", 9)
    }
}