package base.utils.combat

import dev.dyzjct.kura.module.modules.crystal.MelonAura2
import net.minecraft.enchantment.Enchantment
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.enchantment.Enchantments
import net.minecraft.entity.DamageUtil
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.item.ArmorItem
import net.minecraft.item.ItemStack
import kotlin.math.max
import kotlin.math.min

class DamageReduction(val entity: LivingEntity) {
    private val armorValue = entity.armor.toFloat()
    private val toughness = entity.getAttributeValue(EntityAttributes.GENERIC_ARMOR_TOUGHNESS).toFloat()
    private val resistanceMultiplier: Float
    private val genericMultiplier: Float
    private val blastMultiplier: Float

    fun ItemStack.getEnchantmentLevel(enchantment: Enchantment) =
        EnchantmentHelper.getLevel(enchantment, this)

    init {
        var genericEPF = 0
        var blastEPF = 0

        for (itemStack in entity.armorItems) {
            if (MelonAura2.isEnabled) {
                when (MelonAura2.damageMode.value) {
                    MelonAura2.DamageMode.Auto -> {
                        genericEPF += itemStack.getEnchantmentLevel(Enchantments.PROTECTION)
                        blastEPF += itemStack.getEnchantmentLevel(Enchantments.BLAST_PROTECTION) * 2
                    }

                    MelonAura2.DamageMode.PPBP -> {
                        if (itemStack.item is ArmorItem) {
                            if ((itemStack.item as ArmorItem).type == ArmorItem.Type.LEGGINGS) {
                                blastEPF += 4 * 2
                            } else {
                                genericEPF += 4
                            }
                        }
                    }

                    MelonAura2.DamageMode.BBBB -> {
                        blastEPF += 4 * 2
                    }
                }
            } else {
                genericEPF += itemStack.getEnchantmentLevel(Enchantments.PROTECTION)
                blastEPF += itemStack.getEnchantmentLevel(Enchantments.BLAST_PROTECTION) * 2
            }
        }

        resistanceMultiplier = entity.getStatusEffect(StatusEffects.RESISTANCE)?.let {
            max(1.0f - (it.amplifier + 1) * 0.2f, 0.0f)
        } ?: run {
            1.0f
        }

        genericMultiplier = (1.0f - min(genericEPF, 20) / 25.0f)
        blastMultiplier = (1.0f - min(genericEPF + blastEPF, 20) / 25.0f)
    }

    fun calcDamage(damage: Float, isExplosion: Boolean) =
        DamageUtil.getDamageLeft(damage, armorValue, toughness) *
                resistanceMultiplier *
                if (isExplosion) blastMultiplier
                else genericMultiplier
}