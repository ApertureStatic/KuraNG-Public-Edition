//package dev.dyzjct.kura.module.modules.movement
//
//import base.utils.concurrent.threads.runSafe
//import dev.dyzjct.kura.module.Category
//import dev.dyzjct.kura.module.Module
//import dev.dyzjct.kura.setting.ModeSetting
//import net.minecraft.network.play.server.SPacketEntityVelocity
//import kotlin.math.cos
//import kotlin.math.sin
//
//class VelocityTest : Module(
//    name = "VelocityTest",
//    langName = "水影反击退",
//    category = Category.MOVEMENT,
//    description = "Liquidbounce's VelocityMod.",
//    type = Type.Both
//) {
//
//
//    /**
//     * OPTIONS
//     */
//    private val horizontalValue by fsetting("Horizontal", 0F, 0F, 1F)
//    private val verticalValue by fsetting("Vertical", 0F, 0F, 1F)
//    val modeValue = ModeSetting("Mode", Mode.Vanilla)
//    private val aac4XZReducerValue by fsetting("AAC4XZReducer", 1.36F, 1F, 3F)
//    private val newaac4XZReducerValue by fsetting("NewAAC4XZReducer", 0.45F, 0F, 1F)
//
//    private val velocityTickValue by isetting("VelocityTick", 1, 0, 10)
//
//    // Reverse
//    private val reverseStrengthValue by fsetting("ReverseStrength", 1F, 0.1F, 1F)
//    private val reverse2StrengthValue by fsetting("SmoothReverseStrength", 0.05F, 0.02F, 0.1F)
//
//    private val hytpacketaset by fsetting("HytPacketASet", 0.35F, 0.1F, 1F)
//    private val hytpacketbset by fsetting("HytPacketBSet", 0.5F, 1F, 1F)
//
//    // AAC Push
//    private val aacPushXZReducerValue by fsetting("AACPushXZReducer", 2F, 1F, 3F)
//    private val aacPushYReducerValue by bsetting("AACPushYReducer", true)
//    var block: IBlock? = null
//
//    private val noFireValue by bsetting("noFire", false)
//    private val cobwebValue by bsetting("NoCobweb", true)
//
//    private val onlyCombatValue by bsetting("OnlyCombat", false)
//    private val onlyGroundValue by bsetting("OnlyGround", false)
//    private val hytGround by bsetting("HytGround", true)
//
//    //Custom
//    private val customX by fsetting("CustomX", 0F, 0F, 1F)
//    private val customYStart by bsetting("CanCustomY", false)
//    private val customY by fsetting("CustomY", 1F, 1F, 2F)
//    private val customZ by fsetting("CustomZ", 0F, 0F, 1F)
//    private val customC06FakeLag by bsetting("CustomC06FakeLag", false)
//
//    private var huayutingjumpflag = false
//
//    /**
//     * VALUES
//     */
//    private var velocityTimer = MSTimer()
//    private var
//            velocityInput = false
//    private var canCleanJump = false
//    private var velocityTick = 0
//
//    // SmoothReverse
//    private var reverseHurt = false
//
//    private var jumpingflag = false
//
//    // AACPush
//    private var jump = false
//    private var canCancelJump = false
//
//    override fun onDisable() {
//        runSafe {
//            player.speedInAir = 0.02F
//        }
//    }
//
//    init {
//        onMotion {
//            if (player.isInWater || player.isInLava || player.isInWeb)
//                return
//            if ((onlyGroundValue.get() && !player.onGround) || (onlyCombatValue && !LiquidBounce.combatManager.inCombat)) {
//                return
//            }
//            if (noFireValue && player.burning) return
//            when (modeValue.toLowerCase()) {
//                "grimreduce" -> {
//                    if (player.hurtTime > 0) {
//                        player.motionX += -1.0E-7
//                        player.motionY += -1.0E-7
//                        player.motionZ += -1.0E-7
//                        player.isAirBorne = true
//                    }
//                }
//
//                "grim-motion" -> {
//                    if (player.hurtTime > 0) {
//                        player.motionX += -1.1E-7
//                        player.motionY += -1.1E-7
//                        player.motionZ += -1.2E-7
//                        player.isAirBorne = true
//                    }
//                }
//
//                "test" -> {
//                    if (player.hurtTime > 0 && jumpingflag) {
//                        if (player.onGround) {
//                            player.hurtTime <= 2
//                            player.motionX *= 0.1
//                            player.motionZ *= 0.1
//                            player.hurtTime <= 4
//                            player.motionX *= 0.2
//                            player.motionZ *= 0.2
//                        } else if (player.hurtTime <= 2) {
//                            player.motionX *= 0.1
//                            player.motionZ *= 0.1
//                        }
//                        Companion.mc.netHandler.addToSendQueue(
//                            classProvider.createCPacketEntityAction(
//                                player,
//                                ICPacketEntityAction.WAction.START_SNEAKING
//                            )
//                        )
//                        jumpingflag = false
//                    }
//                }
//
//                "test1" -> {
//                    if (player.hurtTime > 0 && jumpingflag) {
//                        player.hurtTime <= 2
//                        player.motionX *= 0.4
//                        player.motionZ *= 0.4
//                        player.hurtTime <= 4
//                        player.motionX *= 0.5
//                        player.motionZ *= 0.5
//                    } else if (player.hurtTime <= 9) {
//                        player.motionX *= 0.6
//                        player.motionZ *= 0.6
//                    }
//                    Companion.mc.netHandler.addToSendQueue(
//                        classProvider.createCPacketEntityAction(
//                            player,
//                            ICPacketEntityAction.WAction.START_SNEAKING
//                        )
//                    )
//                    jumpingflag = false
//                }
//
//                "test2" -> {
//                    if (player.hurtTime > 0 && jumpingflag) {
//                        player.hurtTime <= 6
//                        player.motionX *= 0.3
//                        player.motionZ *= 0.3
//                        player.hurtTime <= 4
//                        player.motionX *= 0.5
//                        player.motionZ *= 0.5
//                    } else {
//                        player.motionX *= 0.3
//                        player.motionZ *= 0.3
//                    }
//                    Companion.mc.netHandler.addToSendQueue(
//                        classProvider.createCPacketEntityAction(
//                            player,
//                            ICPacketEntityAction.WAction.START_SNEAKING
//                        )
//                    )
//                    jumpingflag = false
//                }
//
//                "Hytjump1" -> {
//                    player.noClip = velocityInput
//
//                    if (player.hurtTime == 7)
//                        player.motionX *= 0
//                    player.motionZ *= 0
//                    player.motionY = 0.42
//
//                    Companion.mc.netHandler.addToSendQueue(
//                        classProvider.createCPacketPlayerPosition(
//                            player.posX,
//                            player.posY,
//                            player.posZ,
//                            true
//                        )
//                    )
//                }
//
//                "hytfwnmslcnm" -> {
//                    if (player.hurtTime > 0 && jumpingflag) {
//                        if (player.onGround) {
//                            player.hurtTime <= 6
//                            player.motionX *= 0.326934583
//                            player.motionZ *= 0.326934583
//                            player.hurtTime <= 4
//                            player.motionX *= 0.428534723
//                            player.motionZ *= 0.428534723
//                        } else if (player.hurtTime <= 9) {
//                            player.motionX *= 0.326934583
//                            player.motionZ *= 0.326934583
//                        }
//                        Companion.mc.netHandler.addToSendQueue(
//                            classProvider.createCPacketEntityAction(
//                                player,
//                                ICPacketEntityAction.WAction.START_SNEAKING
//                            )
//                        )
//                        jumpingflag = false
//                    }
//                }
//
//                "testaac5" -> {
//                    if (hytGround) {
//                        if (player.hurtTime > 0 && !player.isDead && player.hurtTime <= 5 && player.onGround) {
//                            player.motionX *= 0.35
//                            player.motionZ *= 0.35
//                            player.motionY *= 0.001
//                            player.motionY /= 0.01F
//                        }
//                    } else {
//                        if (player.hurtTime > 0 && !player.isDead && player.hurtTime <= 5) {
//                            player.motionX *= 0.35
//                            player.motionZ *= 0.35
//                            player.motionY *= 0.001
//                            player.motionY /= 0.01F
//                        }
//                    }
//                }
//
//                "jump" -> if (player.hurtTime > 0 && player.onGround) {
//                    player.motionY = 0.42
//
//                    val yaw = player.rotationYaw * 0.017453292F
//
//                    player.motionX -= sin(yaw) * 0.2
//                    player.motionZ += cos(yaw) * 0.2
//                }
//
//                "glitch" -> {
//                    player.noClip = velocityInput
//
//                    if (player.hurtTime == 7)
//                        player.motionY = 0.4
//
//                    velocityInput = false
//                }
//
//                "feile" -> {
//                    if (player.onGround) {
//                        canCleanJump = true
//                        player.motionY = 1.5
//                        player.motionZ = 1.2
//                        player.motionX = 1.5
//                        if (player.onGround && velocityTick > 2) {
//                            velocityInput = false
//                        }
//                    }
//                }
//
//                "aac5reduce" -> {
//                    if (player.hurtTime > 1 && velocityInput) {
//                        player.motionX *= 0.81
//                        player.motionZ *= 0.81
//                    }
//                    if (velocityInput && (player.hurtTime < 5 || player.onGround) && velocityTimer.hasTimePassed(120L)) {
//                        velocityInput = false
//                    }
//                }
//
//                "huayutingjump" -> {
//                    if (player.hurtTime > 0 && huayutingjumpflag) {
//                        if (player.onGround) {
//                            if (player.hurtTime <= 6) {
//                                player.motionX *= 0.600151164
//                                player.motionZ *= 0.600151164
//                            }
//                            if (player.hurtTime <= 4) {
//                                player.motionX *= 0.700151164
//                                player.motionZ *= 0.700151164
//                            }
//                        } else if (player.hurtTime <= 9) {
//                            player.motionX *= 0.6001421204
//                            player.motionZ *= 0.6001421204
//                        }
//                        Companion.mc.netHandler.addToSendQueue(
//                            classProvider.createCPacketEntityAction(
//                                player,
//                                ICPacketEntityAction.WAction.START_SNEAKING
//                            )
//                        )
//                        huayutingjumpflag = false
//                    }
//                }
//
//                "hyttick" -> {
//                    if (velocityTick > velocityTickValue) {
//                        if (player.motionY > 0) player.motionY = 0.0
//                        player.motionX = 0.0
//                        player.motionZ = 0.0
//                        player.jumpMovementFactor = -0.00001f
//                        velocityInput = false
//                    }
//                    if (player.onGround && velocityTick > 1) {
//                        velocityInput = false
//                    }
//                }
//
//                "reverse" -> {
//                    if (!velocityInput)
//                        return
//
//                    if (!player.onGround) {
//                        MovementUtils.strafe(MovementUtils.speed * reverseStrengthValue)
//                    } else if (velocityTimer.hasTimePassed(80L))
//                        velocityInput = false
//                }
//
//                "aac4" -> {
//                    if (!player.onGround) {
//                        if (velocityInput) {
//                            player.speedInAir = 0.02f
//                            player.motionX *= 0.6
//                            player.motionZ *= 0.6
//                        }
//                    } else if (velocityTimer.hasTimePassed(80L)) {
//                        velocityInput = false
//                        player.speedInAir = 0.02f
//                    }
//                }
//
//                "newaac4" -> {
//                    if (player.hurtTime > 0 && !player.onGround) {
//                        val reduce = newaac4XZReducerValue
//                        player.motionX *= reduce
//                        player.motionZ *= reduce
//                    }
//                }
//
//
//                "hyttestaac4" -> {
//                    if (player.isInWater || player.isInLava || player.isInWeb) {
//                        return
//                    }
//                    if (!player.onGround) {
//                        if (velocityInput) {
//                            player.speedInAir = 0.02f
//                            player.motionX *= 0.6
//                            player.motionZ *= 0.6
//                        }
//                    } else if (velocityTimer.hasTimePassed(80L)) {
//                        velocityInput = false
//                        player.speedInAir = 0.02f
//                    }
//                }
//
//                "hytbest" -> {
//                    if (player.hurtTime > 0) {
//                        player.motionX /= 1
//                        player.motionZ /= 1
//                    }
//                }
//
//                "smoothreverse" -> {
//                    if (!velocityInput) {
//                        player.speedInAir = 0.02F
//                        return
//                    }
//
//                    if (player.hurtTime > 0)
//                        reverseHurt = true
//
//                    if (!player.onGround) {
//                        if (reverseHurt)
//                            player.speedInAir = reverse2StrengthValue
//                    } else if (velocityTimer.hasTimePassed(80L)) {
//                        velocityInput = false
//                        reverseHurt = false
//                    }
//                }
//
//                "aac" -> if (velocityInput && velocityTimer.hasTimePassed(80L)) {
//                    player.motionX *= horizontalValue
//                    player.motionZ *= horizontalValue
//                    //mc.player.motionY *= verticalValue ?
//                    velocityInput = false
//                }
//
//                "hyt" -> {
//                    if (player.hurtTime > 0 && !player.onGround) {
//                        player.motionX *= 0.45f
//                        player.motionZ *= 0.65f
//                    }
//                }
//
//                "hytpacket" -> {
//                    if (hytGround) {
//                        if (player.hurtTime > 0 && !player.isDead && player.hurtTime <= 5 && player.onGround) {
//                            player.motionX *= 0.5
//                            player.motionZ *= 0.5
//                            player.motionY /= 1.781145F
//                        }
//                    } else {
//                        if (player.hurtTime > 0 && !player.isDead && player.hurtTime <= 5) {
//                            player.motionX *= 0.5
//                            player.motionZ *= 0.5
//                            player.motionY /= 1.781145F
//                        }
//                    }
//
//                }
//
//                "hytmotion" -> {
//                    if (hytGround) {
//                        if (player.hurtTime > 0 && !player.isDead && player.hurtTime <= 5 && player.onGround) {
//                            player.motionX *= 0.4
//                            player.motionZ *= 0.4
//                            player.motionY *= 0.381145F
//                            player.motionY /= 1.781145F
//                        }
//                    } else {
//                        if (player.hurtTime > 0 && !player.isDead && player.hurtTime <= 5) {
//                            player.motionX *= 0.4
//                            player.motionZ *= 0.4
//                            player.motionY *= 0.381145F
//                            player.motionY /= 1.781145F
//                        }
//                    }
//
//                }
//
//                "hytmotionb" -> {
//                    if (player.hurtTime > 0 && !player.isDead && !player.onGround) {
//                        if (!player.isPotionActive(classProvider.getPotionEnum(PotionType.MOVE_SPEED))) {
//                            player.motionX *= 0.451145F
//                            player.motionZ *= 0.451145F
//                        }
//                    }
//                }
//
//                "newhytmotion" -> {
//                    if (player.hurtTime > 0 && !player.isDead && !player.onGround) {
//                        if (!player.isPotionActive(classProvider.getPotionEnum(PotionType.MOVE_SPEED))) {
//                            player.motionX *= 0.47188
//                            player.motionZ *= 0.47188
//                            if (player.motionY == 0.42 || player.motionY > 0.42) player.motionY *= 0.4
//                        } else {
//                            player.motionX *= 0.65025
//                            player.motionZ *= 0.65025
//                            if (player.motionY == 0.42 || player.motionY > 0.42) player.motionY *= 0.4
//                        }
//                    }
//                }
//
//                "aacpush" -> {
//                    if (jump) {
//                        if (player.onGround)
//                            jump = false
//                    } else {
//                        // Strafe
//                        if (player.hurtTime > 0 && player.motionX != 0.0 && player.motionZ != 0.0)
//                            player.onGround = true
//
//                        // Reduce Y
//                        if (player.hurtResistantTime > 0 && aacPushYReducerValue
//                            && !LiquidBounce.moduleManager[Speed::class.java]!!.state
//                        )
//                            player.motionY -= 0.014999993
//                    }
//
//
//                    // Reduce XZ
//                    if (player.hurtResistantTime >= 19) {
//                        val reduce = aacPushXZReducerValue
//
//                        player.motionX /= reduce
//                        player.motionZ /= reduce
//                    }
//                }
//
//                "custom" -> {
//                    if (player.hurtTime > 0 && !player.isDead && !player.isPotionActive(
//                            classProvider.getPotionEnum(
//                                PotionType.MOVE_SPEED
//                            )
//                        ) && !player.isInWater
//                    ) {
//                        player.motionX *= customX
//                        player.motionZ *= customZ
//                        if (customYStart) player.motionY /= customY
//                        if (customC06FakeLag) Companion.mc.netHandler.addToSendQueue(
//                            classProvider.createCPacketPlayerPosLook(
//                                player.posX,
//                                player.posY,
//                                player.posZ,
//                                player.rotationYaw,
//                                player.rotationPitch,
//                                player.onGround
//                            )
//                        )
//                    }
//                }
//
//                "aaczero" -> if (player.hurtTime > 0) {
//                    if (!velocityInput || player.onGround || player.fallDistance > 2F)
//                        return
//
//                    player.motionY -= 1.0
//                    player.isAirBorne = true
//                    player.onGround = true
//                } else
//                    velocityInput = false
//            }
//
//
//        }
//
//        onPacketSend {
//            val player = Companion.mc.player ?: return
//
//            val packet = event.packet
//
//            if (classProvider.isSPacketEntityVelocity(packet)) {
//                val packetEntityVelocity = packet.asSPacketEntityVelocity()
//
//
//                if (noFireValue && mc.player!!.burning) return
//                if ((mc.theWorld?.getEntityByID(packetEntityVelocity.entityID) ?: return) != player)
//                    return
//
//                velocityTimer.reset()
//
//                when (modeValue.toLowerCase()) {
//                    "noxz" -> {
//                        if (packetEntityVelocity.motionX == 0 && packetEntityVelocity.motionZ == 0) {
//                            return
//                        }
//                        val ka = LiquidBounce.moduleManager[KillAura::class.java] as KillAura
//                        val target = LiquidBounce.combatManager.getNearByEntity(ka.rangeValue + 1) ?: return
//                        mc.player!!.motionX = 0.0
//                        mc.player!!.motionZ = 0.0
//                        packetEntityVelocity.motionX = 0
//                        packetEntityVelocity.motionZ = 0
//                        for (i in 0..hytCount) {
//                            mc.player!!.sendQueue.addToSendQueue(
//                                classProvider.createCPacketUseEntity(
//                                    target,
//                                    ICPacketUseEntity.WAction.ATTACK
//                                )
//                            )
//                            mc.player!!.sendQueue.addToSendQueue(classProvider.createCPacketAnimation())
//                        }
//                        if (hytCount > 12) hytCount -= 5
//                    }
//
//                    "jumping" -> {
//                        if (packet.unwrap() is SPacketEntityVelocity) {
//                            jumpingflag = true
//
//                            if (mc.player!!.hurtTime != 0) {
//                                event.cancelEvent()
//                                packet.asSPacketEntityVelocity().motionX = 0
//                                packet.asSPacketEntityVelocity().motionY = 0
//                                packet.asSPacketEntityVelocity().motionZ = 0
//                            }
//                        }
//                    }
//
//                    "vanilla" -> {
//                        event.cancelEvent()
//                    }
//
//                    "huayutingjump" -> {
//                        huayutingjumpflag = true
//
//                        if (mc.player!!.hurtTime != 0) {
//                            event.cancelEvent()
//                            packetEntityVelocity.motionX = 0
//                            packetEntityVelocity.motionY = 0
//                            packetEntityVelocity.motionZ = 0
//                        }
//                    }
//
//                    "simple" -> {
//                        val horizontal = horizontalValue
//                        val vertical = verticalValue
//
//                        if (horizontal == 0F && vertical == 0F)
//                            event.cancelEvent()
//
//                        packetEntityVelocity.motionX = (packetEntityVelocity.motionX * horizontal).toInt()
//                        packetEntityVelocity.motionY = (packetEntityVelocity.motionY * vertical).toInt()
//                        packetEntityVelocity.motionZ = (packetEntityVelocity.motionZ * horizontal).toInt()
//                    }
//
//                    "hytpacketfix" -> {
//                        if (player.hurtTime > 0 && !player.isDead && !mc.player!!.isPotionActive(
//                                classProvider.getPotionEnum(
//                                    PotionType.MOVE_SPEED
//                                )
//                            ) && !mc.player!!.isInWater
//                        ) {
//                            player.motionX *= 0.4
//                            player.motionZ *= 0.4
//                            player.motionY /= 1.45F
//                        }
//                        if (player.hurtTime < 1) {
//                            packetEntityVelocity.motionY = 0
//                        }
//                        if (player.hurtTime < 5) {
//                            packetEntityVelocity.motionX = 0
//                            packetEntityVelocity.motionZ = 0
//                        }
//                    }
//
//                    "hyttest" -> {
//                        if (player.onGround) {
//                            canCancelJump = false
//                            packetEntityVelocity.motionX = (0.985114).toInt()
//                            packetEntityVelocity.motionY = (0.885113).toInt()
//                            packetEntityVelocity.motionZ = (0.785112).toInt()
//                            player.motionX /= 1.75
//                            player.motionZ /= 1.75
//                        }
//                    }
//
//
//                    "hytnewtest" -> {
//                        if (player.onGround) {
//                            velocityInput = true
//                            val yaw = player.rotationYaw * 0.017453292F
//                            packetEntityVelocity.motionX = (packetEntityVelocity.motionX * 0.75).toInt()
//                            packetEntityVelocity.motionZ = (packetEntityVelocity.motionZ * 0.75).toInt()
//                            player.motionX -= sin(yaw) * 0.2
//                            player.motionZ += cos(yaw) * 0.2
//                        }
//                    }
//
//                    "hytpacketa" -> {
//                        packetEntityVelocity.motionX =
//                            (packetEntityVelocity.motionX * hytpacketaset / 1.5).toInt()
//                        packetEntityVelocity.motionY = (0.7).toInt()
//                        packetEntityVelocity.motionZ =
//                            (packetEntityVelocity.motionZ * hytpacketaset / 1.5).toInt()
//                        event.cancelEvent()
//                    }
//
//                    "hytpacketb" -> {
//                        packetEntityVelocity.motionX =
//                            (packetEntityVelocity.motionX * hytpacketbset / 2.5).toInt()
//                        packetEntityVelocity.motionY =
//                            (packetEntityVelocity.motionY * hytpacketbset / 2.5).toInt()
//                        packetEntityVelocity.motionZ =
//                            (packetEntityVelocity.motionZ * hytpacketbset / 2.5).toInt()
//                    }
//
//                    "hyttestaac4" -> {
//                        if (mc.player == null || (mc.theWorld?.getEntityByID(packetEntityVelocity.entityID)
//                                ?: return) != mc.player
//                        ) return
//                        velocityTimer.reset()
//                        velocityInput = true
//                    }
//
//                    "aac", "aac4", "reverse", "smoothreverse", "aac5reduce", "aaczero" -> velocityInput = true
//
//                    "hyttick" -> {
//                        velocityInput = true
//                        val horizontal = 0F
//                        val vertical = 0F
//
//                        event.cancelEvent()
//
//                    }
//
//                    "glitch" -> {
//                        if (!player.onGround)
//                            return
//
//                        velocityInput = true
//                        event.cancelEvent()
//                    }
//
//                    "hytcancel" -> {
//                        event.cancelEvent()
//                    }
//                }
//
//            }
//        }
//    }
//
//    @EventTarget
//    fun onBlockBB(event: BlockBBEvent) {
//        block = event.block
//    }
//
//    //    @NativeMethod
//    var hytCount = 24
//
//    @EventTarget
//    fun onJump(event: JumpEvent) {
//        val player = mc.player
//
//        if (player == null || player.isInWater || player.isInLava || player.isInWeb)
//            return
//
//        when (modeValue.toLowerCase()) {
//            "aacpush" -> {
//                jump = true
//
//                if (!player.isCollidedVertically)
//                    event.cancelEvent()
//            }
//
//            "aac4" -> {
//                if (player.hurtTime > 0) {
//                    event.cancelEvent()
//                }
//            }
//
//            "aaczero" -> if (player.hurtTime > 0)
//                event.cancelEvent()
//
//        }
//
//    }
//
//    enum class Mode {
//        GrimReduce, GrimMotion, HytFwNmslCnm, test, Hytjump1, vanilla, test1, test2, Jumping, TestAAC5, HytTestAAC4, HytBest, HuaYuTingJump, Custom, AAC4, Simple, SimpleFix, AAC, AACPush, AACZero,
//        Reverse, SmoothReverse, Jump, AAC5Reduce, HytPacketA, Glitch, HytCancel, HytTick, Vanilla, HytTest, HytNewTest, HytPacket, NewAAC4, Hyt, FeiLe, HytMotion, NewHytMotion, HytPacketB, HytMotionB, HytPacketFix, NoXZ
//    }
//}
