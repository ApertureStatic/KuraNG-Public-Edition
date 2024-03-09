package dev.dyzjct.kura.module.modules.combat

import dev.dyzjct.kura.manager.HotbarManager.spoofHotbar
import dev.dyzjct.kura.manager.HotbarManager.spoofHotbarBypass
import dev.dyzjct.kura.manager.RotationManager
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.utils.TimerUtils
import dev.dyzjct.kura.utils.inventory.HotbarSlot
import base.system.event.SafeClientEvent
import base.system.util.interfaces.DisplayEnum
import base.utils.block.BlockUtil.getNeighbor
import base.utils.block.isLiquidBlock
import base.utils.block.isWater
import base.utils.extension.fastPos
import base.utils.extension.position
import base.utils.extension.positionRotation
import base.utils.extension.sendSequencedPacket
import base.utils.inventory.slot.firstBlock
import base.utils.inventory.slot.hotbarSlots
import net.minecraft.block.*
import net.minecraft.entity.Entity
import net.minecraft.entity.decoration.EndCrystalEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.util.Hand
import net.minecraft.util.math.*
import team.exception.melon.util.math.toBlockPos
import java.util.*
import java.util.function.Consumer
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sqrt

object Burrow : Module(
    name = "Burrow",
    langName = "下体卡黑曜石",
    category = Category.COMBAT,
    description = "HanLIngQI Bypass Burrow"
) {
    private var rotate by bsetting("Rotate", true)
    private var fakeJumpMode = msetting("FakeJumpMode", FakeJumpMode.Strict)
    private var packetMode = msetting("PacketMode", PacketMode.Normal)
    private var bypass by bsetting("Bypass", false)
    private var cancelMotion by bsetting("CancelMotion", false)
    private var spoofBypass by bsetting("SpoofBypass", false)
    private var strictDirection by bsetting("StrictDirection", false)
    private var delay by isetting("Delay", 50, 0, 250)
    private var timer = TimerUtils()
    private var ignore = false

    init {
        onMotion {
            val slot =
                player.hotbarSlots.firstBlock(Blocks.OBSIDIAN) ?: player.hotbarSlots.firstBlock(Blocks.ENDER_CHEST)
            if (slot == null) {
                disable()
                return@onMotion
            }
            if (!player.onGround) {
                toggle()
                return@onMotion
            }
            if (!timer.passedMs(delay.toLong())) {
                toggle()
                return@onMotion
            }

            if (cancelMotion) player.velocity.y = 0.0

            if (canPlace(player.blockPos.down())) {
                val vec = player.pos.add(0.0, -1.0, 0.0)
                if (rotate) RotationManager.addRotations(player.blockPos.down(), true)
                RotationManager.stopRotation()
                sendPlayerRotation(player.yaw, 90f, false)
                RotationManager.startRotation()
                doSneak()
                placeBlock(player.blockPos.down(), slot)
                place(vec.add(0.3, 0.3, 0.3), slot)
                place(vec.add(-0.3, 0.3, -0.3), slot)
                place(vec.add(-0.3, 0.3, 0.3), slot)
                place(vec.add(0.3, 0.3, -0.3), slot)
                cancelSneak()
            }
            val vec = player.pos
            breakCrystal()
            if (player.onGround) {
                when (fakeJumpMode.value) {
                    FakeJumpMode.Normal -> {
                        connection.sendPacket(
                            positionRotation(0.41999998688698)
                        )
                        connection.sendPacket(
                            positionRotation(0.7531999805212)
                        )
                        connection.sendPacket(
                            positionRotation(1.001335979)
                        )
                        connection.sendPacket(
                            positionRotation(1.16610926)
                        )
                    }

                    FakeJumpMode.Strict -> {
                        val selfPos: BlockPos = getFillBlock() ?: return@onMotion
                        val headFillMode: Boolean = selfPos.y > player.y
                        val fakeJumpOffset = getFakeJumpOffset(selfPos, headFillMode)
                        doFakeJump(fakeJumpOffset)
                    }
                }
            }
            if (rotate) RotationManager.addRotations(player.yaw, 90f, true)
            RotationManager.stopRotation()
            sendPlayerRotation(-180f, 90f, false)
            doSneak()
            place(player.pos, slot)
            place(vec.add(0.3, 0.3, 0.3), slot)
            place(vec.add(-.3, 0.3, -0.3), slot)
            place(vec.add(-0.3, 0.3, 0.3), slot)
            place(vec.add(0.3, 0.3, -0.3), slot)
            cancelSneak()

            // stop move
            // player.input.resetMove()

            when (packetMode.value) {
                PacketMode.Normal -> {
                    val clip: Vec3i = getClip()
                    val pos: BlockPos = getFlooredPosition(player as Entity)
                        .add(0, (clip.y + 4.5).toInt(), 0)
                    connection.sendPacket(
                        PlayerMoveC2SPacket.PositionAndOnGround(
                            pos.x + 0.3, pos.y.toDouble(), pos.z + 0.3, true
                        )
                    )
                }

                PacketMode.Strict -> {
                    connection.sendPacket(positionRotation(1.266109260938214))
                    connection.sendPacket(positionRotation(3.000000458100))
                    connection.sendPacket(positionRotation(-2.26158745548))
                    connection.sendPacket(positionRotation(1.000000000000414))
                    connection.sendPacket(positionRotation(-1.000000000000414))
                }

                PacketMode.NCP -> {
                    sendPlayerPos(player.x, player.y + sendPackets(), player.z, false)
                }

                PacketMode.AAC -> {
                    //极端情况发y1337可以重置击退一样可以不弹
                    sendPlayerPos(player.x, player.y + 1.16610926093821, player.z, false)
                    //1.6第一组假跳数据
                    sendPlayerPos(player.x, player.y + 1.170005801788139, player.z, false)
                    sendPlayerPos(player.x, player.y + 1.2426308013947485, player.z, false)
                    //1.2AAC第一次拉回  y 0
                    sendPlayerPos(player.x, player.y + 2.3400880035762786, player.z, false)
                    //2.3AAC第二次拉回  y 0 + 2.3 onGround true
                    sendPlayerPos(player.x, player.y + 2.640088003576279, player.z, true)
                    //2.6AAC判定飞行第三次拉回
                    sendPlayerPos(player.x, player.y + sendPackets(), player.z, true)
                    //拉回之后重新发一次合法包 不然会卡住
                }

                PacketMode.China -> {
                    var boost = (player.y - 3) * -1
                    if (player.y >= 65) {
                        boost -= boost - player.y
                    }
                    connection.sendPacket(position(-8 + boost))
                }

                PacketMode.Xin -> {
                    //XinBypass
                    for (i in 0..19) connection.sendPacket(
                        PlayerMoveC2SPacket.PositionAndOnGround(
                            player.x, player.y + 1337, player.z, false
                        )
                    )
                    connection.sendPacket(
                        PlayerMoveC2SPacket.PositionAndOnGround(
                            player.x, player.y - 1300, player.z, false
                        )
                    )
                }

                PacketMode.OFF -> {}
            }
            RotationManager.startRotation()
        }

        onPacketReceive { event ->
            if (event.packet is PlayerMoveC2SPacket.PositionAndOnGround) {
                val pack: PlayerMoveC2SPacket.PositionAndOnGround = event.packet
                val floored: BlockPos = getPosFloored(player)
                @Suppress("DEPRECATION")
                if (world.getBlockState(floored).isSolid && !world.getBlockState(floored.add(0, 1, 0)).isSolid) {
                    if (bypass && !ignore) {
                        player.velocity.y = 0.0
                        if (world.getBlockState(
                                getPlayerPosFloored(player).add(0, 1, 0)
                            ).isOpaque
                        ) {
                            player.setPosition(player.x, player.y - 1e-10, player.z)
                            ignore = true
                            connection.sendPacket(
                                PlayerMoveC2SPacket.PositionAndOnGround(
                                    player.x, player.y, player.z, false
                                )
                            )
                            connection.sendPacket(
                                PlayerMoveC2SPacket.PositionAndOnGround(
                                    player.x, player.y + 1000, player.z, false
                                )
                            )
                            ignore = false
                            pack.y = pack.y.roundToInt() - 1e-10
                        }
                    }
                }
            }
        }
        disable()
        timer.reset()
    }

    private fun SafeClientEvent.place(vec3d: Vec3d, slot: HotbarSlot) {
        if (getNeighbor(
                vec3d.toBlockPos(), strictDirection
            ) == null
        ) {
            return
        }
        val pos = vec3d.toBlockPos()

        placeBlock(pos.down(), slot)
        if (!canPlace(pos)) {
            return
        }
        if (getNeighbor(pos, false) == null) return
        if (rotate) {
            RotationManager.addRotations(player.yaw, 90.0f, true)
        }
        if (spoofBypass) {
            spoofHotbarBypass(slot) {
                sendSequencedPacket(world) {
                    fastPos(pos, strictDirection, sequence = it)
                }
            }
        } else {
            spoofHotbar(slot) {
                sendSequencedPacket(world) {
                    fastPos(pos, strictDirection, sequence = it)
                }
            }
        }
        connection.sendPacket(HandSwingC2SPacket(Hand.MAIN_HAND))
    }

    private fun SafeClientEvent.placeBlock(pos: BlockPos, slot: HotbarSlot) {
        if (!canPlace(pos)) return
        if (rotate) {
            RotationManager.addRotations(player.yaw, 90.0f, true)
        }
        spoofHotbar(slot) {
            sendSequencedPacket(world) {
                fastPos(pos, strictDirection, sequence = it)
            }
        }
        connection.sendPacket(HandSwingC2SPacket(Hand.MAIN_HAND))
    }

    private fun SafeClientEvent.breakCrystal() {
        for (entity in world.entities.filter {
            it is EndCrystalEntity && (it.boundingBox.intersects(Box(player.blockPos)) || it.boundingBox.intersects(
                Box(
                    player.pos.add(0.3, 0.0, 0.3).toBlockPos()
                )
            ) || it.boundingBox.intersects(
                Box(
                    player.pos.add(-0.3, 0.0, 0.3).toBlockPos()
                )
            ) || it.boundingBox.intersects(
                Box(
                    player.pos.add(-0.3, 0.0, -0.3).toBlockPos()
                )
            ) || it.boundingBox.intersects(Box(player.pos.add(0.3, 0.0, -0.3).toBlockPos())))
        }) {
            if (entity is EndCrystalEntity) {
                RotationManager.addRotations(entity.blockPos, true)
                connection.sendPacket(
                    PlayerInteractEntityC2SPacket.attack(
                        world.getEntityById(entity.id), player.isSneaking
                    )
                )
                connection.sendPacket(HandSwingC2SPacket(Hand.MAIN_HAND))
            }
        }
    }

    private fun SafeClientEvent.getFillBlock(): BlockPos? {
        @Suppress("DEPRECATION")
        val collect: List<BlockPos> = getFeetBlock(0).stream().filter { blockPos: BlockPos? ->
            !world.getBlockState(blockPos).isSolid
        }.filter { p: BlockPos -> !cantBlockPlace(p) }.limit(1L).toList()
        return if (collect.isEmpty()) {
            null
        } else collect[0]
    }

    private fun SafeClientEvent.cantBlockPlace(blockPos: BlockPos): Boolean {
        return if (world.getBlockState(blockPos.add(0, 0, 1)).block === Blocks.AIR && world.getBlockState(
                blockPos.add(0, 0, -1)
            ).block === Blocks.AIR && world.getBlockState(blockPos.add(1, 0, 0))
                .block === Blocks.AIR && world.getBlockState(blockPos.add(-1, 0, 0))
                .block === Blocks.AIR && world.getBlockState(blockPos.add(0, 1, 0))
                .block === Blocks.AIR && world.getBlockState(blockPos.add(0, -1, 0)).block === Blocks.AIR
        ) {
            true
        } else !world.getBlockState(blockPos).isAir && world.getBlockState(blockPos).block !== Blocks.FIRE
    }

    private fun SafeClientEvent.getFeetBlock(yOff: Int): LinkedHashSet<BlockPos> {
        val set = LinkedHashSet<BlockPos>()
        set.add(BlockPos.ofFloored(player.getPos().add(0.0, yOff.toDouble(), 0.0)))
        set.add(BlockPos.ofFloored(player.getPos().add(0.3, yOff.toDouble(), 0.3)))
        set.add(BlockPos.ofFloored(player.getPos().add(-0.3, yOff.toDouble(), 0.3)))
        set.add(BlockPos.ofFloored(player.getPos().add(0.3, yOff.toDouble(), -0.3)))
        set.add(BlockPos.ofFloored(player.getPos().add(-0.3, yOff.toDouble(), -0.3)))
        return set
    }

    private fun SafeClientEvent.getClip(): Vec3i {
        val playerPos = player.blockPos

        if (isSelfBurrowClipPos(playerPos.add(0, 5, 0))) return Vec3i(0, 5, 0)
        if (isSelfBurrowClipPos(playerPos.add(0, 5, 0))) return Vec3i(0, 5, 0)
        if (isSelfBurrowClipPos(playerPos.add(0, 5, 0))) return Vec3i(0, 5, 0)
        if (isSelfBurrowClipPos(playerPos.add(0, 5, 0))) return Vec3i(0, 5, 0)
        return if (isSelfBurrowClipPos(playerPos.add(0, 5, 0))) Vec3i(0, 5, 0) else Vec3i(0, 5, 0)
    }

    @Suppress("DEPRECATION")
    private fun SafeClientEvent.isSelfBurrowClipPos(p: BlockPos): Boolean {
        return !world.getBlockState(p).isSolid && !world.getBlockState(
            p.add(
                0, 4, 0
            )
        ).isSolid
    }

    private fun SafeClientEvent.getFakeJumpOffset(burBlock: BlockPos, headFillMode: Boolean): List<Vec3d> {
        val offsets: MutableList<Vec3d> = LinkedList()
        if (headFillMode) {
            if (fakeBoXxCheck(this.player, Vec3d(0.0, 2.0, 0.0))) {
                val offVec: Vec3d = direction(burBlock)
                offsets.add(
                    Vec3d(
                        this.player.x + offVec.x * 0.42132, this.player.y + 0.4199999868869781,
                        this.player.z + offVec.z * 0.42132
                    )
                )
                offsets.add(
                    Vec3d(
                        this.player.x + offVec.x * 0.95, this.player.y + 0.7531999805212017,
                        this.player.z + offVec.z * 0.95
                    )
                )
                offsets.add(
                    Vec3d(
                        this.player.x + offVec.x * 1.03, this.player.y + 0.9999957640154541,
                        this.player.z + offVec.z * 1.03
                    )
                )
                offsets.add(
                    Vec3d(
                        this.player.x + offVec.x * 1.0933, this.player.y + 1.1661092609382138,
                        this.player.z + offVec.z * 1.0933
                    )
                )
            } else {
                val offVec2: Vec3d = direction(burBlock)
                offsets.add(
                    Vec3d(
                        this.player.x + offVec2.x * 0.42132, this.player.y + 0.12160004615784,
                        this.player.z + offVec2.z * 0.42132
                    )
                )
                offsets.add(
                    Vec3d(
                        this.player.x + offVec2.x * 0.95, this.player.y + 0.200000047683716,
                        this.player.z + offVec2.z * 0.95
                    )
                )
                offsets.add(
                    Vec3d(
                        this.player.x + offVec2.x * 1.03, this.player.y + 0.200000047683716,
                        this.player.z + offVec2.z * 1.03
                    )
                )
                offsets.add(
                    Vec3d(
                        this.player.x + offVec2.x * 1.0933, this.player.y + 0.12160004615784,
                        this.player.z + offVec2.z * 1.0933
                    )
                )
            }
        } else if (fakeBoXxCheck(this.player, Vec3d(0.0, 2.0, 0.0))) {
            offsets.add(Vec3d(this.player.x, this.player.y + 0.4199999868869781, this.player.z))
            offsets.add(Vec3d(this.player.x, this.player.y + 0.7531999805212017, this.player.z))
            offsets.add(Vec3d(this.player.x, this.player.y + 0.9999957640154541, this.player.z))
            offsets.add(Vec3d(this.player.x, this.player.y + 1.1661092609382138, this.player.z))
        } else {
            val offVec3: Vec3d = direction(burBlock)
            offsets.add(
                Vec3d(
                    player.x + offVec3.x * 0.42132, player.y + 0.12160004615784,
                    player.z + offVec3.z * 0.42132
                )
            )
            offsets.add(
                Vec3d(
                    player.x + offVec3.x * 0.95, player.y + 0.200000047683716,
                    player.z + offVec3.z * 0.95
                )
            )
            offsets.add(
                Vec3d(
                    player.x + offVec3.x * 1.03, player.y + 0.200000047683716,
                    player.z + offVec3.z * 1.03
                )
            )
            offsets.add(
                Vec3d(
                    player.x + offVec3.x * 1.0933, player.y + 0.12160004615784,
                    player.z + offVec3.z * 1.0933
                )
            )
        }
        return offsets
    }

    private fun SafeClientEvent.doFakeJump(offsets: List<Vec3d>?) {
        if (offsets == null) {
            return
        }
        offsets.forEach(Consumer { vec: Vec3d? ->
            if (vec == null || vec == Vec3d(0.0, 0.0, 0.0)) {
                return@Consumer
            }
            sendPlayerPos(vec.x, vec.y, vec.z, true)
        })
    }

    private fun SafeClientEvent.sendPlayerRotation(yaw: Float, pitch: Float, onGround: Boolean) {
        connection.sendPacket(PlayerMoveC2SPacket.LookAndOnGround(yaw, pitch, onGround))
    }

    fun SafeClientEvent.direction(burBlockPos: BlockPos): Vec3d {
        val playerPos: BlockPos = player.blockPos
        val centerPos = burBlockPos.toCenterPos()
        val subtracted: Vec3d = player.getPos().subtract(centerPos)
        var off = Vec3d.ZERO
        if (abs(subtracted.x) >= abs(subtracted.z) && abs(subtracted.x) > 0.2) {
            off = if (subtracted.x > 0.0) {
                Vec3d(0.8 - subtracted.x, 0.0, 0.0)
            } else {
                Vec3d(-0.8 - subtracted.x, 0.0, 0.0)
            }
        } else if (abs(subtracted.z) >= abs(subtracted.x) && abs(subtracted.z) > 0.2) {
            off = if (subtracted.z > 0.0) {
                Vec3d(0.0, 0.0, 0.8 - subtracted.z)
            } else {
                Vec3d(0.0, 0.0, -0.8 - subtracted.z)
            }
        } else if (burBlockPos == playerPos) {
            val facList: MutableList<Direction> = ArrayList()
            for (dir in Direction.entries) {
                if (dir == Direction.UP || dir == Direction.DOWN) continue
                if (!solid(playerPos.offset(dir)) && !solid(
                        playerPos.offset(dir).offset(Direction.UP)
                    )
                ) {
                    facList.add(dir)
                }
            }
            val vec3d = Vec3d.ZERO
            val offVec1 = arrayOfNulls<Vec3d>(1)
            val offVec2 = arrayOfNulls<Vec3d>(1)
            facList.sortWith { dir1: Direction, dir2: Direction ->
                offVec1[0] = vec3d.add(
                    Vec3d(
                        dir1.offsetX.toDouble(),
                        dir1.offsetY.toDouble(),
                        dir1.offsetZ.toDouble()
                    ).multiply(0.5)
                )
                offVec2[0] = vec3d.add(
                    Vec3d(
                        dir2.offsetX.toDouble(),
                        dir2.offsetY.toDouble(),
                        dir2.offsetZ.toDouble()
                    ).multiply(0.5)
                )
                (sqrt(
                    player.squaredDistanceTo(
                        offVec1[0]!!.x,
                        player.y,
                        offVec1[0]!!.z
                    )
                ) - sqrt(
                    player.squaredDistanceTo(
                        offVec2[0]!!.x,
                        player.y,
                        offVec2[0]!!.z
                    )
                )).toInt()
            }
            if (facList.size > 0) {
                off = Vec3d(facList[0].offsetX.toDouble(), facList[0].offsetY.toDouble(), facList[0].offsetZ.toDouble())
            }
        }
        return off
    }

    private fun playerPos(targetEntity: PlayerEntity): BlockPos {
        return BlockPos(
            floor(targetEntity.x).toInt(),
            targetEntity.y.roundToInt(),
            floor(targetEntity.z).toInt()
        )
    }

    private fun SafeClientEvent.sendPackets(): Double {
        if (solid(playerPos(player).multiply(2))) {
            return 0.0
        }
        if (solid(playerPos(player).multiply(3))) {
            return 1.2
        }
        for (i in 4..5) {
            if (solid(playerPos(player).multiply(i))) {
                return 2.2 + i - 4.0
            }
        }
        return 10.0
    }

    private fun SafeClientEvent.solid(blockPos: BlockPos?): Boolean {
        val block: Block = world.getBlockState(blockPos).block
        return !(block is AbstractFireBlock || block is FluidBlock || block is AirBlock)
    }

    private fun SafeClientEvent.fakeBoXxCheck(player: PlayerEntity, offset: Vec3d): Boolean {
        val futurePos = player.getPos().add(offset)

        return world.isAir(futurePos.add(0.4, 0.0, 0.4).toBlockPos()) && world.isAir(
            futurePos.add(-0.4, 0.0, 0.4).toBlockPos()
        ) && world.isAir(
            futurePos.add(
                0.4,
                0.0,
                -0.4
            ).toBlockPos()
        ) && world.isAir(futurePos.add(-0.4, 0.0, 0.4).toBlockPos())
    }

    private fun SafeClientEvent.sendPlayerPos(x: Double, y: Double, z: Double, onGround: Boolean) {
        connection.sendPacket(PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, onGround))
    }

    private fun SafeClientEvent.canPlace(pos: BlockPos): Boolean {
        return world.isAir(pos) || world.getBlockState(pos).isWater || world.getBlockState(pos).isLiquidBlock || world.getBlockState(
            pos
        ).block is FireBlock
    }

    private fun SafeClientEvent.doSneak() {
        connection.sendPacket(
            ClientCommandC2SPacket(
                player,
                ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY
            )
        )
    }

    private fun getFlooredPosition(entity: Entity): BlockPos {
        return BlockPos(
            floor(entity.x).toInt(), entity.y.roundToInt().toDouble().toInt(), floor(entity.z).toInt()
        )
    }

    private fun getPlayerPosFloored(player: Entity): BlockPos {
        return BlockPos(
            floor(player.x).toInt(), floor(player.y).toInt(), floor(player.z).toInt()
        )
    }

    private fun getPosFloored(player: Entity): BlockPos {
        return BlockPos(
            floor(player.x).toInt(), floor(player.y + 0.2).toInt(), floor(player.z).toInt()
        )
    }

    private fun SafeClientEvent.cancelSneak() {
        connection.sendPacket(
            ClientCommandC2SPacket(
                player,
                ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY
            )
        )
    }


    private enum class PacketMode(override val displayName: CharSequence) : DisplayEnum {
        Normal("Normal"),
        Strict("Strict"),
        NCP("NCP"),
        AAC("AAC"),
        China("China"),
        Xin("Xin"),
        OFF("Off")
    }

    private enum class FakeJumpMode(override val displayName: CharSequence) : DisplayEnum {
        Normal("Normal"),
        Strict("Strict")
    }
}