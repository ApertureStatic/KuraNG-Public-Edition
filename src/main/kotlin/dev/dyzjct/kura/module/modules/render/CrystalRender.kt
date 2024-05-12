package dev.dyzjct.kura.module.modules.render

import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.utils.TimerUtils
import dev.dyzjct.kura.utils.extension.sq
import dev.dyzjct.kura.utils.extension.synchronized
import base.events.TickEvent
import base.events.render.Render3DEvent
import base.system.event.safeConcurrentListener
import base.system.render.graphic.Render3DEngine
import net.minecraft.entity.decoration.EndCrystalEntity
import net.minecraft.util.math.BlockPos
import base.utils.math.distanceSqTo
import base.utils.math.distanceSqToCenter
import java.awt.Color
import java.util.concurrent.ConcurrentHashMap

object CrystalRender : Module(name = "CrystalRender", langName = "水晶渲染", category = Category.RENDER, type = Type.Both) {
    private val range = isetting("Range", 12, 0, 30)
    private val mode = msetting("Mode", Mode.Normal)
    private val points = isetting("Points", 20, 1, 100).enumIs(mode, Mode.New)
    private val interval = isetting("Interval", 2, 1, 500).enumIs(mode, Mode.New)
    private val lineWidth = fsetting("LineWidth", 2f, 1f, 4f)
    private val animationTime = isetting("AnimationTime", 500, 0, 1500)
    private val fadeSpeed = dsetting("FadeSpeed", 500.0, 0.0, 1500.0)
    private val color = csetting("Color", Color(255, 255, 255))
    private val cryList = ConcurrentHashMap<BlockPos, RenderInfo>().synchronized()
    private val timerUtils = TimerUtils()

    init {
        onRender3D { event ->
            when (mode.value) {
                Mode.Normal -> {
                    cryList.forEach { (_: BlockPos, renderInfo: RenderInfo) ->
                        drawRender(event, renderInfo.entity, renderInfo.time, renderInfo.time)
                    }
                }

                Mode.New -> {
                    var time = 0
                    for (i in 1..points.value) {
                        if (timerUtils.passedMs(500)) {
                            cryList.forEach { (_: BlockPos, info: RenderInfo) ->
                                drawRender(event, info.entity, info.time - time, info.time - time)
                            }
                            time += interval.value
                        }

                    }
                }
            }
        }

        safeConcurrentListener<TickEvent.Pre> {
            for (entity in world.entities) {
                if (entity !is EndCrystalEntity) continue
                if (player.distanceSqTo(entity.pos) > range.value.sq) continue
                if (cryList[entity.blockPos]?.let { System.currentTimeMillis() - it.time > animationTime.value && !it.entity.isAlive } != false && player.distanceSqToCenter(
                        entity.blockPos
                    ) < range.value.sq) {
                    cryList[entity.blockPos] = RenderInfo(entity, System.currentTimeMillis())
                }
            }
            for (render in cryList) {
                if (System.currentTimeMillis() - render.value.time > animationTime.value) {
                    cryList.remove(render.key)
                }
            }
        }
    }

    private fun drawRender(event: Render3DEvent, entity: EndCrystalEntity, radTime: Long, heightTime: Long) {
        val rad = System.currentTimeMillis() - radTime
        val height = System.currentTimeMillis() - heightTime
        if (rad <= animationTime.value) {
            Render3DEngine.drawSphere(
                event.matrices,
                entity,
                rad / fadeSpeed.value.toFloat(),
                height / 1000.toFloat(),
                lineWidth.value,
                color.value
            )
        }
    }

    override fun onDisable() {
        cryList.clear()
    }

    enum class Mode {
        Normal, New
    }

    class RenderInfo(var entity: EndCrystalEntity, var time: Long)
}