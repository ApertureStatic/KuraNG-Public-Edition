package dev.dyzjct.kura.module.modules.render

import dev.dyzjct.kura.mixins.IHeldItemRenderer
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.utils.TimerUtils
import melon.events.render.ItemRenderEvent
import melon.system.event.safeEventListener
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket
import net.minecraft.util.Hand


object HandView : Module(name = "HandView", "手部渲染", category = Category.RENDER) {

    val swingSpeed by isetting("SlowVL", 6, 0, 20)
    val oldSwing by bsetting("OldSwing", true)
    private val disableSwapMain by bsetting("DisableSwapMain", false)
    private val disableSwapOff by bsetting("DisableSwapOff", false)
    private val resetSwing = bsetting("ResetSwing", false)
    private val delay by isetting("ResetDelay", 100, 0, 1000).isTrue(resetSwing)
    private val tick by isetting("ResetTick", 0, 0, 5).isTrue(resetSwing)
    private val mainHand = bsetting("MainHand", true)
    private val scaleX by fsetting("MainScaleX", 1.0f, 0.0f, 2.0f).isTrue(mainHand)
    private val scaleY by fsetting("MainScaleY", 1.0f, 0.0f, 2.0f).isTrue(mainHand)
    private val scaleZ by fsetting("MainScaleZ", 1.0f, 0.0f, 2.0f).isTrue(mainHand)
    private val offHand = bsetting("OffHand", true)
    private val offScaleX by fsetting("OffScaleX", 1.0f, 0.0f, 2.0f).isTrue(offHand)
    private val offScaleY by fsetting("OffScaleY", 1.0f, 0.0f, 2.0f).isTrue(offHand)
    private val offScaleZ by fsetting("OffScaleZ", 1.0f, 0.0f, 2.0f).isTrue(offHand)
    private val timer = TimerUtils()

    init {
        onPacketSend {
            if (it.packet is HandSwingC2SPacket && resetSwing.value && player.handSwinging) {
                if (timer.tickAndReset(delay)) {
                    player.handSwingTicks = tick
                }
            }
        }
        onMotion {
            if (disableSwapMain && (mc.entityRenderDispatcher.heldItemRenderer as IHeldItemRenderer).getEquippedProgressMainHand() <= 1f) {
                (mc.entityRenderDispatcher.heldItemRenderer as IHeldItemRenderer).setEquippedProgressMainHand(1f)
                (mc.entityRenderDispatcher.heldItemRenderer as IHeldItemRenderer).setItemStackMainHand(mc.player!!.mainHandStack)
            }

            if (disableSwapOff && (mc.entityRenderDispatcher.heldItemRenderer as IHeldItemRenderer).getEquippedProgressOffHand() <= 1f) {
                (mc.entityRenderDispatcher.heldItemRenderer as IHeldItemRenderer).setEquippedProgressOffHand(1f)
                (mc.entityRenderDispatcher.heldItemRenderer as IHeldItemRenderer).setItemStackOffHand(mc.player!!.offHandStack)
            }
        }
        safeEventListener<ItemRenderEvent> {
            if (mainHand.value) {
                if (it.hand == Hand.MAIN_HAND) {
                    it.matrices.scale(scaleX, scaleY, scaleZ)
                }
            }
            if (offHand.value) {
                if (it.hand == Hand.OFF_HAND) {
                    it.matrices.scale(offScaleX, offScaleY, offScaleZ)
                }
            }
        }
    }
}