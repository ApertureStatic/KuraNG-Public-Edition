package dev.dyzjct.kura.manager

import dev.dyzjct.kura.event.eventbus.AlwaysListening
import dev.dyzjct.kura.event.eventbus.safeEventListener
import dev.dyzjct.kura.event.events.player.PlayerUpdateEvent
import dev.dyzjct.kura.event.events.player.UpdateMovementEvent
import dev.dyzjct.kura.utils.rotation.Rotation
import net.minecraft.entity.Entity
import net.minecraft.util.math.Direction
import java.util.*
import java.util.concurrent.PriorityBlockingQueue
import kotlin.math.*

class RotationManager : AlwaysListening {
    private val queue: PriorityBlockingQueue<Rotation> = PriorityBlockingQueue<Rotation>(
        11
    ) { target: Rotation, rotation: Rotation -> this.compareRotations(target, rotation) }

    private var rotation: Rotation? = null

    private var prevFixYaw = 0f

    private var prevYaw = 0f
    private var prevPitch = 0f

    private var serverYaw = 0f

    private var serverPitch = 0f

    private var prevRenderYaw = 0f
    private var prevRenderPitch = 0f
    private var lastRenderTime = 0L


    init {
        safeEventListener<PlayerUpdateEvent> {
            queue.removeIf { rotation: Rotation -> System.currentTimeMillis() - rotation.time > 100 }
            rotation = queue.peek()

            if (rotation == null) return@safeEventListener
            lastRenderTime = System.currentTimeMillis()
        }

        safeEventListener<UpdateMovementEvent.Pre> {
            if (rotation == null) return@safeEventListener

            prevYaw = player.yaw
            prevPitch = player.pitch

            player.setYaw(rotation!!.yaw)
            player.setPitch(rotation!!.pitch)
        }
        safeEventListener<UpdateMovementEvent.Post> {
            if (rotation == null) return@safeEventListener

            player.setYaw(prevYaw)
            player.setPitch(prevPitch)
        }
    }

    @SubscribeEvent
    fun onUpdateVelocity(event: UpdateVelocityEvent) {
        if (mc.player == null) return
        if (!Opan.MODULE_MANAGER.getModule(RotationsModule::class.java).movementFix.getValue()) return
        if (rotation == null) return

        event.setVelocity(
            EntityAccessor.invokeMovementInputToVelocity(
                event.getMovementInput(),
                event.getSpeed(),
                rotation.getYaw()
            )
        )
        event.setCancelled(true)
    }


    @SubscribeEvent
    fun onKeyboardTick(event: KeyboardTickEvent) {
        if (mc.player == null || mc.world == null || mc.player.isRiding()) return
        if (!Opan.MODULE_MANAGER.getModule(RotationsModule::class.java).movementFix.getValue()) return
        if (rotation == null) return

        val movementForward: Float = event.getMovementForward()
        val movementSideways: Float = event.getMovementSideways()

        val delta: Float = (mc.player.getYaw() - rotation.getYaw()) * MathHelper.RADIANS_PER_DEGREE

        val cos: Float = MathHelper.cos(delta)
        val sin: Float = MathHelper.sin(delta)

        event.setMovementForward(Math.round(movementForward * cos + movementSideways * sin))
        event.setMovementSideways(Math.round(movementSideways * cos - movementForward * sin))
        event.setCancelled(true)
    }

    @SubscribeEvent
    fun onPlayerJump(event: PlayerJumpEvent?) {
        if (mc.player == null || mc.world == null || mc.player.isRiding()) return
        if (!Opan.MODULE_MANAGER.getModule(RotationsModule::class.java).movementFix.getValue()) return
        if (rotation == null) return

        prevFixYaw = mc.player.getYaw()
        mc.player.setYaw(rotation.getYaw())
    }

    @SubscribeEvent
    fun `onPlayerJump$POST`(event: PlayerJumpEvent.Post?) {
        if (mc.player == null || mc.world == null || mc.player.isRiding()) return
        if (!Opan.MODULE_MANAGER.getModule(RotationsModule::class.java).movementFix.getValue()) return
        if (rotation == null) return

        mc.player.setYaw(prevFixYaw)
    }

    @SubscribeEvent
    fun onPacketSend(event: PacketSendEvent) {
        if (mc.player == null) return

        if (event.getPacket() is PlayerMoveC2SPacket) {
            if (!packet.changesLook()) return

            serverYaw = packet.getYaw(mc.player.getYaw())
            serverPitch = packet.getPitch(mc.player.getPitch())
        }
    }


