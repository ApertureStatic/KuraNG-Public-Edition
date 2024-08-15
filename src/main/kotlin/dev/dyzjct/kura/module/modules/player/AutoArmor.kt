package dev.dyzjct.kura.module.modules.player

import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.module.modules.movement.ElytraFly
import it.unimi.dsi.fastutil.objects.Object2IntMap
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import dev.dyzjct.kura.event.eventbus.SafeClientEvent
import base.utils.concurrent.threads.runSafe
import base.utils.inventory.InvUtils
import net.minecraft.enchantment.Enchantment
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.enchantment.Enchantments
import net.minecraft.item.*
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import java.util.*

object AutoArmor : Module(
    name = "AutoArmor",
    langName = "自动穿甲",
    description = "Automatically equips armour",
    category = Category.PLAYER
) {
    private var antiBreak = bsetting("AntiBreak", true)
    private var elytraCheck by bsetting("ElytraCheck", true)
    private var delay = isetting("TickDelay", 1, 0, 5)

    private val enchantments = Object2IntOpenHashMap<Enchantment>()
    private val armorPieces = arrayOfNulls<ArmorPiece>(4)
    private val helmet = ArmorPiece(3)
    private val chestplate = ArmorPiece(2)
    private val leggings = ArmorPiece(1)
    private val boots = ArmorPiece(0)
    private var timer = 0

    init {
        armorPieces[0] = helmet
        armorPieces[1] = chestplate
        armorPieces[2] = leggings
        armorPieces[3] = boots
    }

    override fun onEnable() {
        timer = 0
    }

    fun getEnchantments(itemStack: ItemStack, enchantments: Object2IntMap<Enchantment>) {
        enchantments.clear()
        if (!itemStack.isEmpty) {
            val listTag =
                if (itemStack.item === Items.ENCHANTED_BOOK) EnchantedBookItem.getEnchantmentNbt(itemStack) else itemStack.enchantments
            for (i in listTag.indices) {
                val tag = listTag.getCompound(i)
                Registries.ENCHANTMENT.getOrEmpty(Identifier.tryParse(tag.getString("id")))
                    .ifPresent { enchantment: Enchantment? ->
                        enchantments.put(
                            enchantment,
                            EnchantmentHelper.getLevel(Enchantments.BLAST_PROTECTION, itemStack)
                        )
                    }
            }
        }
    }


    init {
        onMotion {
            if (timer > 0) {
                timer--
                return@onMotion
            }
            for (armorPiece in armorPieces) {
                if (armorPiece == null) continue
                armorPiece.reset()
            }
            for (i in 0 until player.inventory.main.size) {
                val itemStack = player.inventory.getStack(i)
                if (itemStack.isEmpty || itemStack.item !is ArmorItem) continue

                if (antiBreak.value && itemStack.isDamageable && itemStack.maxDamage - itemStack.damage <= 10) {
                    continue
                }
                getEnchantments(itemStack, enchantments)
                if (hasAvoidedEnchantment()) continue
                when (getItemSlotId(itemStack)) {
                    0 -> boots.add(itemStack, i)
                    1 -> leggings.add(itemStack, i)
                    2 -> chestplate.add(itemStack, i)
                    3 -> helmet.add(itemStack, i)
                }
            }
            for (armorPiece in armorPieces) {
                if (armorPiece == null) continue
                armorPiece.calculate()
            }
            Arrays.sort(armorPieces, Comparator.comparingInt { obj: ArmorPiece -> obj.sortScore })
            for (armorPiece in armorPieces) {
                if (armorPiece == null) continue
                armorPiece.apply()
            }
        }
    }

    private fun hasAvoidedEnchantment(): Boolean {
        for (enchantment in arrayOf(Enchantments.BINDING_CURSE, Enchantments.FROST_WALKER)) {
            if (enchantments.containsKey(enchantment)) return true
        }
        return false
    }

    private fun getItemSlotId(itemStack: ItemStack): Int {
        return if (itemStack.item is ElytraItem) 2 else (itemStack.item as ArmorItem).slotType.entitySlotId
    }

    private fun getScore(itemStack: ItemStack): Int {
        if (itemStack.isEmpty) return 0
        var score = 0
        var protection = Enchantments.BLAST_PROTECTION
        if (itemStack.item is ArmorItem && getItemSlotId(itemStack) == 1) {
            protection = Enchantments.BLAST_PROTECTION
        }
        score += 3 * enchantments.getInt(protection)
        score += enchantments.getInt(Enchantments.PROTECTION)
        score += enchantments.getInt(Enchantments.BLAST_PROTECTION)
        score += enchantments.getInt(Enchantments.FIRE_PROTECTION)
        score += enchantments.getInt(Enchantments.PROJECTILE_PROTECTION)
        score += enchantments.getInt(Enchantments.UNBREAKING)
        score += 2 * enchantments.getInt(Enchantments.MENDING)
        score += if (itemStack.item is ArmorItem) (itemStack.item as ArmorItem).protection else 0
        score = (score + if (itemStack.item is ArmorItem) (itemStack.item as ArmorItem).toughness else 0f).toInt()
        return score
    }

    private fun cannotSwap(): Boolean {
        return timer > 0
    }

    private fun swap(from: Int, armorSlotId: Int) {
        kotlin.runCatching {
            InvUtils.move().from(from).toArmor(armorSlotId)
            timer = delay.value
        }
    }

    private fun SafeClientEvent.moveToEmpty(armorSlotId: Int) {
        for (i in 0 until player.inventory.main.size) {
            if (player.inventory.getStack(i).isEmpty) {
                InvUtils.move().fromArmor(armorSlotId).to(i)
                timer = delay.value
                break
            }
        }
    }

    private class ArmorPiece(private val id: Int) {
        private var bestSlot = 0
        private var bestScore = 0
        private var score = 0
        private var durability = 0
        fun reset() {
            bestSlot = -1
            bestScore = -1
            score = -1
            durability = Int.MAX_VALUE
        }

        fun add(itemStack: ItemStack, slot: Int) {
            val score = getScore(itemStack)
            if (score > bestScore) {
                bestScore = score
                bestSlot = slot
            }
        }

        fun calculate() {
            runSafe {
                if (cannotSwap()) return
                val itemStack = player.inventory.getArmorStack(id)

                getEnchantments(itemStack, enchantments)
                if (enchantments.containsKey(Enchantments.BINDING_CURSE)) {
                    return
                }
                score = getScore(itemStack)
                score = decreaseScoreByAvoidedEnchantments(score)
                score = applyAntiBreakScore(score, itemStack)
                if (!itemStack.isEmpty) {
                    durability = itemStack.maxDamage - itemStack.damage
                }
            }
        }

        val sortScore: Int
            get() = if (antiBreak.value && durability <= 10) -1 else bestScore

        fun apply() {
            runSafe {
                if (cannotSwap() || score == Int.MAX_VALUE) return

                fun takeAction() {
                    if (bestScore > score) {
                        swap(bestSlot, id)
                    } else if (antiBreak.value && durability <= 10) {
                        moveToEmpty(id)
                    }
                }

                if (ElytraFly.isEnabled && elytraCheck) {
                    if (player.inventory.getArmorStack(2).item != Items.ELYTRA) {
                        var elytraSlot = -1
                        for (slot in 0..36) {
                            if (player.inventory.getStack(slot) == ItemStack.EMPTY || player.inventory.getStack(slot).item != Items.ELYTRA) continue
                            elytraSlot = slot
                            break
                        }
                        if (elytraSlot != -1) {
                            swap(elytraSlot, 2)
                            return
                        }
                    }
                } else {
                    takeAction()
                }
            }
        }


        private fun decreaseScoreByAvoidedEnchantments(score0: Int): Int {
            var score = score0
            for (enchantment in arrayOf(Enchantments.BINDING_CURSE, Enchantments.FROST_WALKER)) {
                score -= 2 * enchantments.getInt(enchantment)
            }
            return score
        }

        private fun applyAntiBreakScore(score: Int, itemStack: ItemStack): Int {
            return if (antiBreak.value && itemStack.isDamageable && itemStack.maxDamage - itemStack.damage <= 10) {
                -1
            } else score
        }
    }

}