package dev.dyzjct.kura.module.modules.render

import dev.dyzjct.kura.manager.FriendManager
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import net.minecraft.entity.Entity
import net.minecraft.entity.decoration.EndCrystalEntity
import net.minecraft.entity.player.PlayerEntity
import java.awt.Color

object Chams : Module(name = "Chams", langName = "实体渲染", category = Category.RENDER, type = Type.Both) {
    var players by bsetting("PLayers", true)
    private var playerColor by csetting("PlayerColor", Color(132, 123, 54, 120))
    private var friendColor by csetting("FriendColor", Color(0, 255, 255, 120))
    var crystals by bsetting("Crystals", true)
    private var crystalColor by csetting("CrystalColor", Color(231, 51, 251, 120))

    fun getEntityColor(entity: Entity): Color {
        return when (entity) {
            is PlayerEntity -> {
                if (FriendManager.isFriend(entity)) {
                    friendColor
                } else playerColor
            }

            is EndCrystalEntity -> {
                crystalColor
            }

            else -> {
                Color.WHITE
            }
        }
    }
}