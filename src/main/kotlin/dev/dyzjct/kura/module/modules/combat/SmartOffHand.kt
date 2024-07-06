package dev.dyzjct.kura.module.modules.combat

import dev.dyzjct.kura.event.eventbus.SafeClientEvent
import base.utils.concurrent.threads.runSafe
import base.utils.entity.EntityUtils.getHealth
import base.utils.hole.SurroundUtils
import base.utils.hole.SurroundUtils.checkHole
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.module.modules.client.CombatSystem
import dev.dyzjct.kura.module.modules.crystal.CrystalDamageCalculator.calcDamage
import dev.dyzjct.kura.module.modules.crystal.CrystalHelper.getPredictedTarget
import dev.dyzjct.kura.utils.TimerUtils
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.decoration.EndCrystalEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.*
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.util.Hand
import net.minecraft.util.math.BlockPos

object SmartOffHand :
    Module(name = "SmartOffHand", langName = "自动副手", description = "StupidOffHand", category = Category.COMBAT) {
    private var mode = msetting("Mode", Mode.Crystal)
    private var delay = isetting("Delay", 0, 0, 1000)
    private var totem = bsetting("SwitchTotem", true)
    private var sbHealth = dsetting("Health", 11.0, 0.0, 36.0)
    private var autoSwitch = bsetting("SwitchGap", true)
    private var elytra = bsetting("CheckElytra", true)
    private var holeCheck = bsetting("CheckHole", false)
    private var holeSwitch = dsetting("HoleHealth", 8.0, 0.0, 36.0).isTrue(holeCheck)
    private var crystalCalculate = bsetting("CalculateDmg", true)
    private var maxSelfDmg = dsetting("MaxSelfDmg", 26.0, 0.0, 36.0).isTrue(crystalCalculate)
    private var timerUtils = TimerUtils()
    private var totems = 0
    private var count = 0

    init {
        onMotion {
            val shouldSwitch: Boolean
            var crystals =
                player.inventory.main.stream().filter { itemStack: ItemStack -> itemStack.item === Items.END_CRYSTAL }
                    .mapToInt { obj: ItemStack -> obj.count }.sum()
            if (player.getStackInHand(Hand.OFF_HAND).item === Items.END_CRYSTAL) {
                crystals += player.getStackInHand(Hand.OFF_HAND).count
            }
            var gapple =
                player.inventory.main.stream().filter { itemStack: ItemStack -> itemStack.item === Items.GOLDEN_APPLE }
                    .mapToInt { obj: ItemStack -> obj.count }.sum()
            if (player.getStackInHand(Hand.OFF_HAND).item === Items.GOLDEN_APPLE) {
                gapple += player.getStackInHand(Hand.OFF_HAND).count
            }
            totems = player.inventory.main.stream()
                .filter { itemStack: ItemStack -> itemStack.item === Items.TOTEM_OF_UNDYING }
                .mapToInt { obj: ItemStack -> obj.count }.sum()
            if (player.getStackInHand(Hand.OFF_HAND).item === Items.TOTEM_OF_UNDYING) {
                ++totems
            }
            var item: Item? = null
            if (!player.getStackInHand(Hand.OFF_HAND).isEmpty) {
                item = player.getStackInHand(Hand.OFF_HAND).item
            }
            count = if (item != null) {
                when (item) {
                    Items.END_CRYSTAL -> {
                        crystals
                    }

                    Items.TOTEM_OF_UNDYING -> {
                        totems
                    }

                    else -> {
                        gapple
                    }
                }
            } else {
                0
            }
            val handItem = player.getStackInHand(Hand.MAIN_HAND).item
            val offhandItem = if (mode.value == Mode.Crystal) Items.END_CRYSTAL else Items.GOLDEN_APPLE
            shouldSwitch = handItem is SwordItem && mc.options.rightKey.isPressed && autoSwitch.value

            if (shouldTotem() && getItemSlot(Items.TOTEM_OF_UNDYING) != -1) {
                switchTotem()
            } else if (shouldSwitch && getItemSlot(Items.GOLDEN_APPLE) != -1) {
                if (player.getStackInHand(Hand.OFF_HAND).item != Items.GOLDEN_APPLE) {
                    val slot =
                        if (getItemSlot(Items.GOLDEN_APPLE) < 9) getItemSlot(Items.GOLDEN_APPLE) + 36 else getItemSlot(
                            Items.GOLDEN_APPLE
                        )
                    switchTo(slot)
                }
            } else if (getItemSlot(offhandItem) != -1) {
                val slot: Int =
                    if (getItemSlot(offhandItem) < 9) getItemSlot(offhandItem) + 36 else getItemSlot(offhandItem)
                if (player.getStackInHand(Hand.OFF_HAND).item != offhandItem) {
                    switchTo(slot)
                }
            } else {
                switchTotem()
            }
        }
    }

    private fun SafeClientEvent.shouldTotem(): Boolean {
        return if (totem.value) {
            checkHealth() || player.inventory.getStack(EquipmentSlot.CHEST.entitySlotId).item === Items.ELYTRA && elytra.value || player.fallDistance >= 5.0f || checkHole(
                player
            ) != SurroundUtils.HoleType.NONE && holeCheck.value && getHealth() <= holeSwitch.value || crystalCalculate.value && calcHealth()
        } else false
    }

    private fun SafeClientEvent.calcHealth(): Boolean {
        var maxDmg = 0.5
        for (entity in world.entities) {
            if (entity !is EndCrystalEntity) {
                continue
            }
            if (player.distanceTo(entity) > 12.0f) {
                continue
            }
            val predictionTarget =
                getPredictedTarget(entity, CombatSystem.predictTicks)
            val d = calcDamage(
                player,
                player.pos.add(predictionTarget),
                player.boundingBox,
                entity.x + 0.5,
                (entity.y + 1),
                entity.z + 0.5,
                BlockPos.Mutable()
            ).toDouble()
            if (d <= maxDmg) {
                continue
            }
            maxDmg = d
        }
        return maxDmg - 0.5 > getHealth() || maxDmg > maxSelfDmg.value
    }

    private fun SafeClientEvent.checkHealth(): Boolean {
        val lowHealth = getHealth() <= sbHealth.value
        val notInHoleAndLowHealth = lowHealth && checkHole(player) != SurroundUtils.HoleType.NONE
        return if (holeCheck.value) notInHoleAndLowHealth else lowHealth
    }

    private fun SafeClientEvent.switchTotem() {
        if (totems != 0 && player.getStackInHand(Hand.OFF_HAND).item != Items.TOTEM_OF_UNDYING) {
            val slot =
                if (getItemSlot(Items.TOTEM_OF_UNDYING) < 9) getItemSlot(Items.TOTEM_OF_UNDYING) + 36 else getItemSlot(
                    Items.TOTEM_OF_UNDYING
                )
            switchTo(slot)
        }
    }

    private fun SafeClientEvent.switchTo(slot: Int) {
        if (timerUtils.tickAndReset(delay.value)) {
            playerController.clickSlot(
                player.currentScreenHandler.syncId, slot, 0, SlotActionType.PICKUP, player as PlayerEntity
            )
            playerController.clickSlot(
                player.currentScreenHandler.syncId, 45, 0, SlotActionType.PICKUP, player as PlayerEntity
            )
            playerController.clickSlot(
                player.currentScreenHandler.syncId, slot, 0, SlotActionType.PICKUP, player as PlayerEntity
            )
        }
    }

    private fun SafeClientEvent.getItemSlot(input: Item): Int {
        var itemSlot = -1
        for (i in 45 downTo 1) {
            if (player.inventory.getStack(i).item !== input) continue
            itemSlot = i
            break
        }
        return itemSlot
    }

    override fun getHudInfo(): String {
        return runSafe {
            if (player.getStackInHand(Hand.OFF_HAND).item === Items.TOTEM_OF_UNDYING) {
                return@runSafe "Totem"
            }
            if (player.getStackInHand(Hand.OFF_HAND).item === Items.END_CRYSTAL) {
                return@runSafe "Crystal"
            }
            if (player.getStackInHand(Hand.OFF_HAND).item === Items.GOLDEN_APPLE) {
                return@runSafe "Gapple"
            }
            return@runSafe if (player.getStackInHand(Hand.OFF_HAND).item is BedItem) {
                "Bed"
            } else "None"
        } ?: "None"
    }

    @Suppress("unused")
    enum class Mode {
        Crystal, Gap
    }
}