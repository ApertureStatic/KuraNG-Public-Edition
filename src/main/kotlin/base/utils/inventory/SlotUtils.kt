package base.utils.inventory

import dev.dyzjct.kura.event.eventbus.SafeClientEvent
import base.utils.concurrent.threads.runSafe
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen.CreativeScreenHandler
import net.minecraft.entity.mob.SkeletonHorseEntity
import net.minecraft.entity.mob.ZombieHorseEntity
import net.minecraft.entity.passive.AbstractDonkeyEntity
import net.minecraft.entity.passive.AbstractHorseEntity
import net.minecraft.entity.passive.HorseEntity
import net.minecraft.entity.passive.LlamaEntity
import net.minecraft.item.ItemGroups
import net.minecraft.registry.Registries
import net.minecraft.screen.*

object SlotUtils {
    const val HOTBAR_START = 0
    const val HOTBAR_END = 8
    const val OFFHAND = 45
    const val MAIN_START = 9
    const val MAIN_END = 35
    const val ARMOR_START = 36
    const val ARMOR_END = 39

    fun indexToId(i: Int): Int {
        return runSafe {
            val handler: ScreenHandler = player.currentScreenHandler
            if (handler is PlayerScreenHandler) return survivalInventory(i)
            if (handler is CreativeScreenHandler) return creativeInventory(i)
            if (handler is GenericContainerScreenHandler) return genericContainer(i, handler.rows)
            if (handler is CraftingScreenHandler) return craftingTable(i)
            if (handler is FurnaceScreenHandler) return furnace(i)
            if (handler is BlastFurnaceScreenHandler) return furnace(i)
            if (handler is SmokerScreenHandler) return furnace(i)
            if (handler is Generic3x3ContainerScreenHandler) return generic3x3(i)
            if (handler is EnchantmentScreenHandler) return enchantmentTable(i)
            if (handler is BrewingStandScreenHandler) return brewingStand(i)
            if (handler is MerchantScreenHandler) return villager(i)
            if (handler is BeaconScreenHandler) return beacon(i)
            if (handler is AnvilScreenHandler) return anvil(i)
            if (handler is HopperScreenHandler) return hopper(i)
            if (handler is ShulkerBoxScreenHandler) return genericContainer(i, 3)
            if (handler is HorseScreenHandler) return horse(handler, i)
            if (handler is CartographyTableScreenHandler) return cartographyTable(i)
            if (handler is GrindstoneScreenHandler) return grindstone(i)
            if (handler is LecternScreenHandler) return lectern()
            if (handler is LoomScreenHandler) return loom(i)
            return if (handler is StonecutterScreenHandler) stonecutter(i) else -1
        } ?: -1
    }

    private fun survivalInventory(i: Int): Int {
        if (isHotbar(i)) return 36 + i
        return if (isArmor(i)) 5 + (i - 36) else i
    }

    private fun SafeClientEvent.creativeInventory(i: Int): Int {
        return if (mc.currentScreen !is CreativeInventoryScreen || CreativeInventoryScreen.selectedTab != Registries.ITEM_GROUP[ItemGroups.INVENTORY]) -1 else survivalInventory(
            i
        )
    }

    private fun genericContainer(i: Int, rows: Int): Int {
        if (isHotbar(i)) return (rows + 3) * 9 + i
        return if (isMain(i)) rows * 9 + (i - 9) else -1
    }

    private fun craftingTable(i: Int): Int {
        if (isHotbar(i)) return 37 + i
        return if (isMain(i)) i + 1 else -1
    }

    private fun furnace(i: Int): Int {
        if (isHotbar(i)) return 30 + i
        return if (isMain(i)) 3 + (i - 9) else -1
    }

    private fun generic3x3(i: Int): Int {
        if (isHotbar(i)) return 36 + i
        return if (isMain(i)) i else -1
    }

    private fun enchantmentTable(i: Int): Int {
        if (isHotbar(i)) return 29 + i
        return if (isMain(i)) 2 + (i - 9) else -1
    }

    private fun brewingStand(i: Int): Int {
        if (isHotbar(i)) return 32 + i
        return if (isMain(i)) 5 + (i - 9) else -1
    }

    private fun villager(i: Int): Int {
        if (isHotbar(i)) return 30 + i
        return if (isMain(i)) 3 + (i - 9) else -1
    }

    private fun beacon(i: Int): Int {
        if (isHotbar(i)) return 28 + i
        return if (isMain(i)) 1 + (i - 9) else -1
    }

    private fun anvil(i: Int): Int {
        if (isHotbar(i)) return 30 + i
        return if (isMain(i)) 3 + (i - 9) else -1
    }

    private fun hopper(i: Int): Int {
        if (isHotbar(i)) return 32 + i
        return if (isMain(i)) 5 + (i - 9) else -1
    }

    private fun horse(handler: ScreenHandler, i: Int): Int {
        val entity: AbstractHorseEntity = (handler as HorseScreenHandler).entity
        if (entity is LlamaEntity) {
            val strength: Int = entity.strength
            if (isHotbar(i)) return 2 + 3 * strength + 28 + i
            if (isMain(i)) return 2 + 3 * strength + 1 + (i - 9)
        } else if (entity is HorseEntity || entity is SkeletonHorseEntity || entity is ZombieHorseEntity) {
            if (isHotbar(i)) return 29 + i
            if (isMain(i)) return 2 + (i - 9)
        } else if (entity is AbstractDonkeyEntity) {
            val chest: Boolean = entity.hasChest()
            if (isHotbar(i)) return (if (chest) 44 else 29) + i
            if (isMain(i)) return (if (chest) 17 else 2) + (i - 9)
        }
        return -1
    }

    private fun cartographyTable(i: Int): Int {
        if (isHotbar(i)) return 30 + i
        return if (isMain(i)) 3 + (i - 9) else -1
    }

    private fun grindstone(i: Int): Int {
        if (isHotbar(i)) return 30 + i
        return if (isMain(i)) 3 + (i - 9) else -1
    }

    private fun lectern(): Int {
        return -1
    }

    private fun loom(i: Int): Int {
        if (isHotbar(i)) return 31 + i
        return if (isMain(i)) 4 + (i - 9) else -1
    }

    private fun stonecutter(i: Int): Int {
        if (isHotbar(i)) return 29 + i
        return if (isMain(i)) 2 + (i - 9) else -1
    }

    // Utils
    fun isHotbar(i: Int): Boolean {
        return i >= HOTBAR_START && i <= HOTBAR_END
    }

    fun isMain(i: Int): Boolean {
        return i >= MAIN_START && i <= MAIN_END
    }

    fun isArmor(i: Int): Boolean {
        return i >= ARMOR_START && i <= ARMOR_END
    }
}
