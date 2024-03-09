package dev.dyzjct.kura.module.modules.render

import com.mojang.blaze3d.systems.RenderSystem
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import melon.events.TickEvent
import melon.system.event.SafeClientEvent
import melon.system.event.safeConcurrentListener
import melon.system.render.graphic.Render2DEngine
import melon.system.render.graphic.Render3DEngine
import net.minecraft.block.Blocks
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import java.awt.Color
import java.util.concurrent.CopyOnWriteArrayList

object HoleESP : Module(name = "HoleESP", langName = "坑渲染", category = Category.RENDER) {
    private val positions = CopyOnWriteArrayList<PosWithColor>()
    private val mode = msetting("Mode", Mode.CubeOutline)
    private val rangeXZ = isetting("RangeXY", 10, 1, 128)
    private val rangeY = isetting("RangeY", 5, 1, 128)
    private val obiColor = csetting("ObiColor", Color(Color(0x7A00FF).rgb))
    private val bedRockColor = csetting("BedrockColor", Color(Color(0x00FF51).rgb))
    private val height = fsetting("Height", 1f, 0.01f, 5f)
    private val lineWith = fsetting("LineWidth", 0.5f, 0.01f, 5f)

    private enum class Mode {
        Fade,
        CubeOutline,
        CubeFill,
        CubeBoth
    }

    init {
        safeConcurrentListener<TickEvent.Pre> {
            runCatching {
                findHoles()
            }
        }

        onRender3D { event ->
            if (positions.isEmpty()) return@onRender3D
            for (posWithColor in positions) {
                if (mode.value === Mode.CubeOutline || mode.value === Mode.CubeBoth) {
                    Render3DEngine.drawBoxOutline(
                        Box(
                            posWithColor.pos.x.toDouble(),
                            posWithColor.pos.y.toDouble(),
                            posWithColor.pos.z.toDouble(),
                            (
                                    posWithColor.pos.x + if (posWithColor.dirX) 2f else 1f).toDouble(),
                            (posWithColor.pos.y + height.value).toDouble(),
                            (
                                    posWithColor.pos.z + if (posWithColor.dirZ) 2f else 1f
                                    ).toDouble()
                        ), posWithColor.color, lineWith.value
                    )
                }
                if (mode.value === Mode.CubeFill || mode.value === Mode.CubeBoth) {
                    Render3DEngine.drawFilledBox(
                        event.matrices,
                        Box(
                            posWithColor.pos.x.toDouble(),
                            posWithColor.pos.y.toDouble(),
                            posWithColor.pos.z.toDouble(),
                            (
                                    posWithColor.pos.x + if (posWithColor.dirX) 2f else 1f).toDouble(),
                            (posWithColor.pos.y + height.value).toDouble(),
                            (
                                    posWithColor.pos.z + if (posWithColor.dirZ) 2f else 1f
                                    ).toDouble()
                        ), posWithColor.color
                    )
                }
                if (mode.value === Mode.Fade) {
                    RenderSystem.disableCull()
                    Render3DEngine.drawFilledFadeBox(
                        event.matrices,
                        Box(
                            posWithColor.pos.x.toDouble(),
                            posWithColor.pos.y.toDouble(),
                            posWithColor.pos.z.toDouble(),
                            (
                                    posWithColor.pos.x + if (posWithColor.dirX) 2f else 1f).toDouble(),
                            (posWithColor.pos.y + height.value).toDouble(),
                            (
                                    posWithColor.pos.z + if (posWithColor.dirZ) 2f else 1f
                                    ).toDouble()
                        ),
                        Render2DEngine.injectAlpha(posWithColor.color, 60),
                        Render2DEngine.injectAlpha(posWithColor.color, 0)
                    )
                    Render3DEngine.drawBottomOutline(
                        Box(
                            posWithColor.pos.x.toDouble(),
                            posWithColor.pos.y.toDouble(),
                            posWithColor.pos.z.toDouble(),
                            (
                                    posWithColor.pos.x + if (posWithColor.dirX) 2f else 1f).toDouble(),
                            (posWithColor.pos.y + height.value).toDouble(),
                            (
                                    posWithColor.pos.z + if (posWithColor.dirZ) 2f else 1f
                                    ).toDouble()
                        ), posWithColor.color, lineWith.value
                    )
                    RenderSystem.enableCull()
                }
            }
        }
    }

