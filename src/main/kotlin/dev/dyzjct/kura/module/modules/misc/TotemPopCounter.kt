package dev.dyzjct.kura.module.modules.misc

import base.utils.chat.ChatUtil
import base.utils.sound.SoundPlayer
import dev.dyzjct.kura.Kura
import dev.dyzjct.kura.event.eventbus.safeEventListener
import dev.dyzjct.kura.event.events.TickEvent
import dev.dyzjct.kura.manager.EntityManager
import dev.dyzjct.kura.manager.FriendManager
import dev.dyzjct.kura.manager.NotificationManager
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.module.hud.NotificationHUD
import dev.dyzjct.kura.module.modules.client.Sound
import net.minecraft.entity.EntityStatuses
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket
import java.util.concurrent.ConcurrentHashMap

object TotemPopCounter : Module(
    name = "TotemPopCounter",
    langName = "图腾爆炸提示",
    description = "Counts how many times players pop",
    category = Category.MISC
) {
    private var mode = msetting("Mode", Mode.Both)
    private var playerList = ConcurrentHashMap<PlayerEntity, Int>()

    enum class Mode {
        Notification, Chat, Both
    }

    init {
        onPacketReceive { event ->
            if (event.packet is EntityStatusS2CPacket) {
                val players = event.packet.getEntity(world)
                if (event.packet.status == EntityStatuses.USE_TOTEM_OF_UNDYING && players is PlayerEntity) {
                    if (playerList.containsKey(players)) {
                        playerList[players]?.let {
                            playerList[players] = it + 1
                        }
                    } else {
                        playerList[players] = 1
                    }

                    val name = players.name.string
                    val pop = playerList[players]
                    if (players.isAlive) {
                        if (FriendManager.isFriend(name) && player != players) {
                            if (mode.value == Mode.Chat || mode.value == Mode.Both) {
                                ChatUtil.sendMessage("Your Friend $name Popped ${ChatUtil.YELLOW}$pop ${NotificationHUD.defaultFontColor()} Totem!")
                                NotificationManager.addNotification(
                                    "${name}Popped ${ChatUtil.colorKANJI}$pop ${NotificationHUD.defaultFontColor()} Totem!"
                                )
                            }
                        } else if (player == players) {
                            if (mode.value == Mode.Chat || mode.value == Mode.Both) {
                                ChatUtil.sendMessage("I Popped ${ChatUtil.colorKANJI}$pop ${NotificationHUD.defaultFontColor()}Totem!")
                            }
                            if (mode.value == Mode.Notification || mode.value == Mode.Both) {
                                NotificationManager.addNotification(
                                    "I Popped ${ChatUtil.RED}$pop ${NotificationHUD.defaultFontColor()}Totem!"
                                )
                            }
                        } else {
                            if (mode.value == Mode.Chat || mode.value == Mode.Both) {
                                ChatUtil.sendMessage("$name Popped ${ChatUtil.colorKANJI}$pop ${NotificationHUD.defaultFontColor()} Totem!")
                            }
                            if (mode.value == Mode.Notification || mode.value == Mode.Both) {
                                NotificationManager.addNotification(
                                    "$name Popped ${ChatUtil.colorKANJI}$pop ${NotificationHUD.defaultFontColor()} Totem!"
                                )
                            }
                        }
                    }
                }
            }
        }

        safeEventListener<TickEvent.Pre> {
            playerList.forEach {
                if (!EntityManager.players.contains(it.key) && it.key.isDead) {
                    if (it.key.name.string != player.name.string) {
                        if (mode.value == Mode.Chat || mode.value == Mode.Both) {
                            ChatUtil.sendMessage("${ChatUtil.GREEN}${it.key.name.string} died after popped ${ChatUtil.RED}${it.value} ${ChatUtil.GREEN} totems!")
                        }
                        if (mode.value == Mode.Notification || mode.value == Mode.Both) {
                            NotificationManager.addNotification(
                                "${ChatUtil.GREEN}${it.key.name.string} died after popped ${ChatUtil.RED}${it.value} ${ChatUtil.GREEN} totems!"
                            )
                        }
                        if (Sound.isEnabled && Sound.ezz) {
                            Kura::class.java.getResourceAsStream("/assets/kura/sounds/EZ.wav")?.let { sound ->
                                SoundPlayer(sound).play(Sound.volume)
                            } ?: run {
                                NotificationManager.addNotification("${ChatUtil.YELLOW}SoundFailed!")
                            }
                        }
                        playerList.remove(it.key)
                    } else if (it.key.name.string == player.name.string) {
                        if (mode.value == Mode.Chat || mode.value == Mode.Both) {
                            ChatUtil.sendMessage("${ChatUtil.RED}I died after popped ${it.value} totems!")
                        }
                        if (mode.value == Mode.Notification || mode.value == Mode.Both) {
                            NotificationManager.addNotification(
                                "${ChatUtil.RED}I died after popped${it.value}  totems!"
                            )
                        }
                        if (Sound.isEnabled && Sound.ezz) {
                            Kura::class.java.getResourceAsStream("/assets/kura/sounds/EZ.wav")?.let { sound ->
                                SoundPlayer(sound).play(Sound.volume)
                            } ?: run {
                                NotificationManager.addNotification("${ChatUtil.YELLOW}SoundFailed!")
                            }
                        }
                        playerList.remove(it.key)
                    }
                }
            }
        }
    }
}