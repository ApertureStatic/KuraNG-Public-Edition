package dev.dyzjct.kura.module.modules.render

import base.utils.chat.ChatUtil
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import net.minecraft.entity.EquipmentSlot


object AntiPlayerSwing : Module(name = "AntiPlayerSwing", langName = "防止玩家摆动", category = Category.RENDER) {
    val fakeSneak by bsetting("FakeSneak", false)
    val arm by bsetting("Arm", false)
    val leg by bsetting("Leg", false)
    val debug by bsetting("Debug", false)
}