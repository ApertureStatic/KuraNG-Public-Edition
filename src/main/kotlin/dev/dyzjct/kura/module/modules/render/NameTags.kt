package dev.dyzjct.kura.module.modules.render

import dev.dyzjct.kura.manager.FriendManager
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.module.modules.client.Colors
import base.events.render.Render2DEvent
import base.system.event.SafeClientEvent
import base.system.event.safeEventListener
import base.system.render.graphic.Render2DEngine
import base.system.render.graphic.Render3DEngine
import base.system.render.newfont.FontRenderers
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.network.PlayerListEntry
import net.minecraft.client.render.DiffuseLighting
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.effect.StatusEffect
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtList
import net.minecraft.util.Formatting
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import net.minecraft.world.GameMode
import org.joml.Vector4d
import java.awt.Color
import java.math.BigDecimal
import java.math.RoundingMode

object NameTags : Module(name = "NameTags", langName = "名牌显示", category = Category.RENDER, safeModule = true) {
    private val fillColorA = csetting("Color", Color(-0x80000000))
    private val armorMode = msetting("ArmorMode", Armor.Full)
    private val font = msetting("FontMode", Font.Fancy)
    private val gameMode = bsetting("GameMode", true)
    private val ping = bsetting("Ping", true)
    private val health = bsetting("Health", true)
    private val distance = bsetting("Distance", true)
    private val outline = bsetting("Outline", true)
    private val enchantment = bsetting("Enchants", true)
    private val potions = bsetting("Potions", true)
    private val box = bsetting("Box", true)

    @Suppress("UNUSED")
    enum class Font {
        Fancy, Fast
    }

    enum class Armor {
        None, Full, Durability
    }

