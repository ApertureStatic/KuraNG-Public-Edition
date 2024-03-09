package dev.dyzjct.kura.module.modules.render

import com.mojang.authlib.GameProfile
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.utils.animations.Easing
import net.minecraft.client.network.OtherClientPlayerEntity
import net.minecraft.client.render.GameRenderer
import net.minecraft.client.render.Tessellator
import net.minecraft.client.render.VertexFormat
import net.minecraft.client.render.VertexFormats
import net.minecraft.client.render.entity.EntityRendererFactory
import net.minecraft.client.render.entity.model.EntityModelLayers
import net.minecraft.client.render.entity.model.PlayerEntityModel
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.EntityStatuses
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket
import net.minecraft.util.math.RotationAxis
import java.awt.Color
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object PopChams : Module(
    name = "PopChams",
    langName = "图腾爆炸渲染",
    description = "Rendering on totem pop.",
    category = Category.RENDER
) {
    private val colorFill by csetting("ColorFill", Color(255, 255, 255, 200))

    //    private val colorLine by csetting("Color", Color(255, 255, 255, 255))
    private val fadeLength by isetting("FadeLength", 200, 0, 1000)

    private var renderPlayers = ConcurrentHashMap<OtherClientPlayerEntity, Long>()

    init {
        onPacketReceive { event ->
            if (event.packet is EntityStatusS2CPacket) {
                val players = event.packet.getEntity(world)
                if (event.packet.status == EntityStatuses.USE_TOTEM_OF_UNDYING && players is PlayerEntity) {
                    val clonedPlayer = OtherClientPlayerEntity(
                        world,
                        GameProfile(UUID.fromString("60569353-f22b-42da-b84b-d706a65c5ddf"), players.entityName)
                    )
                    clonedPlayer.copyPositionAndRotation(players)
                    clonedPlayer.yaw = players.yaw
                    clonedPlayer.pitch = players.pitch
                    clonedPlayer.bodyYaw = players.bodyYaw
                    renderPlayers[clonedPlayer] = System.currentTimeMillis()
                }
            }
        }

        onRender3D {
            runCatching {
                renderPlayers.forEach { (entity: OtherClientPlayerEntity, time: Long) ->
                    if (System.currentTimeMillis() - time > fadeLength) {
                        renderPlayers.remove(entity)
                    } else {
                        val scale = Easing.IN_CUBIC.dec(Easing.toDelta(time, fadeLength))
                        renderEntity(it.matrices, entity, colorFill.alpha * scale)
                    }
                }
            }
        }
    }

    private fun renderEntity(
        matrices: MatrixStack,
        entity: OtherClientPlayerEntity,
        alpha: Float
    ) {
        val x = entity.x - mc.entityRenderDispatcher.camera.pos.getX()
        val y = entity.y - mc.entityRenderDispatcher.camera.pos.getY()
        val z = entity.z - mc.entityRenderDispatcher.camera.pos.getZ()
        PlayerEntityModel<PlayerEntity>(
            EntityRendererFactory.Context(
                mc.entityRenderDispatcher,
                mc.itemRenderer,
                mc.blockRenderManager,
                mc.entityRenderDispatcher.heldItemRenderer,
                mc.resourceManager,
                mc.entityModelLoader,
                mc.textRenderer
            ).getPart(EntityModelLayers.PLAYER), false
        ).let { modelBase ->
            matrices.push()
            matrices.translate(x.toFloat(), y.toFloat(), z.toFloat())
            matrices.multiply(
                RotationAxis.POSITIVE_Y.rotation(((180 - entity.bodyYaw) * Math.PI).toFloat())
            )
            prepareScale(matrices)
            modelBase.animateModel(
                entity as PlayerEntity,
                entity.limbAnimator.pos,
                entity.limbAnimator.speed,
                mc.tickDelta
            )
            modelBase.setAngles(
                entity,
                entity.limbAnimator.pos,
                entity.limbAnimator.speed,
                entity.age.toFloat(),
                entity.headYaw,
                entity.getPitch()
            )
            RenderSystem.enableBlend()
            val tessellator = Tessellator.getInstance()
            val buffer = tessellator.buffer
            RenderSystem.blendFuncSeparate(
                GlStateManager.SrcFactor.SRC_ALPHA,
                GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SrcFactor.ONE,
                GlStateManager.DstFactor.ZERO
            )
            RenderSystem.setShader { GameRenderer.getPositionColorProgram() }
            buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR)
            modelBase.render(
                matrices,
                buffer,
                10,
                0,
                colorFill.red.toFloat(),
                colorFill.green.toFloat(),
                colorFill.blue.toFloat(),
                alpha
            )
            tessellator.draw()
            RenderSystem.disableBlend()
            matrices.pop()
        }
    }

    private fun prepareScale(matrixStack: MatrixStack) {
        matrixStack.scale(-1.0f, -1.0f, 1.0f)
        matrixStack.scale(1.6f, 1.8f, 1.6f)
        matrixStack.translate(0.0f, -1.501f, 0.0f)
    }
}