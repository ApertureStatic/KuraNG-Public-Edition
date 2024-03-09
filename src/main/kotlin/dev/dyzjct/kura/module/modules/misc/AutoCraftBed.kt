package dev.dyzjct.kura.module.modules.misc

import dev.dyzjct.kura.manager.HotbarManager.spoofHotbar
import dev.dyzjct.kura.manager.RotationManager
import dev.dyzjct.kura.manager.SphereCalculatorManager
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.utils.TimerUtils
import dev.dyzjct.kura.utils.animations.Easing
import dev.dyzjct.kura.utils.animations.sq
import base.events.RunGameLoopEvent
import base.events.TickEvent
import base.system.event.SafeClientEvent
import base.system.event.StageType
import base.system.event.safeConcurrentListener
import base.system.event.safeParallelListener
import base.system.util.delegate.CachedValueN
import base.utils.block.BlockUtil.getNeighbor
import base.utils.concurrent.threads.runSafe
import base.utils.extension.fastPos
import base.utils.graphics.ESPRenderer
import base.utils.inventory.slot.firstBlock
import base.utils.inventory.slot.hotbarSlots
import base.utils.player.updateController
import net.minecraft.block.Blocks
import net.minecraft.client.gui.screen.ingame.CraftingScreen
import net.minecraft.client.gui.screen.ingame.InventoryScreen
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.network.packet.c2s.play.CraftRequestC2SPacket
import net.minecraft.network.packet.c2s.play.RecipeCategoryOptionsC2SPacket
import net.minecraft.recipe.book.RecipeBookCategory
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import team.exception.melon.util.math.distanceSqToCenter
import java.awt.Color
import java.util.concurrent.CopyOnWriteArrayList
import java.util.stream.Collectors

object AutoCraftBed : Module(name = "AutoCraftBed", langName = "自动合成床", category = Category.MISC) {
    private val craftDelay = isetting("CraftDelay", 5, 0, 1000)
    private var autoBedPVP = bsetting("AutoBedPVP", true)
    private var prioReady = bsetting("PrioReady", true)
    private var airPlace = bsetting("AirPlace", false)
    var placeRange = dsetting("PlaceRange", 3.5, 1.0, 6.0)
    private val manualCraft = bsetting("ManualCraft", false)
    private var fastPacket = bsetting("FastPacket", true)
    private var color = csetting("BaseColor", Color(113, 67, 231, 120))
    private var lineColor = csetting("LineColor", Color(123, 234, 132))
    private var craftTask = CopyOnWriteArrayList<BlockPos>()
    private var craftNeeded = false
    private var placePos: BlockPos? = null
    private var craftTimer = TimerUtils()

    private val bedList = mutableListOf<Item>(
        Items.YELLOW_BED,
        Items.RED_BED,
        Items.BLACK_BED,
        Items.BROWN_BED,
        Items.BLUE_BED,
        Items.CYAN_BED,
        Items.GRAY_BED,
        Items.GREEN_BED,
        Items.LIGHT_BLUE_BED,
        Items.LIGHT_GRAY_BED,
        Items.LIME_BED,
        Items.MAGENTA_BED,
        Items.ORANGE_BED,
        Items.PINK_BED,
        Items.PURPLE_BED,
        Items.WHITE_BED
    )

    private val rawPosList = CachedValueN(50L) {
        runSafe {
            getPlaceablePos()
        } ?: emptyList()
    }

    override fun getHudInfo(): String {
        return placePos?.let { "Not Null" } ?: "Null"
    }

    private fun SafeClientEvent.getPlaceablePos(): List<BlockPos> {
        val positions = CopyOnWriteArrayList<BlockPos>()
        positions.addAll(
            SphereCalculatorManager.sphereList.stream()
                .filter { world.isInBuildLimit(it.up()) && world.worldBorder.contains(it.up()) }
                .filter { player.distanceSqToCenter(it.up()) <= placeRange.value.sq }
                .filter { world.isAir(it.up()) || world.getBlockState(it.up()).block == Blocks.CRAFTING_TABLE }.filter {
                    if (world.getBlockState(it.up()).block != Blocks.CRAFTING_TABLE) {
                        if (airPlace.value) true else getNeighbor(it.up(), false) != null
                    } else true
                }.sorted(Comparator.comparingInt { getSafeFactor(it.up()) }).collect(Collectors.toList())
        )
        return positions
    }

    private fun SafeClientEvent.getSafeFactor(blockPos: BlockPos): Int {
        if (world.getBlockState(blockPos).block == Blocks.CRAFTING_TABLE && prioReady.value && player.distanceSqToCenter(
                blockPos
            ) <= placeRange.value.sq
        ) {
            return 1145141919
        }
        var factor = 0
        for (facing in Direction.values()) {
            if (world.isAir(blockPos.offset(facing))) continue
            factor++
        }
        return factor
    }

