package dev.dyzjct.kura.module.modules.render

import com.mojang.authlib.GameProfile
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.utils.animations.Easing
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
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.RotationAxis
import org.joml.Vector3f
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
    private val globalScale by fsetting("GlobalScale", 1f, 0.1f, 2f)
    private val headScale by fsetting("HeadScale", 0f, 0f, 1f)
    private val bodyScale by fsetting("BodyScale", 0f, 0f, 1f)
    private val legArmScale by fsetting("LegArmScale", 0f, 0f, 1f)
    private val putin by bsetting("Putin", false)

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
                    renderEntity(it.matrices, entity, colorFill.alpha * easing / 255f, easing)
                    RenderSystem.disableBlend()
                    RenderSystem.depthMask(true)
                }
            }
        }
    }

    private fun renderEntity(
        matrices: MatrixStack,
        entity: PlayerEntity,
        alpha: Float,
        easing: Float
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
            modelBase.head.scale(
                Vector3f(
                    (if (putin) if (headScale != 0f) headScale else 0.1f * 3f else headScale),
                    (if (putin) if (headScale != 0f) headScale else 0.1f * 0.1f else headScale),
                    headScale
                )
            )
            modelBase.body.scale(
                Vector3f(
                    (if (putin) if (bodyScale != 0f) bodyScale else 0.1f * 5f else bodyScale),
                    (if (putin) if (bodyScale != 0f) bodyScale else 0.1f * 0.1f else bodyScale),
                    bodyScale
                )
            )
            val lagArmVec = Vector3f(
                (if (putin) if (legArmScale != 0f) legArmScale else 0.1f * 4f else legArmScale),
                (if (putin) if (legArmScale != 0f) legArmScale else 0.1f * 0.1f else legArmScale),
                legArmScale
            )
            modelBase.leftLeg.scale(lagArmVec)
            modelBase.rightLeg.scale(lagArmVec)
            modelBase.leftArm.scale(lagArmVec)
            modelBase.rightArm.scale(lagArmVec)
            matrices.push()
            matrices.translate(x.toFloat(), y.toFloat(), z.toFloat())
            matrices.multiply(RotationAxis.POSITIVE_Y.rotation(rad(180 - entity.bodyYaw)))

            matrices.scale(-1.0f, -1.0f, 1.0f)
            matrices.scale(1.6f * easing * globalScale, 1.8f * easing * globalScale, 1.6f * easing * globalScale)
            matrices.translate(0.0f, -1.501f, 0.0f)

            modelBase.animateModel(
                entity,
                entity.limbAnimator.pos,
                entity.limbAnimator.speed,
                mc.tickDelta
            )
            modelBase.setAngles(
                entity,
                entity.limbAnimator.pos,
                entity.limbAnimator.speed,
                entity.age.toFloat(),
                entity.headYaw - entity.bodyYaw,
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
                colorFill.red / 255f,
                colorFill.green / 255f,
                colorFill.blue / 255f,
                alpha
            )
            tessellator.draw()
            RenderSystem.disableBlend()
            matrices.pop()
        }
    }

    private fun rad(angle: Float): Float {
        return (angle * Math.PI / 180).toFloat()
    }
}