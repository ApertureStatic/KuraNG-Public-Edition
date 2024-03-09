package dev.dyzjct.kura.module.modules.combat

import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import base.utils.concurrent.threads.runSafe
import net.minecraft.item.BowItem
import net.minecraft.item.Items

object FastUse :
    Module(category = Category.COMBAT, langName = "快速使用物品", description = "Use items faster", name = "FastUse") {
    private val delay = isetting("Delay", 1, 0, 10)
    private val exp = bsetting("XP", true)
    private val bow = bsetting("Bow", false)

    init {
        onMotion {
            if (player.mainHandStack.item == Items.EXPERIENCE_BOTTLE && exp.value) {
                mc.itemUseCooldown =
                    mc.itemUseCooldown.coerceAtMost(delay.value)
            } else if (player.mainHandStack.item is BowItem && bow.value) {
                mc.itemUseCooldown =
                    mc.itemUseCooldown.coerceAtMost(delay.value)
            }
        }
    }

    override fun onDisable() {
        runSafe {
            mc.itemUseCooldown = 2
        }
    }
}
