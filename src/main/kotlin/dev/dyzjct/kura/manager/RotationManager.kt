package dev.dyzjct.kura.manager

import base.utils.math.vector.Vec2f
import dev.dyzjct.kura.event.eventbus.AlwaysListening
import dev.dyzjct.kura.event.eventbus.SafeClientEvent
import dev.dyzjct.kura.event.eventbus.safeEventListener
import dev.dyzjct.kura.event.events.PacketEvents
import dev.dyzjct.kura.event.events.input.KeyboardTickEvent
import dev.dyzjct.kura.event.events.player.JumpEvent
import dev.dyzjct.kura.event.events.player.PlayerUpdateEvent
import dev.dyzjct.kura.event.events.player.UpdateMovementEvent
import dev.dyzjct.kura.event.events.player.UpdateVelocityEvent
import dev.dyzjct.kura.mixin.accessor.EntityAccessor
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.module.modules.client.Rotations
import dev.dyzjct.kura.module.modules.player.FreeCam
import dev.dyzjct.kura.utils.animations.Easing
import dev.dyzjct.kura.utils.extension.sendSequencedPacket
import dev.dyzjct.kura.utils.math.MathUtil
import dev.dyzjct.kura.utils.rotation.Rotation
import dev.dyzjct.kura.utils.rotation.RotationUtils.getRotationFromVec
import net.minecraft.block.BlockState
import net.minecraft.block.FluidBlock
import net.minecraft.entity.Entity
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.*
import net.minecraft.util.shape.VoxelShapes
import net.minecraft.world.RaycastContext
import java.util.concurrent.PriorityBlockingQueue
import kotlin.math.*

object RotationManager : AlwaysListening {
    var yaw: Float = 0f
    var pitch: Float = 0f

    fun SafeClientEvent.updateRotations() {
        yaw = player.getYaw()
        pitch = player.getPitch()
    }

    fun SafeClientEvent.restoreRotations() {
        player.setYaw(yaw)
        player.headYaw = yaw
        player.setPitch(pitch)
    }

    fun SafeClientEvent.setPlayerRotations(yaw: Float, pitch: Float) {
        player.setYaw(yaw)
        player.headYaw = yaw
        player.setPitch(pitch)
    }

    fun SafeClientEvent.setPlayerYaw(yaw: Float) {
        player.setYaw(yaw)
        player.headYaw = yaw
    }

    fun SafeClientEvent.lookAtPos(pos: BlockPos) {
        val angle: FloatArray = MathUtil.calcAngle(
            player.eyePos,
            Vec3d(
                (pos.x.toFloat() + 0.5f).toDouble(),
                (pos.y.toFloat() + 0.5f).toDouble(),
                (pos.z.toFloat() + 0.5f).toDouble()
            )
        )
        this.setPlayerRotations(angle[0], angle[1])
    }

    fun SafeClientEvent.lookAtVec3d(vec3d: Vec3d) {
        val angle: FloatArray = MathUtil.calcAngle(player.eyePos, Vec3d(vec3d.x, vec3d.y, vec3d.z))
        setPlayerRotations(angle[0], angle[1])
    }

    fun SafeClientEvent.lookAtVec3d(x: Double, y: Double, z: Double) {
        val vec3d = Vec3d(x, y, z)
        this.lookAtVec3d(vec3d)
    }

    fun SafeClientEvent.lookAtEntity(entity: Entity) {
        val angle: FloatArray = MathUtil.calcAngle(player.eyePos, entity.eyePos)
        setPlayerRotations(angle[0], angle[1])
    }

    fun SafeClientEvent.setPlayerPitch(pitch: Float) {
        player.setPitch(pitch)
    }


    private val queue: PriorityBlockingQueue<Rotation> = PriorityBlockingQueue<Rotation>(
        11,
        Comparator<Rotation> { target: Rotation, rotation: Rotation -> this.compareRotations(target, rotation) })

    private var rotation: Rotation? = null

    private var prevFixYaw = 0f

    private var prevYaw = 0f
    private var prevPitch = 0f

