package dev.dyzjct.kura.utils.animations

import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d

class DeconstructiveVec3d : Vec3d {
    constructor(xIn: Double, yIn: Double, zIn: Double) : super(xIn, yIn, zIn)
    constructor(vec3d: Vec3d) : super(vec3d.x, vec3d.y, vec3d.z)

    operator fun component1(): Double = this.x

    operator fun component2(): Double = this.y

    operator fun component3(): Double = this.z
}

class BlockEasingRender(pos: BlockPos, movingLength: Float, fadingLength: Float) {

    constructor(movingLength: Float, fadingLength: Float): this(BlockPos.ORIGIN, movingLength, fadingLength)

    private var lastPos : BlockPos
    private var newPos : BlockPos
    private val offset: Vec3d
        get() = Vec3d(
            (newPos.x - lastPos.x).toDouble(),
            (newPos.y - lastPos.y).toDouble(),
            (newPos.z - lastPos.z).toDouble()
        )

    private val animationX: AnimationFlag
    private val animationY: AnimationFlag
    private val animationZ: AnimationFlag
    private var animationSize: AnimationFlag
    private var animationSizeReverse: AnimationFlag

    private var startTime: Long
    private var isEnded = false
    private var size = 0.5

    init {
        lastPos = pos
        newPos = pos
        isEnded = true
        animationX = AnimationFlag(Easing.OUT_CUBIC, movingLength)
        animationY = AnimationFlag(Easing.OUT_CUBIC, movingLength)
        animationZ = AnimationFlag(Easing.OUT_CUBIC, movingLength)
        animationSize = AnimationFlag(Easing.OUT_QUINT, fadingLength)
        animationSizeReverse = AnimationFlag(Easing.IN_QUINT, fadingLength)
        startTime = System.currentTimeMillis()
    }

    fun forceUpdatePos(pos: BlockPos) {
        lastPos = pos
        newPos = pos
        this.animationX.forceUpdate(pos.x.toFloat())
        this.animationY.forceUpdate(pos.y.toFloat())
        this.animationZ.forceUpdate(pos.z.toFloat())
    }

    fun updatePos(pos: BlockPos) {
        lastPos = newPos
        newPos = pos

    }

    fun updatePos(pos: BlockPos, newLength: Float) {
        updatePos(pos)
        animationSize = AnimationFlag(Easing.OUT_QUINT, newLength)
        animationSizeReverse = AnimationFlag(Easing.IN_QUINT, newLength)
    }

    fun getFullUpdate(): Box {
        return getFullUpdate(false)
    }

    fun getFullUpdate(reverse: Boolean): Box {
        val (x, y, z) = DeconstructiveVec3d(getUpdate())
        size = if (!reverse) animationSize.getAndUpdate(if (isEnded) 0f else 50f)
            .toDouble() else animationSizeReverse.getAndUpdate(if (isEnded) 0f else 50f).toDouble()
        val axisAlignedBB = Box(
            x,
            y,
            z,
            x + 1,
            y + 1,
            z + 1
        )
        val centerX = axisAlignedBB.minX + (axisAlignedBB.maxX - axisAlignedBB.minX) / 2
        val centerY = axisAlignedBB.minY + (axisAlignedBB.maxY - axisAlignedBB.minY) / 2
        val centerZ = axisAlignedBB.minZ + (axisAlignedBB.maxZ - axisAlignedBB.minZ) / 2
        return Box(
            centerX + size / 100,
            centerY + size / 100,
            centerZ + size / 100,
            centerX - size / 100,
            centerY - size / 100,
            centerZ - size / 100
        )
    }

    fun getUpdate(): Vec3d {
        return Vec3d(
            animationX.getAndUpdate(offset.x.toFloat() + lastPos.x).toDouble(),
            animationY.getAndUpdate(offset.y.toFloat() + lastPos.y).toDouble(),
            animationZ.getAndUpdate(offset.z.toFloat() + lastPos.z).toDouble()
        )
    }

    fun resetFade() {
        this.animationSize.forceUpdate(0f, 0f)
        this.size = 0.0
    }

    fun reset() {
        this.animationX.forceUpdate(0f, 0f)
        this.animationY.forceUpdate(0f, 0f)
        this.animationZ.forceUpdate(0f, 0f)
        this.animationSize.forceUpdate(0f, 0f)
        this.isEnded = false
        this.size = 0.0
    }

    fun end() {
        this.isEnded = true
    }

    fun begin() {
        this.isEnded = false
    }
}