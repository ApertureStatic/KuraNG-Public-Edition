package dev.dyzjct.kura.module.modules.render

import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import base.events.TickEvent
import base.system.event.safeConcurrentListener
import base.system.render.graphic.Render2DEngine
import base.system.render.graphic.Render3DEngine
import base.system.render.newfont.FontRenderers
import base.utils.chat.ChatUtil
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket
import net.minecraft.util.math.Vec3d
import org.joml.Vector4d
import java.awt.Color
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object LogESP : Module(name = "LogESP", langName = "下线位置", category = Category.RENDER) {
    private val playerCache = ConcurrentHashMap<UUID, PlayerEntity>()
    private val logoutCache = ConcurrentHashMap<UUID, PlayerEntity>()
    var color by csetting("Color", Color(-0x77ff0100))

    init {
        onPacketReceive { event ->
            if (event.packet is PlayerListS2CPacket) {
                if (event.packet.actions.equals(PlayerListS2CPacket.Action.ADD_PLAYER)) {
                    for (ple in event.packet.playerAdditionEntries) {
                        for (uuid in logoutCache.keys) {
                            if (ple.profile() == null) continue
                            if (uuid != ple.profile()!!.id) continue
                            logoutCache[uuid]?.let {
                                ChatUtil.sendMessage("${it.name.string} logged${ChatUtil.GREEN} back${ChatUtil.RESET} at at X: ${it.x.toInt()} Y: ${it.y.toInt()} Z: ${it.z.toInt()}")
                            }
                            logoutCache.remove(uuid)
                        }
                    }
                }
                playerCache.clear()
            }

            if (event.packet is PlayerRemoveS2CPacket) {
                for (uuid2 in event.packet.profileIds) {
                    for (uuid in playerCache.keys) {
                        if (uuid != uuid2) continue
                        playerCache[uuid]?.let {
                            ChatUtil.sendMessage("${it.name.string} logged${ChatUtil.RED} out${ChatUtil.RESET} at X: ${it.x.toInt()} Y: ${it.y.toInt()} Z: ${it.z.toInt()}")
                            if (!logoutCache.containsKey(uuid)) logoutCache[uuid] = it
                        }
                    }
                }
                playerCache.clear()
            }
        }

        safeConcurrentListener<TickEvent.Pre> {
            for (target in world.players) {
                if (target == player) continue
                playerCache[target.gameProfile.id] = target
            }
        }

        onRender3D {
            for (uuid in logoutCache.keys) {
                logoutCache[uuid]?.let {
                    Render3DEngine.drawBoxOutline(
                        it.boundingBox,
                        color,
                        2f
                    )
                }
            }
        }

        onRender2D { event ->
            for (uuid in logoutCache.keys) {
                logoutCache[uuid]?.let { data ->
                    var vector = Vec3d(data.x, data.y + 2, data.z)
                    var position0: Vector4d? = null
                    vector = Render3DEngine.worldSpaceToScreenSpace(Vec3d(vector.x, vector.y, vector.z))
                    if (vector.z > 0 && vector.z < 1) {
                        position0 = Vector4d(vector.x, vector.y, vector.z, 0.0)
                        position0.x = vector.x.coerceAtMost(position0.x)
                        position0.y = vector.y.coerceAtMost(position0.y)
                        position0.z = vector.x.coerceAtLeast(position0.z)
                    }
                    val string = data.name.string + " " + String.format(
                        "%.1f",
                        data.health + data.absorptionAmount
                    ) + " X: " + data.x.toInt() + " " + " Z: " + data.z.toInt()
                    position0?.let { position ->
                        val diff = (position.z - position.x).toFloat() / 2
                        val textWidth: Float = FontRenderers.cn.getStringWidth(string) * 1
                        val tagX = ((position.x + diff - textWidth / 2) * 1).toFloat()
                        Render2DEngine.drawRect(
                            event.drawContext.matrices,
                            tagX - 2,
                            (position.y - 13f).toFloat(),
                            textWidth + 4,
                            11f,
                            Color(-0x66ffffff, true)
                        )
                        FontRenderers.cn.drawString(
                            event.drawContext.matrices,
                            string,
                            tagX,
                            position.y.toFloat() - 10,
                            -1
                        )
                    }
                }
            }
        }
    }

    override fun onEnable() {
        playerCache.clear()
        logoutCache.clear()
    }
}