    private var serverYaw = 0f

    private var serverPitch = 0f

    private var prevRenderYaw = 0f
    private var prevRenderPitch = 0f
    private var lastRenderTime = 0L

    fun onInit() {
        safeEventListener<PlayerUpdateEvent> {
            queue.removeIf { rotation: Rotation -> System.currentTimeMillis() - rotation.time > 100 }
            rotation = queue.peek()

            if (rotation == null) return@safeEventListener
            lastRenderTime = System.currentTimeMillis()
        }

        safeEventListener<UpdateMovementEvent.Pre> {
            rotation?.let { rotation ->
                prevYaw = player.getYaw()
                prevPitch = player.getPitch()

                player.setYaw(rotation.yaw)
                player.setPitch(rotation.pitch)
            }
        }

        safeEventListener<UpdateMovementEvent.Post> {
            rotation?.let {
                player.setYaw(prevYaw)
                player.setPitch(prevPitch)
            }
        }

        safeEventListener<UpdateVelocityEvent> { event ->
            if (!Rotations.movement_fix) return@safeEventListener
            if (FreeCam.isEnabled) FreeCam.disable()
            rotation?.let { rotation ->
                event.velocity = (
                        EntityAccessor.invokeMovementInputToVelocity(
                            event.movementInput,
                            event.speed,
                            rotation.yaw
                        )
                        )
                event.cancel()
            }
        }

        safeEventListener<KeyboardTickEvent> { event ->
            if (player.isRiding) return@safeEventListener
            if (!Rotations.movement_fix) return@safeEventListener
            if (FreeCam.isEnabled) return@safeEventListener
            rotation?.let { rotation ->
                val movementForward: Float = event.movementForward
                val movementSideways: Float = event.movementSideways

                val delta: Float = (player.getYaw() - rotation.yaw) * MathHelper.RADIANS_PER_DEGREE

                val cos = MathHelper.cos(delta)
                val sin = MathHelper.sin(delta)

                event.movementForward = Math.round(movementForward * cos + movementSideways * sin).toFloat()
                event.movementSideways = Math.round(movementSideways * cos - movementForward * sin).toFloat()
                event.cancel()
            }
        }
        safeEventListener<JumpEvent.Post> {
            if (player.isRiding) return@safeEventListener
            if (!Rotations.movement_fix) return@safeEventListener
            if (FreeCam.isEnabled) return@safeEventListener
            rotation?.let { rotation ->
                prevFixYaw = player.yaw
                player.setYaw(rotation.yaw)
            }
        }
        safeEventListener<PacketEvents.Send> { event ->
            if (event.packet is PlayerMoveC2SPacket) {
                if (!event.packet.changesLook()) return@safeEventListener

                serverYaw = event.packet.getYaw(player.yaw)
                serverPitch = event.packet.getPitch(player.pitch)
            }
        }
    }

    //
    // 平滑设置旋转
    fun SafeClientEvent.setSmoothRotation(targetYaw: Float, targetPitch: Float) {
        val currentYaw: Float = player.yaw
        val currentPitch: Float = player.pitch

        // 平滑因子
        val smoothFactor: Float = Rotations.smooth_factor

        // 计算平滑旋转
        val smoothYaw = currentYaw + (targetYaw - currentYaw) * smoothFactor
        val smoothPitch = currentPitch + (targetPitch - currentPitch) * smoothFactor

        player.setYaw(smoothYaw)
        player.setPitch(smoothPitch)
    }

    // 方法：检查视线是否可达
    fun SafeClientEvent.canPlaceBlockAt(pos: BlockPos): Boolean {
        val blockCenter = Vec3d(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5)

        val rayTraceResult: BlockHitResult = world.raycast(
            RaycastContext(
                player.eyePos,
                blockCenter,
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE,
                mc.player
            )
        )

        return rayTraceResult.type == HitResult.Type.MISS // 如果没有命中则可以放置
    }


    // 主逻辑调用
    //
    fun rotate(rotations: Vec2f, module: Module) {
        rotate(rotations.x, rotations.y, module)
    }

