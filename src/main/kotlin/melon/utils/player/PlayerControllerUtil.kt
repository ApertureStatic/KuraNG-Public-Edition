package melon.utils.player

import melon.utils.concurrent.threads.runSafe
import net.minecraft.client.network.ClientPlayerInteractionManager
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket

fun ClientPlayerInteractionManager.updateController() {
    runSafe {
        runCatching {
            val i = player.inventory.selectedSlot
            if (i != lastSelectedSlot) {
                lastSelectedSlot = i
                connection.sendPacket(UpdateSelectedSlotC2SPacket(lastSelectedSlot))
            }
        }
    }
    //(this as AccessorClientPlayerInteractionManager).syncSelectedSlot()
}