    private fun SafeClientEvent.findHoles() {
        val bloks = CopyOnWriteArrayList<PosWithColor>()
        val centerPos = player.blockPos
        for (i in centerPos.x - rangeXZ.value until centerPos.x + rangeXZ.value) {
            for (j in centerPos.y - rangeY.value until centerPos.y + rangeY.value) {
                for (k in centerPos.z - rangeXZ.value until centerPos.z + rangeXZ.value) {
                    val pos = BlockPos(i, j, k)
                    if (validIndestructible(pos)) {
                        bloks.add(PosWithColor(pos, false, false, obiColor.value))
                    } else if (validBedrock(pos)) {
                        bloks.add(PosWithColor(pos, false, false, bedRockColor.value))
                    } else if (validTwoBlockBedrockXZ(pos)) {
                        bloks.add(PosWithColor(pos, true, false, bedRockColor.value))
                    } else if (validTwoBlockIndestructibleXZ(pos)) {
                        bloks.add(PosWithColor(pos, true, false, obiColor.value))
                    } else if (validTwoBlockBedrockXZ1(pos)) {
                        bloks.add(PosWithColor(pos, false, true, bedRockColor.value))
                    } else if (validTwoBlockIndestructibleXZ1(pos)) {
                        bloks.add(PosWithColor(pos, false, true, obiColor.value))
                    } else if (validQuadBedrock(pos)) {
                        bloks.add(PosWithColor(pos, true, true, bedRockColor.value))
                    } else if (validQuadIndestructible(pos)) {
                        bloks.add(PosWithColor(pos, true, true, obiColor.value))
                    }
                }
            }
        }
        positions.clear()
        positions.addAll(bloks)
    }

    class PosWithColor(var pos: BlockPos, var dirX: Boolean, var dirZ: Boolean, var color: Color)

    private fun SafeClientEvent.validIndestructible(pos: BlockPos): Boolean {
        return (!validBedrock(pos)
                && (isIndestructible(pos.add(0, -1, 0)) || isBedrock(pos.add(0, -1, 0)))
                && (isIndestructible(pos.add(1, 0, 0)) || isBedrock(pos.add(1, 0, 0)))
                && (isIndestructible(pos.add(-1, 0, 0)) || isBedrock(pos.add(-1, 0, 0)))
                && (isIndestructible(pos.add(0, 0, 1)) || isBedrock(pos.add(0, 0, 1)))
                && (isIndestructible(pos.add(0, 0, -1)) || isBedrock(pos.add(0, 0, -1)))
                && isAir(pos)
                && isAir(pos.add(0, 1, 0))
                && isAir(pos.add(0, 2, 0)))
    }

    private fun SafeClientEvent.validBedrock(pos: BlockPos): Boolean {
        return (isBedrock(pos.add(0, -1, 0))
                && isBedrock(pos.add(1, 0, 0))
                && isBedrock(pos.add(-1, 0, 0))
                && isBedrock(pos.add(0, 0, 1))
                && isBedrock(pos.add(0, 0, -1))
                && isAir(pos)
                && isAir(pos.add(0, 1, 0))
                && isAir(pos.add(0, 2, 0)))
    }

    private fun SafeClientEvent.validTwoBlockIndestructibleXZ(pos: BlockPos): Boolean {
        return ((isIndestructible(pos.down()) || isBedrock(pos.down()))
                && (isIndestructible(pos.west()) || isBedrock(pos.west()))
                && (isIndestructible(pos.south()) || isBedrock(pos.south()))
                && (isIndestructible(pos.north()) || isBedrock(pos.north()))
                && isAir(pos)
                && isAir(pos.up())
                && isAir(pos.up(2))
                && (isIndestructible(pos.east().down()) || isBedrock(pos.east().down()))
                && (isIndestructible(pos.east(2)) || isBedrock(pos.east(2)))
                && (isIndestructible(pos.east().south()) || isBedrock(pos.east().south()))
                && (isIndestructible(pos.east().north()) || isBedrock(pos.east().north()))
                && isAir(pos.east())
                && isAir(pos.east().up())
                && isAir(pos.east().up(2)))
    }

    private fun SafeClientEvent.validTwoBlockIndestructibleXZ1(pos: BlockPos): Boolean {
        return ((isIndestructible(pos.down()) || isBedrock(pos.down()))
                && (isIndestructible(pos.west()) || isBedrock(pos.west()))
                && (isIndestructible(pos.east()) || isBedrock(pos.east()))
                && (isIndestructible(pos.north()) || isBedrock(pos.north()))
                && isAir(pos)
                && isAir(pos.up())
                && isAir(pos.up(2))
                && (isIndestructible(pos.south().down()) || isBedrock(pos.south().down()))
                && (isIndestructible(pos.south(2)) || isBedrock(pos.south(2)))
                && (isIndestructible(pos.south().east()) || isBedrock(pos.south().east()))
                && (isIndestructible(pos.south().west()) || isBedrock(pos.south().west()))
                && isAir(pos.south())
                && isAir(pos.south().up())
                && isAir(pos.south().up(2)))
    }

