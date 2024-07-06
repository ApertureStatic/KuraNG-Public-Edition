package dev.dyzjct.kura.module.modules.render

import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects

object Brightness : Module(name = "Brightness", langName = "客户端亮度", category = Category.RENDER) {
    var brightness = isetting("Brightness", 15, 0, 15)
    private var gammaVal by dsetting("GammaValue", 1.0, 1.0, 100.0)
    private var effect = bsetting("Effect", true)

    init {
        onMotion {
            if (effect.value) {
                mc.options.gamma.value = 1.0
                player.addStatusEffect(StatusEffectInstance(StatusEffects.NIGHT_VISION, 5210))
            } else {
                mc.options.gamma.value = gammaVal
                player.removeStatusEffect(StatusEffects.NIGHT_VISION)
            }
        }
    }
}