package dev.dyzjct.kura.module.modules.combat

import base.utils.chat.ChatUtil
import base.utils.math.toBlockPos
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.utils.TimerUtils
import net.minecraft.entity.decoration.EndCrystalEntity
import net.minecraft.util.Hand
import net.minecraft.util.hit.EntityHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.Box

object AutoHitCrystal : Module(
    name = "AutoHitCrystal",
    langName = "  自动敲击水晶",
    category = Category.COMBAT,
    description = "Auto Hit Crystal."
) {
    private val delay by isetting("HitDelay", 50, 0, 250)
    private val debug by bsetting("Debug", false)
    private val timer = TimerUtils()

    init {
        onMotion {
            fun doAttack(entity: EndCrystalEntity) {
                if (timer.tickAndReset(delay)) {
                    playerController.attackEntity(player, entity)
                    player.swingHand(Hand.MAIN_HAND)
                }
            }
            if (mc.options.rightKey.isPressed) {
                mc.crosshairTarget?.let { result ->
                    if (result.type == HitResult.Type.ENTITY) {
                        if (debug) ChatUtil.sendNoSpamMessage("TYPE == ENTITY")
                        if ((result as EntityHitResult).entity is EndCrystalEntity) {
                            doAttack(result.entity as EndCrystalEntity)
                        }
                    }
                    if (result.type == HitResult.Type.BLOCK) {
                        result.pos?.let { pos ->
                            if (debug) ChatUtil.sendNoSpamMessage("TYPE == BLOCK")
                            if (world.entities.any {
                                    it is EndCrystalEntity && it.boundingBox.intersects(
                                        Box(
                                            pos.toBlockPos().up()
                                        )
                                    )
                                }) {
                                doAttack(world.entities.filter {
                                    it is EndCrystalEntity && it.boundingBox.intersects(
                                        Box(
                                            pos.toBlockPos().up()
                                        )
                                    )
                                }.first() as EndCrystalEntity)
                            }
                        }
                    }
                }
            }
        }
    }
}