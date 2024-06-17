package dev.dyzjct.kura.manager

import dev.dyzjct.kura.event.events.ConnectionEvent
import dev.dyzjct.kura.event.events.TickEvent
import dev.dyzjct.kura.event.events.WorldEvent
import dev.dyzjct.kura.event.eventbus.AlwaysListening
import dev.dyzjct.kura.event.eventbus.listener
import dev.dyzjct.kura.event.eventbus.safeParallelListener
import base.utils.entity.EntityUtils.preventEntitySpawning
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.Box

@Suppress("NOTHING_TO_INLINE")
object EntityManager : AlwaysListening {
    private var entity0 = emptyList<Entity>()
    val entity: List<Entity>
        get() = entity0

    private var livingBase0 = emptyList<LivingEntity>()
    val livingBase: List<LivingEntity>
        get() = livingBase0

    private var players0 = emptyList<PlayerEntity>()
    val players: List<PlayerEntity>
        get() = players0

    fun onInit() {
        listener<ConnectionEvent.Disconnect>(Int.MAX_VALUE, true) {
            entity0 = emptyList()
            livingBase0 = emptyList()
            players0 = emptyList()
        }

        listener<WorldEvent.Entity.Add>(Int.MAX_VALUE, true) {
            entity0 = entity0 + it.entity

            if (it.entity is LivingEntity) {
                livingBase0 = livingBase0 + it.entity

                if (it.entity is ClientPlayerEntity) {
                    players0 = players0 + it.entity
                }
            }
        }

        listener<WorldEvent.Entity.Remove>(Int.MAX_VALUE, true) {
            entity0 = entity0 - it.entity

            if (it.entity is LivingEntity) {
                livingBase0 = livingBase0 - it.entity

                if (it.entity is ClientPlayerEntity) {
                    players0 = players0 - it.entity
                }
            }
        }

        safeParallelListener<TickEvent.Post> {
            entity0 = world.entities.toList()
            livingBase0 = world.entities.filterIsInstance<LivingEntity>()
            players0 = world.players.toList()
        }
    }

    inline fun checkEntityCollision(box: Box, noinline predicate: (Entity) -> Boolean): Boolean {
        return entity.asSequence()
            .filter { it.isAlive }
            .filter { it.preventEntitySpawning }
            .filter { it.boundingBox.intersects(box) }
            .filter(predicate)
            .none()
    }

    inline fun checkEntityCollision(box: Box, ignoreEntity: Entity): Boolean {
        return entity.asSequence()
            .filter { it.isAlive }
            .filter { it.preventEntitySpawning }
            .filter { it != ignoreEntity }
            .filter { it.boundingBox.intersects(box) }
            .none()
    }

    inline fun checkEntityCollision(box: Box): Boolean {
        return entity.asSequence()
            .filter { it.isAlive }
            .filter { it.preventEntitySpawning }
            .filter { it.boundingBox.intersects(box) }
            .none()
    }

    inline fun checkAnyEntity(box: Box, noinline predicate: (Entity) -> Boolean): Boolean {
        return entity.asSequence()
            .filter { it.isAlive }
            .filter { it.boundingBox.intersects(box) }
            .filter(predicate)
            .none()
    }

    inline fun checkAnyEntity(box: Box, ignoreEntity: Entity): Boolean {
        return entity.asSequence()
            .filter { it.isAlive }
            .filter { it != ignoreEntity }
            .filter { it.boundingBox.intersects(box) }
            .none()
    }

    inline fun checkAnyEntity(box: Box): Boolean {
        return entity.asSequence()
            .filter { it.isAlive }
            .filter { it.boundingBox.intersects(box) }
            .none()
    }
}