    fun rotate(yaw: Float, pitch: Float, module: Module) {
        queue.removeIf { rotation: Rotation -> rotation.module === module }
        queue.add(Rotation(yaw, pitch, module, getModulePriority(module)))
    }

    fun pistonRotate(yaw: Float, pitch: Float, priority: Int) {
        rotate(yaw, pitch, priority)
    }

    fun rotate(rotations: FloatArray, module: Module, priority: Int) {
        rotate(rotations[0], rotations[1], module, priority)
    }

    fun rotate(yaw: Float, pitch: Float, module: Module, priority: Int) {
        queue.removeIf { rotation: Rotation -> rotation.module === module }
        queue.add(Rotation(yaw, pitch, module, priority))
    }

    fun SafeClientEvent.packetRotate(rotations: Vec2f) {
        packetRotate(rotations.x, rotations.y)
    }

    fun SafeClientEvent.packetRotate(vec3d: Vec3d) {
        val rotations = getRotation(vec3d)
        packetRotate(rotations.x, rotations.y)
    }

    fun SafeClientEvent.packetRotate(blockPos: BlockPos) {
        val rotations = getRotation(blockPos.toCenterPos())
        packetRotate(rotations.x, rotations.y)
    }

//    @TODO ServerSide PLEASE!!
//    fun SafeClientEvent.SNAPPacketRotate(yaw: Float, pitch: Float) {
//        if (serverYaw == yaw && serverPitch == pitch) {
//            connection.sendPacket(
//                PlayerMoveC2SPacket.Full(
//                    player.x,
//                    player.y,
//                    player.z,
//                    yaw,
//                    pitch,
//                    player.onGround
//                )
//            )
//        }
//    }


    // 新增平滑过渡相关成员变量
    private var currentYaw = 0f
    private var currentPitch = 0f
    fun SafeClientEvent.getRotation(vec: Vec3d): Vec2f {
        // 保持原有实现不变
        val eyesPos = player.eyePos
        val diffX = vec.x - eyesPos.x
        val diffY = vec.y - eyesPos.y
        val diffZ = vec.z - eyesPos.z

        val diffXZSq = diffX * diffX + diffZ * diffZ
        if (diffXZSq < VERTICAL_THRESHOLD_SQ) {
            return Vec2f(0f, (if (diffY > 0) -90 else 90).toFloat())
        }

        if (abs(diffX) < 1e-9 && abs(diffY) < 1e-9 && abs(diffZ) < 1e-9) {
            return Vec2f(0f, 0f)
        }

        val diffXZ = sqrt(diffXZSq)
        val yaw = (Math.toDegrees(atan2(diffZ, diffX)) - YAW_OFFSET).toFloat()
        val pitch = (-Math.toDegrees(atan2(diffY, diffXZ))).toFloat()

        return Vec2f(
            MathHelper.wrapDegrees(yaw),
            MathHelper.wrapDegrees(pitch)
        )
    }

    fun SafeClientEvent.Spam(pos: BlockPos, side: Direction) {
        val sideVector = Vec3d.of(side.vector)
        val offset = Vec3d(
            sideVector.getX() * HIT_VEC_OFFSET,
            sideVector.getY() * HIT_VEC_OFFSET,
            sideVector.getZ() * HIT_VEC_OFFSET
        )

        Spam(pos.toCenterPos().add(offset))
    }

    fun SafeClientEvent.Spam(directionVec: Vec3d) {
        rotate(getRotation(directionVec), 1)
        SpamRotate(directionVec)
    }

    fun rotate(rotations: Vec2f, priority: Int) {
        rotate(rotations.x, rotations.y, priority)
    }

    fun rotate(yaw: Float, pitch: Float, priority: Int) {
        queue.removeIf { rotation: Rotation -> rotation.module == null && rotation.priority === priority }
        queue.add(Rotation(yaw, pitch, priority = priority))
    }