    init {
        safeEventListener<Render2DEvent> { event ->
            val context = event.drawContext
            for (ent in world.players) {
                if (ent === player && mc.options.perspective.isFirstPerson) continue
                val x: Double = ent.prevX + (ent.x - ent.prevX) * mc.tickDelta
                val y: Double = ent.prevY + (ent.y - ent.prevY) * mc.tickDelta
                val z: Double = ent.prevZ + (ent.z - ent.prevZ) * mc.tickDelta
                var vector = Vec3d(x, y + 2, z)
                var position0: Vector4d? = null
                vector = Render3DEngine.worldSpaceToScreenSpace(Vec3d(vector.x, vector.y, vector.z))
                if (vector.z > 0 && vector.z < 1) {
                    position0 = Vector4d(vector.x, vector.y, vector.z, 0.0)
                    position0.x = vector.x.coerceAtMost(position0.x)
                    position0.y = vector.y.coerceAtMost(position0.y)
                    position0.z = vector.x.coerceAtLeast(position0.z)
                }
                val p: PlayerEntity = ent as PlayerEntity
                var finalString = ""
                if (ping.value) {
                    finalString += getEntityPing(p).toString() + "ms "
                }
                if (gameMode.value) {
                    finalString += translateGamemode(getEntityGamemode(p)) + " "
                }
                finalString += p.displayName.string + " "
                if (health.value) {
                    finalString += getHealthColor(p) + round2((p.absorptionAmount + p.health).toDouble()) + " "
                }
                if (distance.value) {
                    finalString += java.lang.String.format("%.1f", player.distanceTo(p)) + "m "
                }
                position0?.let { position ->
                    val posX = position.x
                    val posY = position.y
                    val endPosX = position.z
                    val diff = (endPosX - posX).toFloat() / 2
                    val textWidth = if (font.value === Font.Fancy) {
                        FontRenderers.cn.getStringWidth(finalString) * 1
                    } else {
                        mc.textRenderer.getWidth(finalString).toFloat()
                    }
                    val tagX = ((posX + diff - textWidth / 2) * 1).toFloat()
                    val stacks = ArrayList<ItemStack>()
                    if (armorMode.value !== Armor.Durability) {
                        stacks.add(p.offHandStack)
                    }
                    stacks.add(p.inventory.armor[0])
                    stacks.add(p.inventory.armor[1])
                    stacks.add(p.inventory.armor[2])
                    stacks.add(p.inventory.armor[3])
                    if (armorMode.value !== Armor.Durability) {
                        stacks.add(p.mainHandStack)
                    }
                    var itemOffset = 0f
                    if (armorMode.value !== Armor.None) for (armorComponent in stacks) {
                        if (!armorComponent.isEmpty) {
                            if (armorMode.value === Armor.Full) {
                                context.matrices.push()
                                context.matrices.translate(
                                    posX - 55 + itemOffset,
                                    (posY - 35f).toFloat().toDouble(),
                                    0.0
                                )
                                context.matrices.scale(1.1f, 1.1f, 1.1f)
                                DiffuseLighting.disableGuiDepthLighting()
                                context.drawItem(armorComponent, 0, 0)
                                context.drawItemInSlot(mc.textRenderer, armorComponent, 0, 0)
                                context.matrices.pop()
                            } else {
                                context.matrices.push()
                                context.matrices.translate(
                                    posX - 35 + itemOffset,
                                    (posY - 20).toFloat().toDouble(),
                                    0.0
                                )
                                context.matrices.scale(0.7f, 0.7f, 0.7f)
                                val durability: Float = (armorComponent.maxDamage - armorComponent.damage).toFloat()
                                val percent: Int = (durability / armorComponent.maxDamage.toFloat() * 100f).toInt()
                                val color = if (percent < 33) {
                                    Color.RED
                                } else if (percent in 34..65) {
                                    Color.YELLOW
                                } else {
                                    Color.GREEN
                                }
                                context.drawText(mc.textRenderer, "$percent%", 0, 0, color.rgb, false)
                                context.matrices.pop()
                            }
                            var enchantmentY = 0f
                            val enchants: NbtList = armorComponent.enchantments
                            if (enchantment.value) for (index in enchants.indices) {
                                val id: String = enchants.getCompound(index).getString("id")
                                val level: Short = enchants.getCompound(index).getShort("lvl")
                                val encName = when (id) {
                                    "minecraft:protection" -> "P$level"
                                    "minecraft:blast_protection" -> "B$level"
                                    "minecraft:thorns" -> "T$level"
                                    "minecraft:sharpness" -> "S$level"
                                    "minecraft:efficiency" -> "E$level"
                                    "minecraft:unbreaking" -> "U$level"
                                    "minecraft:power" -> "PO$level"
                                    else -> continue
                                }
                                if (font.value === Font.Fancy) {
                                    FontRenderers.cn.drawString(
                                        context.matrices,
                                        encName,
                                        posX - 50 + itemOffset,
                                        posY.toFloat() - 45 + enchantmentY.toDouble(),
                                        -1
                                    )
                                } else {
                                    context.drawText(
                                        mc.textRenderer,
                                        encName,
                                        (posX.toInt() - 50 + itemOffset).toInt(),
                                        (posY.toInt() - 45 + enchantmentY).toInt(),
                                        -1,
                                        false
                                    )
                                }
                                enchantmentY -= 8f
                            }
                        }
                        itemOffset += 18f
                    }
                    Render2DEngine.drawRect(
                        context.matrices, tagX - 2, (posY - 13f).toFloat(), textWidth + 4, 11f, fillColorA.value
                    )
                    if (outline.value) {
                        Render2DEngine.drawRect(
                            context.matrices, tagX - 3, (posY - 14f).toFloat(), textWidth + 6, 1f, Colors.getColor(270)
                        )
                        Render2DEngine.drawRect(
                            context.matrices, tagX - 3, (posY - 3f).toFloat(), textWidth + 6, 1f, Colors.getColor(0)
                        )
                        Render2DEngine.drawRect(
                            context.matrices, tagX - 3, (posY - 14f).toFloat(), 1f, 11f, Colors.getColor(180)
                        )
                        Render2DEngine.drawRect(
                            context.matrices, tagX + textWidth + 2, (posY - 14f).toFloat(), 1f, 11f, Colors.getColor(90)
                        )
                    }
                    if (font.value === Font.Fancy) {
                        FontRenderers.cn.drawString(
                            context.matrices,
                            finalString,
                            tagX,
                            posY.toFloat() - 10,
                            if (!FriendManager.isFriend(ent.name.string)) -1 else Color(0, 255, 255).rgb
                        )
                    } else {
                        context.drawText(
                            mc.textRenderer,
                            finalString,
                            tagX.toInt(),
                            (posY.toFloat() - 11).toInt(),
                            if (!FriendManager.isFriend(ent.name.string)) -1 else Color(0, 255, 255).rgb,
                            false
                        )
                    }
                    if (box.value) drawBox(p, context)
                }
            }
        }

    }

