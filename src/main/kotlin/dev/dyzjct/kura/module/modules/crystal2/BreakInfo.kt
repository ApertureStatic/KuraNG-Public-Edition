package dev.dyzjct.kura.module.modules.crystal2

import dev.dyzjct.kura.module.modules.crystal2.AutoCrystal2
import net.minecraft.entity.decoration.EndCrystalEntity

open class BreakInfo(
    open val crystal: EndCrystalEntity,
    open val selfDamage: Float,
    open val targetDamage: Double
) {
    class Mutable : BreakInfo(DUMMY_CRYSTAL, Float.MAX_VALUE, AutoCrystal2.forcePlaceDmg.value) {
        override var crystal = super.crystal; private set
        override var selfDamage = super.selfDamage; private set
        override var targetDamage = super.targetDamage; private set

        fun update(
            target: EndCrystalEntity,
            selfDamage: Float,
            targetDamage: Double
        ) {
            this.crystal = target
            this.selfDamage = selfDamage
            this.targetDamage = targetDamage
        }

        fun clear() {
            update(DUMMY_CRYSTAL, Float.MAX_VALUE, AutoCrystal2.forcePlaceDmg.value)
        }
    }

    fun takeValid(): BreakInfo? {
        return this.takeIf {
            crystal !== DUMMY_CRYSTAL
                    && selfDamage != Float.MAX_VALUE
                    && targetDamage != AutoCrystal2.forcePlaceDmg.value
        }
    }

    companion object {
        @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        private val DUMMY_CRYSTAL = EndCrystalEntity(null, 0.0, 0.0, 0.0)
    }
}