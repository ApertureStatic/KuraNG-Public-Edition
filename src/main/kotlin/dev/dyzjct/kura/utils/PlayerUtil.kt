package dev.dyzjct.kura.utils

import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.entity.effect.StatusEffects

fun ClientPlayerEntity.isWeaknessActive(): Boolean {
    return this.getStatusEffect(StatusEffects.WEAKNESS) != null && this.getStatusEffect(StatusEffects.STRENGTH)
        ?.let {
            it.amplifier <= 0
        } ?: true
}