    fun SafeClientEvent.validQuadIndestructible(pos: BlockPos): Boolean {
        return (isIndestructible(pos.down()) || isBedrock(pos.down())) && isAir(
            pos
        ) && isAir(pos.up()) && isAir(pos.up(2)) && (isIndestructible(pos.south().down()) || isBedrock(
            pos.south().down()
        )) && isAir(pos.south()) && isAir(
            pos.south().up()
        ) && isAir(pos.south().up(2)) && (isIndestructible(pos.east().down()) || isBedrock(
            pos.east().down()
        )) && isAir(pos.east()) && isAir(
            pos.east().up()
        ) && isAir(pos.east().up(2)) && (isIndestructible(
            pos.south().east().down()
        ) || isBedrock(pos.south().east().down())) && isAir(
            pos.south().east()
        ) && isAir(pos.south().east().up()) && isAir(
            pos.south().east().up(2)
        ) && (isIndestructible(pos.north()) || isBedrock(pos.north())) && (isIndestructible(
            pos.west()
        ) || isBedrock(pos.west())) && (isIndestructible(pos.east().north()) || isBedrock(
            pos.east().north()
        )) && (isIndestructible(pos.east().east()) || isBedrock(
            pos.east().east()
        )) && (isIndestructible(pos.south().west()) || isBedrock(
            pos.south().west()
        )) && (isIndestructible(pos.south().south()) || isBedrock(
            pos.south().south()
        )) && (isIndestructible(
            pos.east().south().south()
        ) || isBedrock(pos.east().south().south())) && (isIndestructible(
            pos.east().south().east()
        ) || isBedrock(pos.east().south().east()))
    }

    fun SafeClientEvent.validQuadBedrock(pos: BlockPos): Boolean {
        return isBedrock(pos.down()) && isAir(pos) && isAir(pos.up()) && isAir(
            pos.up(2)
        ) && isBedrock(
            pos.south().down()
        ) && isAir(pos.south()) && isAir(
            pos.south().up()
        ) && isAir(pos.south().up(2)) && isBedrock(
            pos.east().down()
        ) && isAir(pos.east()) && isAir(
            pos.east().up()
        ) && isAir(pos.east().up(2)) && isBedrock(pos.south().east().down()) && isAir(
            pos.south().east()
        ) && isAir(pos.south().east().up()) && isAir(
            pos.south().east().up(2)
        ) && isBedrock(pos.north()) && isBedrock(pos.west()) && isBedrock(
            pos.east().north()
        ) && isBedrock(pos.east().east()) && isBedrock(pos.south().west()) && isBedrock(
            pos.south().south()
        ) && isBedrock(pos.east().south().south()) && isBedrock(
            pos.east().south().east()
        )
    }

    fun SafeClientEvent.validTwoBlockBedrockXZ(pos: BlockPos): Boolean {
        return (isBedrock(pos.down())
                && isBedrock(pos.west())
                && isBedrock(pos.south())
                && isBedrock(pos.north())
                && isAir(pos)
                && isAir(pos.up())
                && isAir(pos.up(2))
                && isBedrock(pos.east().down())
                && isBedrock(pos.east(2))
                && isBedrock(pos.east().south())
                && isBedrock(pos.east().north())
                && isAir(pos.east())
                && isAir(pos.east().up())
                && isAir(pos.east().up(2)))
    }

    fun SafeClientEvent.validTwoBlockBedrockXZ1(pos: BlockPos): Boolean {
        return (isBedrock(pos.down())
                && isBedrock(pos.west())
                && isBedrock(pos.east())
                && isBedrock(pos.north())
                && isAir(pos)
                && isAir(pos.up())
                && isAir(pos.up(2))
                && isBedrock(pos.south().down())
                && isBedrock(pos.south(2))
                && isBedrock(pos.south().east())
                && isBedrock(pos.south().west())
                && isAir(pos.south())
                && isAir(pos.south().up())
                && isAir(pos.south().up(2)))
    }

    private fun SafeClientEvent.isIndestructible(bp: BlockPos): Boolean {
        return world.getBlockState(bp).block === Blocks.OBSIDIAN || world.getBlockState(bp)
            .block === Blocks.NETHERITE_BLOCK || world.getBlockState(bp)
            .block === Blocks.CRYING_OBSIDIAN || world.getBlockState(bp)
            .block === Blocks.RESPAWN_ANCHOR
    }

    private fun SafeClientEvent.isBedrock(bp: BlockPos): Boolean {
        return world.getBlockState(bp).block == Blocks.BEDROCK
    }

    private fun SafeClientEvent.isAir(bp: BlockPos): Boolean {
        return world.getBlockState(bp).block == Blocks.AIR
    }
}