    init {
        safeParallelListener<TickEvent.Post> {
            rawPosList.updateForce()
        }

        safeConcurrentListener<RunGameLoopEvent.Tick> {
            val targetBlocks = rawPosList.get()
            if (targetBlocks.isEmpty()) return@safeConcurrentListener
            if (craftNeeded && autoBedPVP.value) {
                for (pos in targetBlocks) {
                    if (world.getBlockState(pos.up()).block == Blocks.CRAFTING_TABLE && !prioReady.value) continue
                    if (placePos != null && world.getBlockState(pos.up()).block != Blocks.CRAFTING_TABLE) break
                    placePos = pos.up()
                    break
                }
            } else {
                placePos = null
            }
        }

        onLoop {
            if (autoBedPVP.value) {
                if (craftNeeded && mc.currentScreen !is CraftingScreen) {
                    placePos?.let { pos ->
                        if (world.getBlockState(pos).block != Blocks.CRAFTING_TABLE && player.distanceSqToCenter(pos) <= placeRange.value.sq) {
                            player.hotbarSlots.firstBlock(Blocks.CRAFTING_TABLE)?.let { slot ->
                                spoofHotbar(slot) {
                                    RotationManager.addRotations(pos)
                                    connection.sendPacket(fastPos(pos))
                                }
                                if (!craftTask.contains(pos)) {
                                    craftTask.add(pos)
                                }
                            } ?: {
                                placePos = null
                            }
                        }
                    }
                }
            }
        }

        onMotion { event ->
            craftNeeded = !player.inventory.main.any { it.item in bedList }
            var smart = 0
            for (slot in 0..36) {
                if (player.inventory.getStack(slot) == ItemStack.EMPTY) {
                    smart++
                }
            }
            if (autoBedPVP.value) {
                placePos?.let { pos ->
                    for (slot in 0..36) {
                        if (mc.currentScreen is CraftingScreen) break
                        if (world.getBlockState(pos).block == Blocks.CRAFTING_TABLE && prioReady.value) {
                            playerController.interactBlock(
                                player, Hand.MAIN_HAND, BlockHitResult(
                                    pos.toCenterPos(), Direction.UP, pos, false
                                )
                            )
                            connection.sendPacket(
                                RecipeCategoryOptionsC2SPacket(
                                    RecipeBookCategory.CRAFTING, true, false
                                )
                            )
                            placePos = null
                            break
                        }
                    }
                }
                if (craftTask.isNotEmpty()) {
                    craftTask.forEach { pos ->
                        if (mc.currentScreen !is CraftingScreen && player.distanceSqToCenter(pos) <= placeRange.value.sq) {
                            playerController.interactBlock(
                                player, Hand.MAIN_HAND, BlockHitResult(
                                    pos.toCenterPos(), Direction.UP, pos, false
                                )
                            )
                            connection.sendPacket(
                                RecipeCategoryOptionsC2SPacket(
                                    RecipeBookCategory.CRAFTING, true, false
                                )
                            )
                            placePos = null
                            craftTask.clear()
                        } else {
                            craftTask.clear()
                        }
                    }
                }
            }
            if (event.stageType == StageType.END) {
                if (mc.currentScreen is CraftingScreen && mc.currentScreen !is InventoryScreen) {
                    val recipeResultCollectionList = player.recipeBook.orderedResults
                    outerLoop@ for (recipeResultCollection in recipeResultCollectionList) {
                        for (recipe in recipeResultCollection.getRecipes(true)) {
                            if (!bedList.contains(recipe.getOutput(world.registryManager).item)) continue
                            if (!bedList.any { item -> item != recipe.getOutput(world.registryManager).item }) break
                            if (smart > 0) {
                                if (craftTimer.tickAndReset(craftDelay.value)) {
                                    if (fastPacket.value) {
                                        connection.sendPacket(
                                            CraftRequestC2SPacket(
                                                player.currentScreenHandler.syncId, recipe, false
                                            )
                                        )
                                    } else {
                                        mc.interactionManager?.clickRecipe(
                                            player.currentScreenHandler.syncId, recipe, false
                                        )
                                    }
                                }
                                if (!manualCraft.value) {
                                    playerController.clickSlot(
                                        player.currentScreenHandler.syncId, 0, 1, SlotActionType.QUICK_MOVE, player
                                    )
                                    playerController.updateController()
                                }
                                smart--
                            } else {
                                player.closeScreen()
                                break@outerLoop
                            }
                        }
                    }
                }
            }
        }

        onRender3D { event ->
            val scale = if (placePos != null) {
                Easing.OUT_CUBIC.inc(Easing.toDelta(System.currentTimeMillis(), 300))
            } else {
                Easing.IN_CUBIC.dec(Easing.toDelta(System.currentTimeMillis(), 300))
            }
            placePos?.let { pos ->
                val esp = ESPRenderer()
                esp.aFilled = (color.value.alpha * scale).toInt()
                esp.aOutline = (lineColor.value.alpha * scale).toInt()
                esp.add(pos, color.value, lineColor.value)
                esp.render(event.matrices, false)
            }
        }
    }
}