    fun SafeClientEvent.SpamRotate(directionVec: Vec3d) {
        val angle = getRotation(directionVec)
        val actualYaw: Float = prevFixYaw
        val actualPitch: Float = prevPitch

        // 同步当前角度
        currentYaw = actualYaw
        currentPitch = actualPitch

        if (!Rotations.grim_rotation) {
            if (Rotations.smooth_rotation) {
                smoothRotation(angle.x, angle.y)
            } else {
                packetRotate(angle.x, angle.y)
            }
            return
        }

        val fovThreshold: Int = Rotations.fov
        val yawDiff = MathHelper.angleBetween(angle.x, actualYaw)
        val pitchDiff = abs((angle.y - actualPitch).toDouble()).toFloat()

        if (yawDiff < fovThreshold && pitchDiff < fovThreshold) return

        if (Rotations.smooth_rotation) {
            smoothRotation(angle.x, angle.y)
        } else {
            packetRotate(angle.x, angle.y)
        }
    }

    // 新增平滑过渡方法（带随机扰动）
    // 改进后的平滑过渡方法
    fun SafeClientEvent.smoothRotation(targetYaw: Float, targetPitch: Float) {
        // 获取配置参数并约束范围
        val smoothStep = MathHelper.clamp(Rotations.smooth_factor, 0.01f, 1.0f)

        // 计算最短路径角度差（处理-180~180环绕）
        val deltaYaw = MathHelper.wrapDegrees(targetYaw - currentYaw)
        val deltaPitch = targetPitch - currentPitch

        // 应用精准线性插值
        currentYaw = MathHelper.wrapDegrees(currentYaw + deltaYaw * smoothStep)
        currentPitch = MathHelper.clamp(currentPitch + deltaPitch * smoothStep, -90f, 90f)

        // 发送精确角度数据包
        packetRotate(currentYaw, currentPitch)
    }


    fun SafeClientEvent.packetRotate(yaw: Float, pitch: Float) {
        val wrappedYaw = MathHelper.wrapDegrees(yaw)
        val clampedPitch = MathHelper.clamp(pitch, -90f, 90f)
        if (mc.player != null) {
            sendSequencedPacket(world) { id ->
                (PlayerMoveC2SPacket.Full(
                    player.x,
                    player.y,
                    player.z,
                    wrappedYaw,
                    clampedPitch,
                    player.isOnGround
                ))
            }
        }
    }

    fun inRenderTime(): Boolean {
        return System.currentTimeMillis() - lastRenderTime < 1000
    }

    val SafeClientEvent.renderRotations: Vec2f
        get() {
            val from: Float = MathUtil.wrapAngle(prevRenderYaw)
            val to: Float = MathUtil.wrapAngle(if (rotation == null) player.getYaw() else serverYaw)
            var delta = to - from
            if (delta > 180) delta -= 380f
            else if (delta < -180) delta += 360f

            val yaw = MathHelper.lerp(Easing.toDelta(lastRenderTime, 1000), from, from + delta)
            val pitch: Float = MathHelper.lerp(
                Easing.toDelta(lastRenderTime, 1000),
                prevRenderPitch,
                if (rotation == null) player.getPitch() else serverPitch
            )
            prevRenderYaw = yaw
            prevRenderPitch = pitch

            return Vec2f(yaw, pitch)
        }

    fun getModulePriority(module: Module): Int {
        return PRIORITIES.getOrDefault(module.moduleName, 0)
    }

    private fun compareRotations(target: Rotation, rotation: Rotation): Int {
        if (target.priority === rotation.priority) return -java.lang.Long.compare(
            target.time,
            rotation.time
        )
        return -Integer.compare(target.priority, rotation.priority)
    }


    //shi
    private val ticksExisted = 0

    fun SafeClientEvent.lookAt(pos: BlockPos, side: Direction) {
        val hitVec = pos.toCenterPos().add(Vec3d(side.vector.x * 0.5, side.vector.y * 0.5, side.vector.z * 0.5))
        lookAt(hitVec)
    }

    fun SafeClientEvent.lookAt(directionVec: Vec3d) {
        rotate(getRotation(directionVec), 1)
        snapAt(directionVec)
    }

