package dev.dyzjct.kura.module.modules.render

import com.mojang.authlib.GameProfile
import com.mojang.blaze3d.systems.RenderSystem
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.utils.animations.Easing
import net.minecraft.client.render.*
import net.minecraft.client.render.entity.model.EntityModelLayers
import net.minecraft.client.render.entity.model.PlayerEntityModel
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.EntityStatuses
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.RotationAxis
import java.awt.Color
import java.util.concurrent.ConcurrentHashMap


object PopChams : Module(
    name = "PopChams",
    langName = "图腾爆炸渲染",
    description = "Rendering on totem pop.",
    category = Category.RENDER
) {
    private val colorFill by csetting("ColorFill", Color(255, 255, 255, 200))
    private val fadeLength by isetting("FadeLength", 200, 0, 1000)
    private val scale_setting by fsetting("Scale", 1f, 0.1f, 2f)

    private var renderPlayers = ConcurrentHashMap<PlayerEntity, Long>()

    init {
        onPacketReceive { event ->
            if (event.packet is EntityStatusS2CPacket) {
                val players = event.packet.getEntity(world)
                if (event.packet.status == EntityStatuses.USE_TOTEM_OF_UNDYING && players is PlayerEntity && player.distanceTo(
                        players
                    ) < 25 && players.name.string != player.name.string
                ) {
                    val entity: PlayerEntity = object : PlayerEntity(
                        mc.world,
                        BlockPos.ORIGIN,
                        players.bodyYaw,
                        GameProfile(players.getUuid(), players.getName().string)
                    ) {
                        override fun isSpectator(): Boolean {
                            return false
                        }

                        override fun isCreative(): Boolean {
                            return false
                        }
                    }
                    entity.copyPositionAndRotation(players)
                    entity.bodyYaw = players.bodyYaw
                    entity.headYaw = players.headYaw
                    entity.handSwingProgress = players.handSwingProgress
                    entity.handSwingTicks = players.handSwingTicks
                    entity.isSneaking = players.isSneaking()
                    entity.limbAnimator.speed = players.limbAnimator.speed
                    renderPlayers[entity] = System.currentTimeMillis()
                }
            }
        }

        onRender3D {
            renderPlayers.forEach { (entity: PlayerEntity, time: Long) ->
                val easing = Easing.IN_CUBIC.dec(Easing.toDelta(time, fadeLength))
                if (System.currentTimeMillis() - time > fadeLength) {
                    renderPlayers.remove(entity)
                } else {
                    RenderSystem.depthMask(false)
                    RenderSystem.disableDepthTest()
                    RenderSystem.enableBlend()
                    RenderSystem.blendFuncSeparate(770, 771, 0, 1)
                    renderEntity(it.matrices, entity, colorFill, easing, scale_setting)
                    RenderSystem.disableBlend()
                    RenderSystem.depthMask(true)
                }
            }
        }
    }

    fun renderEntity(
        matrices: MatrixStack,
        entity: PlayerEntity,
        color: Color,
        easing: Float,
        scale: Float,
    ) {
        val cameraPos = mc.entityRenderDispatcher.camera.pos
        val x = entity.x - cameraPos.x
        val y = entity.y - cameraPos.y
        val z = entity.z - cameraPos.z

        val model = PlayerEntityModel<PlayerEntity>(
            mc.entityModelLoader.getModelPart(EntityModelLayers.PLAYER),
            false
        )

        // 设置动画参数
        model.animateModel(entity, entity.limbAnimator.pos, entity.limbAnimator.speed, mc.tickDelta)
        model.setAngles(
            entity,
            entity.limbAnimator.pos,
            entity.limbAnimator.speed,
            entity.age.toFloat(),
            entity.headYaw - entity.bodyYaw,
            entity.pitch
        )

        val tessellator = Tessellator.getInstance()
        val buffer = tessellator.buffer

        RenderSystem.enableBlend()
        RenderSystem.defaultBlendFunc()
        RenderSystem.setShader { GameRenderer.getPositionColorProgram() }

        // 🟡 第一次渲染：发光描边（稍微放大模型）
        matrices.push()
        matrices.translate(x.toFloat(), y.toFloat(), z.toFloat())
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f - entity.bodyYaw))
        matrices.scale(-1.1f * scale, -1.1f * scale, 1.1f * scale) // 略微放大用于描边
        matrices.translate(0.0f, -1.501f, 0.0f)

        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR)
        model.render(
            matrices,
            buffer,
            15728880, // full brightness
            OverlayTexture.DEFAULT_UV,
            color.red / 255f,
            color.green / 255f,
            color.blue / 255f,
            (color.alpha / 255f) * easing
        )
        tessellator.draw()
        matrices.pop()

        // 可选：第二次渲染本体（标准尺寸，可使用透明色或略深颜色）
        matrices.push()
        matrices.translate(x.toFloat(), y.toFloat(), z.toFloat())
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f - entity.bodyYaw))
        matrices.scale(-1.0f * scale, -1.0f * scale, 1.0f * scale)
        matrices.translate(0.0f, -1.501f, 0.0f)

        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR)
        model.render(
            matrices,
            buffer,
            15728880,
            OverlayTexture.DEFAULT_UV,
            0f,
            0f,
            0f,
            0f // 本体透明（或设为普通颜色）
        )
        tessellator.draw()
        matrices.pop()

        RenderSystem.disableBlend()
    }


    private fun rad(angle: Float): Float {
        return (angle * Math.PI / 180).toFloat()
    }
}