    //
    // 平滑设置旋转
    fun setSmoothRotation(targetYaw: Float, targetPitch: Float) {
        val currentYaw: Float = mc.player.getYaw()
        val currentPitch: Float = mc.player.getPitch()

        // 平滑因子
        val smoothFactor: Float =
            Opan.MODULE_MANAGER.getModule(RotationsModule::class.java).smoothFactor.getValue().intValue()

        // 计算平滑旋转
        val smoothYaw = currentYaw + (targetYaw - currentYaw) * smoothFactor
        val smoothPitch = currentPitch + (targetPitch - currentPitch) * smoothFactor

        mc.player.setYaw(smoothYaw)
        mc.player.setPitch(smoothPitch)
    }

    // 方法：检查视线是否可达
    fun canPlaceBlockAt(pos: BlockPos): Boolean {
        val eyesPos: Vec3d = eyesPos
        val blockCenter: Vec3d = Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)

        val rayTraceResult: BlockHitResult = mc.world.raycast(
            RaycastContext(
                eyesPos,
                blockCenter,
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE,
                mc.player
            )
        )

        return rayTraceResult.getType() == BlockHitResult.Type.MISS // 如果没有命中则可以放置
    }


    // 主逻辑调用
    //
    fun rotate(rotations: FloatArray, module: Module) {
        rotate(rotations[0], rotations[1], module)
    }

    fun rotate(yaw: Float, pitch: Float, module: Module) {
        queue.removeIf { rotation: Rotation -> rotation.getModule() === module }
        queue.add(Rotation(yaw, pitch, module, getModulePriority(module)))
    }

    fun PistonRotate(yaw: Float, pitch: Float, priority: Int) {
        rotate(yaw, pitch, priority)
    }

    fun rotate(rotations: FloatArray, module: Module, priority: Int) {
        rotate(rotations[0], rotations[1], module, priority)
    }

    fun rotate(yaw: Float, pitch: Float, module: Module, priority: Int) {
        queue.removeIf { rotation: Rotation -> rotation.getModule() === module }
        queue.add(Rotation(yaw, pitch, module, priority))
    }


    fun getRotationss(eyesPos: Vec3d, vec: Vec3d): FloatArray {
        val diffX: Double = vec.x - eyesPos.x
        val diffY: Double = vec.y - eyesPos.y
        val diffZ: Double = vec.z - eyesPos.z
        var diffXZ = sqrt(diffX * diffX + diffZ * diffZ)

        if (diffXZ < 0.001) diffXZ = 0.001

        val yaw = Math.toDegrees(atan2(diffZ, diffX)).toFloat() - 90.0f
        val pitch = (-Math.toDegrees(atan2(diffY, diffXZ))).toFloat()

        return floatArrayOf(MathHelper.wrapDegrees(yaw), MathHelper.wrapDegrees(pitch))
    }

    fun getRotations1(vec: Vec3d): FloatArray {
        val eyesPos: Vec3d = eyesPos
        return getRotationss(eyesPos, vec)
    }

    fun packetRotate(rotations: FloatArray) {
        packetRotate(rotations[0], rotations[1])
    }

    fun SNAPPacketRotate(yaw: Float, pitch: Float) {
        when (Opan.MODULE_MANAGER.getModule(RotationsModule::class.java).snapBackMode.getValue()) {
            "ServerFull" -> {
                if (serverYaw == yaw && serverPitch == pitch) {
                    Objects.requireNonNull<T>(mc.getNetworkHandler()).sendPacket(
                        PlayerMoveC2SPacket.Full(
                            Opan.POSITION_MANAGER.getServerX(),
                            Opan.POSITION_MANAGER.getServerY(),
                            Opan.POSITION_MANAGER.getServerZ(),
                            yaw,
                            pitch,
                            Opan.POSITION_MANAGER.isServerOnGround(),
                            mc.player.horizontalCollision
                        )
                    )
                }
            }

            "ServerLook" -> {
                if (serverYaw == yaw && serverPitch == pitch) {
                    mc.getNetworkHandler().sendPacket(
                        LookAndOnGround(
                            yaw,
                            pitch,
                            Opan.POSITION_MANAGER.isServerOnGround(),
                            mc.player.horizontalCollision
                        )
                    )
                }
            }

            "ClientFull" -> {
                if (mc.player != null) {
                    mc.player.networkHandler.sendPacket(
                        PlayerMoveC2SPacket.Full(
                            mc.player.getX(),
                            mc.player.getY(),
                            mc.player.getZ(),
                            yaw,
                            pitch,
                            mc.player.isOnGround(),
                            mc.player.horizontalCollision
                        )
                    )
                }
            }

            "ClientLook" -> {
                if (mc.player != null) {
                    Objects.requireNonNull<T>(
                        mc.getNetworkHandler()
                    )
                        .sendPacket(
                            LookAndOnGround(
                                yaw,
                                pitch,
                                mc.player.isOnGround(),
                                mc.player.horizontalCollision
                            )
                        )
                }
            }

            "Yaw-pitch" -> {
                if (mc.player != null) {
                    if (serverYaw == yaw && serverPitch == pitch) {
                        Objects.requireNonNull<T>(mc.getNetworkHandler()).sendPacket(
                            PlayerMoveC2SPacket.Full(
                                mc.player.getX(),
                                mc.player.getY(),
                                mc.player.getZ(),
                                yaw,
                                pitch,
                                Opan.POSITION_MANAGER.isServerOnGround(),
                                mc.player.horizontalCollision
                            )
                        )
                    }
                }
            }
        }
    }


    // 新增平滑过渡相关成员变量
    private var currentYaw = 0f
    private var currentPitch = 0f
    fun getRotation(eyesPos: Vec3d?, vec: Vec3d?): FloatArray {
        // 保持原有实现不变
        if (eyesPos == null || vec == null) return floatArrayOf(0f, 0f)

        val diffX: Double = vec.x - eyesPos.x
        val diffY: Double = vec.y - eyesPos.y
        val diffZ: Double = vec.z - eyesPos.z

        val diffXZSq = diffX * diffX + diffZ * diffZ
        if (diffXZSq < VERTICAL_THRESHOLD_SQ) {
            return floatArrayOf(0f, (if (diffY > 0) -90 else 90).toFloat())
        }

        if (abs(diffX) < 1e-9 && abs(diffY) < 1e-9 && abs(diffZ) < 1e-9) {
            return floatArrayOf(0f, 0f)
        }

        val diffXZ = sqrt(diffXZSq)
        val yaw = (Math.toDegrees(atan2(diffZ, diffX)) - YAW_OFFSET).toFloat()
        val pitch = (-Math.toDegrees(atan2(diffY, diffXZ))).toFloat()

        return floatArrayOf(
            MathHelper.wrapDegrees(yaw),
            MathHelper.wrapDegrees(pitch)
        )
    }

    fun getRotation(vec: Vec3d?): FloatArray {
        return getRotation(getEyesPos(mc.player), vec)
    }

    fun Spam(pos: BlockPos, side: Direction) {
        val sideVector: Vec3d = Vec3d.of(side.vector)
        val offset: Vec3d = Vec3d(
            sideVector.getX() * HIT_VEC_OFFSET,
            sideVector.getY() * HIT_VEC_OFFSET,
            sideVector.getZ() * HIT_VEC_OFFSET
        )

        Spam(pos.toCenterPos().add(offset))
    }

    fun Spam(directionVec: Vec3d?) {
        rotate(getRotations(directionVec), 1)
        SpamRotate(directionVec)
    }

    fun rotate(rotations: FloatArray, priority: Int) {
        rotate(rotations[0], rotations[1], priority)
    }

    fun rotate(yaw: Float, pitch: Float, priority: Int) {
        queue.removeIf { rotation: Rotation -> rotation.getModule() == null && rotation.getPriority() === priority }
        queue.add(Rotation(yaw, pitch, priority))
    }

    fun SpamRotate(directionVec: Vec3d?) {
        val module: RotationsModule = Opan.MODULE_MANAGER.getModule(RotationsModule::class.java) ?: return

        val angle = getRotation(directionVec)
        val actualYaw: Float = Opan.ROTATION_MANAGER.prevFixYaw
        val actualPitch: Float = Opan.ROTATION_MANAGER.prevPitch

        // 同步当前角度
        currentYaw = actualYaw
        currentPitch = actualPitch

        if (!module.Spam_grimRotation.getValue()) {
            if (module.useSmoothRotation.getValue()) {
                smoothRotation(angle[0], angle[1], module)
            } else {
                packetRotate(angle[0], angle[1])
            }
            return
        }

        val fovThreshold: Int = module.fov.getValue().intValue()
        val yawDiff: Float = MathHelper.angleBetween(angle[0], actualYaw)
        val pitchDiff = abs((angle[1] - actualPitch).toDouble()).toFloat()

        if (yawDiff < fovThreshold && pitchDiff < fovThreshold) return

        if (module.useSmoothRotation.getValue()) {
            smoothRotation(angle[0], angle[1], module)
        } else {
            packetRotate(angle[0], angle[1])
        }
    }

    // 新增平滑过渡方法（带随机扰动）
    // 改进后的平滑过渡方法
    fun smoothRotation(targetYaw: Float, targetPitch: Float, module: RotationsModule) {
        // 获取配置参数并约束范围
        val smoothStep: Float = MathHelper.clamp(module.smoothFactor.getValue().intValue(), 0.01f, 1.0f)

        // 计算最短路径角度差（处理-180~180环绕）
        val deltaYaw: Float = MathHelper.wrapDegrees(targetYaw - currentYaw)
        val deltaPitch = targetPitch - currentPitch

        // 应用精准线性插值
        currentYaw = MathHelper.wrapDegrees(currentYaw + deltaYaw * smoothStep)
        currentPitch = MathHelper.clamp(currentPitch + deltaPitch * smoothStep, -90f, 90f)

        // 发送精确角度数据包
        packetRotate(currentYaw, currentPitch)
    }


    fun packetRotate(yaw: Float, pitch: Float) {
        when (Opan.MODULE_MANAGER.getModule(RotationsModule::class.java).GrimRotations.getValue()) {
            "WcAx3ps" -> {
                if (Opan.MODULE_MANAGER.getModule(RotationsModule::class.java).Strict_grimRotation.getValue()) {
                    if (mc.player != null) {
                        Objects.requireNonNull<T>(mc.getNetworkHandler()).sendPacket(
                            PlayerMoveC2SPacket.Full(
                                mc.player.getX(),
                                mc.player.getY(),
                                mc.player.getZ(),
                                yaw,
                                pitch,
                                mc.player.isOnGround(),
                                mc.player.horizontalCollision
                            )
                        )
                    }
                } else {
                    if (mc.player != null) {
                        Objects.requireNonNull<T>(
                            mc.getNetworkHandler()
                        ).sendPacket(
                            LookAndOnGround(
                                yaw,
                                pitch,
                                mc.player.isOnGround(),
                                mc.player.horizontalCollision
                            )
                        )
                    }
                }
            }

            "Catty" -> {
                if (mc.player != null) {
                    Objects.requireNonNull<T>(mc.getNetworkHandler()).sendPacket(
                        PlayerMoveC2SPacket.Full(
                            Opan.POSITION_MANAGER.getServerX(),
                            Opan.POSITION_MANAGER.getServerY(),
                            Opan.POSITION_MANAGER.getServerZ(),
                            yaw,
                            pitch,
                            Opan.POSITION_MANAGER.isServerOnGround(),
                            mc.player.horizontalCollision
                        )
                    )
                }
            }

            "3ar" -> {
                if (mc.player != null) {
                    mc.player.networkHandler.sendPacket(
                        PlayerMoveC2SPacket.Full(
                            mc.player.getX(),
                            mc.player.getY(),
                            mc.player.getZ(),
                            yaw,
                            pitch,
                            mc.player.isOnGround(),
                            mc.player.horizontalCollision
                        )
                    )
                }
            }

            "AI24" -> {
                if (mc.player != null && mc.player.isOnGround()) {
                    mc.player.networkHandler.sendPacket(
                        PlayerMoveC2SPacket.Full(
                            mc.player.getX(),
                            mc.player.getY(),
                            mc.player.getZ(),
                            yaw,
                            pitch,
                            mc.player.isOnGround(),
                            mc.player.horizontalCollision
                        )
                    )
                } else if (Opan.MODULE_MANAGER.getModule(RotationsModule::class.java).Rotations.getValue()) {
                    mc.player.networkHandler.sendPacket(
                        PlayerMoveC2SPacket.Full(
                            mc.player.getX(),
                            mc.player.getY(),
                            mc.player.getZ(),
                            yaw,
                            pitch,
                            mc.player.isOnGround(),
                            mc.player.horizontalCollision
                        )
                    )
                }
            }

            "asphyxia" -> {
                val wrappedYaw: Float = MathHelper.wrapDegrees(yaw)
                val clampedPitch: Float = MathHelper.clamp(pitch, -90f, 90f)
                if (Opan.MODULE_MANAGER.getModule(RotationsModule::class.java).Look.getValue() && mc.player != null) {
                    NetworkUtils.sendSequencedPacket { id ->
                        LookAndOnGround(
                            wrappedYaw,
                            clampedPitch,
                            mc.player.isOnGround(),
                            mc.player.horizontalCollision
                        )
                    }
                } else if (mc.player != null) {
                    NetworkUtils.sendSequencedPacket { id ->
                        (PlayerMoveC2SPacket.Full(
                            mc.player.getX(),
                            mc.player.getY(),
                            mc.player.getZ(),
                            wrappedYaw,
                            clampedPitch,
                            mc.player.isOnGround(),
                            mc.player.horizontalCollision
                        ))
                    }
                }
            }
        }
    }

    fun inRenderTime(): Boolean {
        return System.currentTimeMillis() - lastRenderTime < 1000
    }

    val renderRotations: FloatArray
        get() {
            val from: Float = MathUtils.wrapAngle(prevRenderYaw)
            val to: Float = MathUtils.wrapAngle(if (rotation == null) mc.player.getYaw() else getServerYaw())
            var delta = to - from
            if (delta > 180) delta -= 380f
            else if (delta < -180) delta += 360f

            val yaw: Float = MathHelper.lerp(Easing.toDelta(lastRenderTime, 1000), from, from + delta)
            val pitch: Float = MathHelper.lerp(
                Easing.toDelta(lastRenderTime, 1000),
                prevRenderPitch,
                if (rotation == null) mc.player.getPitch() else getServerPitch()
            )
            prevRenderYaw = yaw
            prevRenderPitch = pitch

            return floatArrayOf(yaw, pitch)
        }

    fun getModulePriority(module: Module): Int {
        return PRIORITIES.getOrDefault(module.name, 0)
    }

    private fun compareRotations(target: Rotation, rotation: Rotation): Int {
        if (target.getPriority() === rotation.getPriority()) return -java.lang.Long.compare(
            target.getTime(),
            rotation.getTime()
        )
        return -Integer.compare(target.getPriority(), rotation.getPriority())
    }


    //shi
    private val ticksExisted = 0


    //    private static final HashMap<String, Integer> PRIORITIES = new HashMap<>();
    //    static {
    //        PRIORITIES.put("Sprint", 1);
    //        PRIORITIES.put("KillAura", 2);
    //        PRIORITIES.put("SpeedMine", 3);
    //        PRIORITIES.put("PistonCrystal", 4);
    //        PRIORITIES.put("PistonKick", 5);
    //        PRIORITIES.put("SelfFill", 6);
    //        PRIORITIES.put("AutoCrystal", 7);
    //        PRIORITIES.put("Phase", 8);
    //    }
    init {
        Opan.EVENT_HANDLER.subscribe(this)
    }

    fun lookAt(pos: BlockPos, side: Direction) {
        val hitVec: Vec3d = pos.toCenterPos().add(Vec3d(side.vector.x * 0.5, side.vector.y * 0.5, side.vector.z * 0.5))
        lookAt(hitVec)
    }

    fun lookAt(directionVec: Vec3d?) {
        rotate(getRotations(directionVec), 1)
        snapAt(directionVec)
    }

    fun snapAt(directionVec: Vec3d?) {
        val angle = getRotation(directionVec)
        if (Opan.MODULE_MANAGER.getModule(RotationsModule::class.java).Spam_grimRotation.getValue()) {
            if (MathHelper.angleBetween(angle[0], Opan.ROTATION_MANAGER.prevFixYaw) < Opan.MODULE_MANAGER.getModule(
                    RotationsModule::class.java
                ).fov.getValue()
                    .intValue() && Math.abs(angle[1] - Opan.ROTATION_MANAGER.prevPitch) < Opan.MODULE_MANAGER.getModule(
                    RotationsModule::class.java
                ).fov.getValue().intValue()
            ) {
                return
            }
        }
        packetRotate(angle[0], angle[1])
    }


    companion object {
        val eyesPos: Vec3d
            get() = mc.player.getEyePos()

        // 常量池定义（提升可维护性）
        private const val VERTICAL_THRESHOLD_SQ = 1.0E-8 // (1e-4)^2
        private const val YAW_OFFSET = 90.0f
        private const val HIT_VEC_OFFSET = 0.5

        private const val DEFAULT_SMOOTH_STEP = 0.5f
        private const val DEFAULT_JITTER_STRENGTH = 0.02f

        fun calculateAngles(from: Vec3d, to: Vec3d): FloatArray {
            val diffX: Double = to.x - from.x
            val diffY: Double = (to.y - from.y) * -1.0
            val diffZ: Double = to.z - from.z

            val dist = sqrt(diffX * diffX + diffZ * diffZ)

            val yaw = Math.toDegrees(atan2(diffZ, diffX)).toFloat() - 90f
            val pitch = -Math.toDegrees(atan2(diffY, dist)).toFloat()

            return floatArrayOf(
                MathHelper.wrapDegrees(yaw),
                MathHelper.clamp(MathHelper.wrapDegrees(pitch), -90f, 90f)
            )
        }

        fun getEyesPos(entity: Entity): Vec3d {
            return entity.getPos().add(0.0, entity.getEyeHeight(entity.pose).toDouble(), 0.0)
        }

        fun bc() {
            getEyesPos(mc.player)
        }

        fun calculateAngle(to: Vec3d): FloatArray {
            return calculateAngle(getEyesPos(mc.player), to)
        }

        fun calculateAngle(from: Vec3d, to: Vec3d): FloatArray {
            val difX: Double = to.x - from.x
            val difY: Double = (to.y - from.y) * -1.0
            val difZ: Double = to.z - from.z
            val dist: Double = MathHelper.sqrt((difX * difX + difZ * difZ).toFloat()).toDouble()

            val yD: Float = MathHelper.wrapDegrees(Math.toDegrees(atan2(difZ, difX)) - 90.0).toFloat()
            val pD: Float =
                MathHelper.clamp(MathHelper.wrapDegrees(Math.toDegrees(atan2(difY, dist))), -90.0, 90.0).toFloat()

            return floatArrayOf(yD, pD)
        }

        //穿墙射线
        fun canReach(targetPos: BlockPos, hitVec: Vec3d?): Boolean {
            if (mc.player == null || mc.world == null) return false
            val eyesPos: Vec3d = Vec3d(
                mc.player.getX(),
                mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()),
                mc.player.getZ()
            )
            val context: RaycastContext = RaycastContext(
                eyesPos,
                hitVec,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                mc.player
            )
            val result: BlockHitResult = mc.world.raycast(context)
            return result.getType() == HitResult.Type.MISS ||
                    result.getBlockPos() == targetPos
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

        fun calculateStrictRotations(pos: BlockPos?, side: Direction?): FloatArray {
            // 获取玩家延迟
            val playerListEntry: PlayerListEntry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid())
            val ping = if (playerListEntry != null) playerListEntry.getLatency() else 0
            val latencySeconds = ping / 1000.0

            // 计算预测位置（包含延迟补偿）
            val velocity: Vec3d = mc.player.getVelocity()
            val predictedPos: Vec3d = mc.player.getPos().add(
                velocity.x * latencySeconds,
                velocity.y * latencySeconds,
                velocity.z * latencySeconds
            )

            // 计算精确命中点
            val hitVec: Vec3d = calculatePreciseHitVector(pos, side)
            return getRotations(
                hitVec.x - predictedPos.x,
                hitVec.y - predictedPos.y,
                hitVec.z - predictedPos.z
            )
        }

        fun validatePhysicalState(pos: BlockPos?, side: Direction?): Boolean {
            // 计算精确的命中点
            val hitVec: Vec3d = calculatePreciseHitVector(pos, side)

            // 获取玩家的视线方向
            val lookVec: Vec3d = getLookVector(
                mc.player.getYaw(), mc.player.getPitch()
            )

            // 验证视线方向是否正确
            val toTarget: Vec3d = hitVec.subtract(mc.player.getPos()).normalize()
            if (lookVec.dotProduct(toTarget) < 0.95) {
                return false
            }

            // 修复后的距离验证（两种方案任选其一）
            // 方案1：使用坐标分量
            //if (mc.player.distanceTo(hitVec.x, hitVec.y, hitVec.z) > 5.0) {
            //    return false;
            //}

            //方案2：使用 Vec3d 的 distanceTo
            if (hitVec.distanceTo(mc.player.getPos()) > 5.0) {
                return false
            }

            return true
        }
    }
}