    fun SafeClientEvent.snapAt(directionVec: Vec3d) {
        val angle = getRotation(directionVec)
        if (Rotations.grim_rotation) {
            if (MathHelper.angleBetween(
                    angle.x,
                    prevFixYaw
                ) < Rotations.fov && Math.abs(
                    angle.y - prevPitch
                ) < Rotations.fov
            ) {
                return
            }
        }
        packetRotate(angle.x, angle.y)
    }


    private val PRIORITIES = HashMap<String, Int>()

    // 常量池定义（提升可维护性）
    private const val VERTICAL_THRESHOLD_SQ = 1.0E-8 // (1e-4)^2
    private const val YAW_OFFSET = 90.0f
    private const val HIT_VEC_OFFSET = 0.5

    private const val DEFAULT_SMOOTH_STEP = 0.5f
    private const val DEFAULT_JITTER_STRENGTH = 0.02f

    fun calculateAngles(from: Vec3d, to: Vec3d): Vec2f {
        val diffX = to.x - from.x
        val diffY = (to.y - from.y) * -1.0
        val diffZ = to.z - from.z

        val dist = sqrt(diffX * diffX + diffZ * diffZ)

        val yaw = Math.toDegrees(atan2(diffZ, diffX)).toFloat() - 90f
        val pitch = -Math.toDegrees(atan2(diffY, dist)).toFloat()

        return Vec2f(
            MathHelper.wrapDegrees(yaw),
            MathHelper.clamp(MathHelper.wrapDegrees(pitch), -90f, 90f)
        )
    }

    fun getEyesPos(entity: Entity): Vec3d {
        return entity.getPos().add(0.0, entity.getEyeHeight(entity.pose).toDouble(), 0.0)
    }

    fun SafeClientEvent.bc() {
        getEyesPos(player)
    }

    fun SafeClientEvent.calculateAngle(to: Vec3d): Vec2f {
        return calculateAngle(getEyesPos(player), to)
    }

    fun calculateAngle(from: Vec3d, to: Vec3d): Vec2f {
        val difX = to.x - from.x
        val difY = (to.y - from.y) * -1.0
        val difZ = to.z - from.z
        val dist = MathHelper.sqrt((difX * difX + difZ * difZ).toFloat()).toDouble()

        val yD = MathHelper.wrapDegrees(Math.toDegrees(atan2(difZ, difX)) - 90.0).toFloat()
        val pD = MathHelper.clamp(MathHelper.wrapDegrees(Math.toDegrees(atan2(difY, dist))), -90.0, 90.0).toFloat()

        return Vec2f(yD, pD)
    }

    //穿墙射线
    fun SafeClientEvent.canReach(targetPos: BlockPos, hitVec: Vec3d): Boolean {
        if (player == null || mc.world == null) return false
        val eyesPos = Vec3d(
            player.x,
            player.y + player.getEyeHeight(player.pose),
            player.z
        )
        val context: RaycastContext = RaycastContext(
            eyesPos,
            hitVec,
            RaycastContext.ShapeType.COLLIDER,
            RaycastContext.FluidHandling.NONE,
            mc.player
        )
        val result: BlockHitResult = world.raycast(context)
        return result.type == HitResult.Type.MISS ||
                result.blockPos == targetPos
    }

    fun getLookVector(yaw: Float, pitch: Float): Vec3d {
        val yawRadians = Math.toRadians(-yaw.toDouble()) // 取负数修正顺时针旋转
        val pitchRadians = Math.toRadians(pitch.toDouble())


        val cosPitch = cos(pitchRadians)
        val sinPitch = sin(pitchRadians)


        val cosYaw = cos(yawRadians)
        val sinYaw = sin(yawRadians)

        return Vec3d(
            sinYaw * cosPitch,
            -sinPitch,
            cosYaw * cosPitch
        ).normalize()
    }

