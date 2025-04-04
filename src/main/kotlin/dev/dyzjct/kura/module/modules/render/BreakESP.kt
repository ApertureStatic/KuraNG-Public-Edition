package dev.dyzjct.kura.module.modules.render

import dev.dyzjct.kura.manager.FriendManager
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.module.modules.player.PacketMine
import dev.dyzjct.kura.utils.animations.Easing
import dev.dyzjct.kura.utils.animations.MathUtils
import dev.dyzjct.kura.utils.extension.sq
import dev.dyzjct.kura.event.events.block.BlockBreakEvent
import dev.dyzjct.kura.event.eventbus.SafeClientEvent
import dev.dyzjct.kura.event.eventbus.safeEventListener
import dev.dyzjct.kura.system.render.graphic.Render3DEngine
import dev.dyzjct.kura.utils.block.BlockUtil.canBreak
import dev.dyzjct.kura.utils.block.BlockUtil.getBreakSpeed
import dev.dyzjct.kura.utils.block.BlockUtil.getBreakSpeedObi
import base.utils.graphics.ESPRenderer
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import base.utils.math.distanceSqToCenter
import base.utils.math.scale
import base.utils.math.toBox
import java.awt.Color
import java.util.concurrent.ConcurrentHashMap

@Suppress("unused")
object BreakESP : Module(name = "BreakESP", langName = "挖掘显示", category = Category.RENDER) {
    private var renderMode = msetting("RenderMode", RenderMode.Percent)
    private var renderSelf by bsetting("RenderSelf", false)
    private var renderAir by bsetting("RenderAir", false)
    private var renderRange by isetting("Range", 5, 1, 36)
    private var friendColor by csetting("FriendColor", Color(255, 198, 206, 120))
    private var targetColor by csetting("TargetColor", Color(98, 34, 166, 120))
    private var textColor by csetting("TextColor", Color(98, 245, 32, 120))
    private var statusColor by csetting("StatusColor", Color(98, 245, 32, 120)).enumIs(renderMode, RenderMode.Status)
    private val mineMap = ConcurrentHashMap<String, RenderInfo>()

    init {
        safeEventListener<BlockBreakEvent> { event ->
            if ((!renderSelf && event.breakerID == player.id)) return@safeEventListener
            if (event.blockPos.distanceSqToCenter(player.pos) > renderRange.sq) return@safeEventListener
            if (!canBreak(event.blockPos, false)) return@safeEventListener
            val targetName = world.getEntityById(event.breakerID)?.name?.string ?: return@safeEventListener
            var blockProgress = mineMap[targetName]
            if (blockProgress == null || blockProgress.blockPos != event.blockPos) {
                if (world.getEntityById(event.breakerID) is PlayerEntity) {
                    blockProgress =
                        RenderInfo(
                            event.blockPos.toBox(),
                            event.blockPos,
                            calcBreakTime(event.breakerID, event.blockPos),
                            System.currentTimeMillis()
                        )
                    if (!mineMap.containsKey(targetName)) {
                        mineMap[targetName] = blockProgress
                    } else {
                        mineMap.replace(targetName, blockProgress)
                    }
                }
            }
        }

        onRender3D { event ->
            mineMap.forEach { (targetName, renderInfo) ->
                if (!renderAir && world.isAir(renderInfo.blockPos)) return@forEach
                if (renderInfo.blockPos.distanceSqToCenter(player.pos) > renderRange.sq) return@forEach
                val scale = Easing.OUT_CIRC.inc(Easing.toDelta(renderInfo.startTime, renderInfo.breakTime))
                val color = if (FriendManager.isFriend(targetName)) friendColor else targetColor
                val renderer = ESPRenderer()
                renderer.aOutline = (color.alpha * scale).toInt()
                renderer.aFilled = (color.alpha * scale).toInt()
                renderer.add(renderInfo.renderBox.scale(scale), color)
                renderer.render(event.matrices, false)

                //Render Text
                val textState = when (renderMode.value) {
                    RenderMode.Percent -> {
                        MathUtils.round(scale * 100, 2).toString()
                    }

                    else -> {
                        if (!world.isAir(renderInfo.blockPos)) {
                            "Breaking..."
                        } else {
                            "Broke"
                        }
                    }
                }
                val stateColor = if (renderMode.value == RenderMode.Percent) {
                    when (scale * 100) {
                        in 0.0..50.00 -> {
                            Color(255, 0, 0)
                        }

                        in 50.0..99.0 -> {
                            Color(255, 100, 0)
                        }

                        else -> {
                            Color(0, 255, 0)
                        }
                    }
                } else {
                    statusColor
                }
                Render3DEngine.drawTextIn3D(textState, renderInfo.renderBox.center, 0.0, 0.15, 0.0, stateColor)
                Render3DEngine.drawTextIn3D(targetName, renderInfo.renderBox.center, 0.0, -0.04, 0.0, textColor)
            }
        }
    }

    fun SafeClientEvent.calcBreakTime(target: Int, pos: BlockPos): Float {
        val blockState = world.getBlockState(pos)

        val hardness = blockState.getHardness(world, pos)
        var p: PlayerEntity? = null
        if (world.getEntityById(target) is PlayerEntity) {
            p = world.getEntityById(target) as PlayerEntity
        }
        if (p != null) {
            val breakSpeed = if (p != player) getBreakSpeedObi(blockState) else getBreakSpeed(
                blockState, PacketMine.inventoryTool
            ) //getBreakSpeed(p, blockState)
            if (breakSpeed == -1.0f) {
                return -1f
            }
            val relativeDamage = breakSpeed / hardness / 30.0f
            val ticks = (0.7f / relativeDamage)
            return ticks * 50f
        }
        return 0f
    }

    enum class RenderMode {
        Percent, Status
    }

    class RenderInfo(var renderBox: Box, var blockPos: BlockPos, var breakTime: Float, var startTime: Long)
}