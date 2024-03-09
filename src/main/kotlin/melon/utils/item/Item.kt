package melon.utils.item

import dev.dyzjct.kura.utils.animations.fastCeil
import net.minecraft.block.Block
import net.minecraft.enchantment.Enchantment
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.enchantment.Enchantments
import net.minecraft.item.*

val ItemStack.originalName: String get() = item.getName(this).toString()

val Item.id get() = Item.getRawId(this)

val Item.block: Block get() = Block.getBlockFromItem(this)

val Item.isWeapon get() = this is SwordItem || this is AxeItem

val Item.isTool: Boolean
    get() = this is ToolItem
            || this is SwordItem
            || this is HoeItem
            || this is ShearsItem

// Code By GPT 666
val Item.foodValue: Int
    get() {
        val foodComponent = this.foodComponent ?: return 0
        return foodComponent.hunger
    }
val Item.saturation: Float
    get() {
        val foodComponent = this.foodComponent ?: return 0f
        return foodComponent.saturationModifier * foodValue * 2f
    }

val ItemStack.attackDamage: Float
    get() {
        val item = this.item
        val baseDamage = item.baseAttackDamage // 获取基础攻击伤害
        val sharpnessLevel = EnchantmentHelper.getLevel(Enchantments.SHARPNESS, this) // 获取锋利附魔的等级
        return baseDamage + if (sharpnessLevel > 0) sharpnessLevel * 0.5f + 0.5f else 0.0f // 计算最终攻击伤害
    }

val Item.baseAttackDamage: Float
    get() = when (this) {
        is SwordItem -> this.attackDamage + 4.0f
        is ToolItem -> this.maxDamage + 1.0f
        else -> 1.0f
    }
val ItemStack.durability: Int
    get() = this.maxDamage - this.damage

val ItemStack.duraPercentage: Int
    get() = (this.durability * 100.0f / this.maxDamage.toFloat()).fastCeil()

fun ItemStack.getEnchantmentLevel(enchantment: Enchantment) =
    EnchantmentHelper.getLevel(enchantment, this)