    fun SafeClientEvent.calculateStrictRotations(pos: BlockPos, side: Direction): Vec2f {
        // 获取玩家延迟
        val playerListEntry = connection.getPlayerListEntry(player.uuid)
        val ping = playerListEntry!!.latency
        val latencySeconds = ping / 1000.0

        // 计算预测位置（包含延迟补偿）
        val velocity: Vec3d = player.velocity
        val predictedPos: Vec3d = player.getPos().add(
            velocity.x * latencySeconds,
            velocity.y * latencySeconds,
            velocity.z * latencySeconds
        )

        // 计算精确命中点
        val hitVec = calculatePreciseHitVector(pos, side)
        return getRotationFromVec(
            Vec3d(
                hitVec.x - predictedPos.x,
                hitVec.y - predictedPos.y,
                hitVec.z - predictedPos.z
            )
        )
    }

    fun SafeClientEvent.validatePhysicalState(pos: BlockPos, side: Direction): Boolean {
        // 计算精确的命中点
        val hitVec = calculatePreciseHitVector(pos, side)

        // 获取玩家的视线方向
        val lookVec = getLookVector(
            player.getYaw(), player.getPitch()
        )

        // 验证视线方向是否正确
        val toTarget = hitVec.subtract(player.getPos()).normalize()
        if (lookVec.dotProduct(toTarget) < 0.95) {
            return false
        }

        // 修复后的距离验证（两种方案任选其一）
        // 方案1：使用坐标分量
        //if (mc.player.distanceTo(hitVec.x, hitVec.y, hitVec.z) > 5.0) {
        //    return false;
        //}

        //方案2：使用 Vec3d 的 distanceTo
        if (hitVec.distanceTo(player.getPos()) > 5.0) {
            return false
        }

        return true
    }

    fun SafeClientEvent.calculatePreciseHitVector(pos: BlockPos, side: Direction): Vec3d {
        // 参数校验
        if (world == null);
        if (!world.isChunkLoaded(pos));

        val state: BlockState = world.getBlockState(pos)
        var shape = state.getCollisionShape(world, pos)
        if (state.block is FluidBlock) {
            shape = VoxelShapes.fullCube()
        }
        // 获取有效碰撞箱集合（处理多部分）
        val boxes: MutableList<Box> = ArrayList()
        if (shape.isEmpty) {
            boxes.add(Box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0))
        } else {
            shape.forEachBox { x1: Double, y1: Double, z1: Double, x2: Double, y2: Double, z2: Double ->
                boxes.add(
                    Box(x1, y1, z1, x2, y2, z2)
                )
            }
        }

        var bestHit: Vec3d? = null
        var minDistance = Double.MAX_VALUE
        val eyesPos = preciseEyesPos

        for (localBox in boxes) {
            val hitVec = calculateLocalHitVector(localBox, side)
                .add(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble()) // 转换为世界坐标

            val context: RaycastContext = RaycastContext(
                eyesPos,
                hitVec,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                mc.player
            )

            val result: BlockHitResult = world.raycast(context)
            if (result != null && result.blockPos == pos) {
                val dist = eyesPos.squaredDistanceTo(hitVec)
                if (dist < minDistance) {
                    bestHit = hitVec
                    minDistance = dist
                }
            }
        }

        return bestHit ?: Vec3d.ofCenter(pos)
    }

    private val SafeClientEvent.preciseEyesPos: Vec3d
        get() {
            var yOffset: Double = player.getEyeHeight(player.pose).toDouble()
            if (player.isSneaking) {
                yOffset -= 0.08 // 潜行视线修正
            }
            return Vec3d(
                player.x,
                player.y + yOffset,
                player.z
            )
        }

    private fun calculateLocalHitVector(localBox: Box, side: Direction): Vec3d {
        val epsilon = 1e-5
        val dx = max(localBox.maxX - localBox.minX, epsilon)
        val dy = max(localBox.maxY - localBox.minY, epsilon)
        val dz = max(localBox.maxZ - localBox.minZ, epsilon)

        return Vec3d(
            localBox.minX + dx * (0.5 + 0.5 * side.offsetX),
            localBox.minY + dy * (0.5 + 0.5 * side.offsetY),
            localBox.minZ + dz * (0.5 + 0.5 * side.offsetZ)
        )
    }
}