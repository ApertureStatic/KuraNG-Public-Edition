package dev.dyzjct.kura.mixins;

import net.minecraft.item.ItemStack;

public interface IHeldItemRenderer {
    void setEquippedProgressMainHand(float var1);
    void setEquippedProgressOffHand(float var1);
    float getEquippedProgressMainHand();
    float getEquippedProgressOffHand();
    void setItemStackMainHand(ItemStack var1);
    void setItemStackOffHand(ItemStack var1);
}