    private fun drawBox(ent: PlayerEntity, context: DrawContext) {
        val x: Double = ent.prevX + (ent.x - ent.prevX) * mc.tickDelta
        val y: Double = ent.prevY + (ent.y - ent.prevY) * mc.tickDelta
        val z: Double = ent.prevZ + (ent.z - ent.prevZ) * mc.tickDelta
        val axisAlignedBB2: Box = ent.boundingBox
        val axisAlignedBB = Box(
            axisAlignedBB2.minX - ent.x + x - 0.05,
            axisAlignedBB2.minY - ent.y + y,
            axisAlignedBB2.minZ - ent.z + z - 0.05,
            axisAlignedBB2.maxX - ent.x + x + 0.05,
            axisAlignedBB2.maxY - ent.y + y + 0.15,
            axisAlignedBB2.maxZ - ent.z + z + 0.05
        )
        val vectors: Array<Vec3d> = arrayOf(
            Vec3d(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ),
            Vec3d(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.minZ),
            Vec3d(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.minZ),
            Vec3d(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.minZ),
            Vec3d(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.maxZ),
            Vec3d(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.maxZ),
            Vec3d(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.maxZ),
            Vec3d(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ)
        )
        var position: Vector4d? = null
        for (vector0 in vectors) {
            val vector = Render3DEngine.worldSpaceToScreenSpace(Vec3d(vector0.x, vector0.y, vector0.z))
            if (vector.z > 0 && vector.z < 1) {
                if (position == null) position = Vector4d(vector.x, vector.y, vector.z, 0.0)
                position.x = vector.x.coerceAtMost(position.x)
                position.y = vector.y.coerceAtMost(position.y)
                position.z = vector.x.coerceAtLeast(position.z)
                position.w = vector.y.coerceAtLeast(position.w)
            }
        }
        if (position != null) {
            val posX: Double = position.x
            val posY: Double = position.y
            val endPosX: Double = position.z
            val endPosY: Double = position.w
            Render2DEngine.drawRectDumbWay(
                context.matrices,
                (posX - 1f).toFloat(),
                posY.toFloat(),
                (posX + 0.5).toFloat(),
                (endPosY + 0.5).toFloat(),
                Color.BLACK,
                Color.BLACK,
                Color.BLACK,
                Color.BLACK
            )
            Render2DEngine.drawRectDumbWay(
                context.matrices,
                (posX - 1f).toFloat(),
                (posY - 0.5).toFloat(),
                (endPosX + 0.5).toFloat(),
                (posY + 0.5 + 0.5).toFloat(),
                Color.BLACK,
                Color.BLACK,
                Color.BLACK,
                Color.BLACK
            )
            Render2DEngine.drawRectDumbWay(
                context.matrices,
                (endPosX - 0.5 - 0.5).toFloat(),
                posY.toFloat(),
                (endPosX + 0.5).toFloat(),
                (endPosY + 0.5).toFloat(),
                Color.BLACK,
                Color.BLACK,
                Color.BLACK,
                Color.BLACK
            )
            Render2DEngine.drawRectDumbWay(
                context.matrices,
                (posX - 1).toFloat(),
                (endPosY - 0.5 - 0.5).toFloat(),
                (endPosX + 0.5).toFloat(),
                (endPosY + 0.5).toFloat(),
                Color.BLACK,
                Color.BLACK,
                Color.BLACK,
                Color.BLACK
            )
            Render2DEngine.drawRectDumbWay(
                context.matrices,
                (posX - 0.5f).toFloat(),
                posY.toFloat(),
                (posX + 0.5 - 0.5).toFloat(),
                endPosY.toFloat(),
                Colors.getColor(270),
                Colors.getColor(0),
                Colors.getColor(0),
                Colors.getColor(270)
            )
            Render2DEngine.drawRectDumbWay(
                context.matrices,
                posX.toFloat(),
                (endPosY - 0.5f).toFloat(),
                endPosX.toFloat(),
                endPosY.toFloat(),
                Colors.getColor(0),
                Colors.getColor(180),
                Colors.getColor(180),
                Colors.getColor(0)
            )
            Render2DEngine.drawRectDumbWay(
                context.matrices,
                (posX - 0.5).toFloat(),
                posY.toFloat(),
                endPosX.toFloat(),
                (posY + 0.5).toFloat(),
                Colors.getColor(180),
                Colors.getColor(90),
                Colors.getColor(90),
                Colors.getColor(180)
            )
            Render2DEngine.drawRectDumbWay(
                context.matrices,
                (endPosX - 0.5).toFloat(),
                posY.toFloat(),
                endPosX.toFloat(),
                endPosY.toFloat(),
                Colors.getColor(90),
                Colors.getColor(270),
                Colors.getColor(270),
                Colors.getColor(90)
            )
            Render2DEngine.drawRectDumbWay(
                context.matrices,
                (endPosX + 5).toFloat(),
                posY.toFloat(),
                endPosX.toFloat() + 3,
                endPosY.toFloat(),
                Color.BLACK,
                Color.BLACK,
                Color.BLACK,
                Color.BLACK
            )
            Render2DEngine.drawRectDumbWay(
                context.matrices,
                (endPosX + 5).toFloat(),
                (endPosY + (posY - endPosY) * ent.health / 20f).toFloat(),
                endPosX.toFloat() + 3,
                endPosY.toFloat(),
                Color.RED,
                Color.RED,
                Color.RED,
                Color.RED
            )
            if (potions.value) drawPotions(context.matrices, ent, (endPosX + 7).toFloat(), posY.toFloat())
        }
    }

