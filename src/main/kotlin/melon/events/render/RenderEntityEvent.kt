package melon.events.render

import melon.system.event.Cancellable
import melon.system.event.Event
import melon.system.event.EventBus
import melon.system.event.IEventPosting
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.entity.EntityRenderer
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.Entity

sealed class RenderEntityEvent(
    val entity: Entity,
) : Event, Cancellable() {

    sealed class All(
        entity: Entity,
        private val yaw: Float,
        private val partialTicks: Float,
        private val matrices: MatrixStack,
        private val vertexConsumers: VertexConsumerProvider,
        private val light: Int
    ) : RenderEntityEvent(entity) {

        class Pre(
            entity: Entity,
            yaw: Float,
            partialTicks: Float,
            matrices: MatrixStack,
            vertexConsumers: VertexConsumerProvider,
            light: Int
        ) : All(entity, yaw, partialTicks, matrices, vertexConsumers, light), IEventPosting by Companion {
            companion object : EventBus()
        }

        class Post(
            entity: Entity,
            yaw: Float,
            partialTicks: Float,
            matrices: MatrixStack,
            vertexConsumers: VertexConsumerProvider,
            light: Int
        ) : All(entity, yaw, partialTicks, matrices, vertexConsumers, light), IEventPosting by Companion {
            companion object : EventBus()
        }
    }

    companion object {
        @JvmStatic
        var renderingEntities = false
    }
}