    private fun drawPotions(matrices: MatrixStack, entity: PlayerEntity, posX: Float, posY: Float) {
        val effects = ArrayList<StatusEffectInstance>()
        var yOffset = 0
        for (potionEffect in entity.statusEffects) {
            if (potionEffect.duration != 0) {
                effects.add(potionEffect)
                val potion: StatusEffect = potionEffect.effectType
                val power: String = when (potionEffect.amplifier) {
                    0 -> "I"
                    1 -> "II"
                    2 -> "III"
                    3 -> "IV"
                    4 -> "V"
                    else -> "114514"
                }
                val s: String = potion.name.string + " " + power
                val s2 = getDuration(potionEffect) + ""
                FontRenderers.cn.drawString(matrices, "$s $s2", posX, posY + yOffset, -1)
                yOffset += 8
            }
        }
    }

    private fun translateGamemode(gameMode: GameMode?): String {
        return if (gameMode == null) "[BOT]" else when (gameMode) {
            GameMode.SURVIVAL -> "[S]"
            GameMode.CREATIVE -> "[C]"
            GameMode.SPECTATOR -> "[SP]"
            GameMode.ADVENTURE -> "[A]"
        }
    }

    private fun getHealthColor(entity: PlayerEntity): String {
        val health: Int = (entity.health.toInt() + entity.absorptionAmount).toInt()
        if (health in 8..15) return Formatting.YELLOW.toString() + ""
        return if (health > 15) Formatting.GREEN.toString() + "" else Formatting.RED.toString() + ""
    }

    private fun getDuration(pe: StatusEffectInstance): String {
        return if (pe.isInfinite) {
            "*:*"
        } else {
            val var1: Int = pe.duration
            val mins = var1 / 1200
            val sec = var1 % 1200 / 20
            "$mins:$sec"
        }
    }

    private fun SafeClientEvent.getEntityPing(entity: PlayerEntity): Int {
        if (mc.networkHandler == null) return 0
        val playerListEntry: PlayerListEntry = connection.getPlayerListEntry(entity.uuid) ?: return 0
        return playerListEntry.latency
    }

    private fun SafeClientEvent.getEntityGamemode(entity: PlayerEntity?): GameMode? {
        if (entity == null) return null
        return connection.getPlayerListEntry(entity.uuid)?.gameMode
    }

    private fun round2(value: Double): Float {
        var bd = BigDecimal(value)
        bd = bd.setScale(1, RoundingMode.HALF_UP)
        return bd.toFloat()
    }
}
