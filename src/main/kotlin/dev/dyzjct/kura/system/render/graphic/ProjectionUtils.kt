package dev.dyzjct.kura.system.render.graphic

import dev.dyzjct.kura.graphics.MatrixUtils
import dev.dyzjct.kura.system.util.interfaces.MinecraftWrapper
import net.minecraft.entity.Entity
import net.minecraft.util.math.Vec3d
import org.joml.Matrix4f
import org.joml.Vector4f
import java.nio.ByteBuffer
import java.nio.ByteOrder

object ProjectionUtils : MinecraftWrapper {
    private val floatBuffer = ByteBuffer.allocateDirect(16 shl 2).order(ByteOrder.nativeOrder()).asFloatBuffer()
    private val modelMatrix = Matrix4f()
    private val projectionMatrix = Matrix4f()

    var projectPos = Vec3d.ZERO
        private set
    var camPos = Vec3d.ZERO
        private set

    fun toAbsoluteScreenPos(pos: Vec3d): Vec3d {
        return toScreenPos(pos, mc.window.width.toFloat(), mc.window.height.toFloat())
    }

    fun toScreenPos(pos: Vec3d, width: Float, height: Float): Vec3d {
        val vector4f = transformVec3(pos)

        vector4f.x = width / 2.0f + (0.5f * vector4f.x * width + 0.5f)
        vector4f.y = height / 2.0f - (0.5f * vector4f.y * height + 0.5f)
        val posZ = if (isVisible(vector4f, width, height)) 0.0 else -1.0

        return Vec3d(vector4f.x.toDouble(), vector4f.y.toDouble(), posZ)
    }

    private fun transformVec3(vec3d: Vec3d): Vector4f {
        // 计算相对于相机的相对坐标
        val relativeX = (vec3d.x - projectPos.x).toFloat()
        val relativeY = (vec3d.y - projectPos.y).toFloat()
        val relativeZ = (vec3d.z - projectPos.z).toFloat()

        val vector4f = Vector4f(relativeX, relativeY, relativeZ, 1.0f)

        // 使用 JOML 内置方法替代 transformVec4
        modelMatrix.transform(vector4f)
        projectionMatrix.transform(vector4f)

        if (vector4f.w <= 0.0f) {
            // 如果在相机背后，w 通常 <= 0 (取决于 OpenGL 坐标系习惯)
            // 这里的逻辑可以根据你的需求调整，通常是设置一个标记位
        } else {
            val invert = 1.0f / vector4f.w
            vector4f.x *= invert
            vector4f.y *= invert
            vector4f.z *= invert
        }

        return vector4f
    }

    private fun transformVec4(vec: Vector4f, matrix: Matrix4f) {
        val x = vec.x
        val y = vec.y
        val z = vec.z
        vec.x = x * matrix.m00() + y * matrix.m10() + z * matrix.m20() + matrix.m30()
        vec.y = x * matrix.m01() + y * matrix.m11() + z * matrix.m21() + matrix.m31()
        vec.z = x * matrix.m02() + y * matrix.m12() + z * matrix.m22() + matrix.m32()
        vec.w = x * matrix.m03() + y * matrix.m13() + z * matrix.m23() + matrix.m33()
    }

    private fun isVisible(vector4f: Vector4f, width: Float, height: Float): Boolean {
        return vector4f.x in 0.0f..width && vector4f.y in 0.0f..height
    }

    private fun projectViewFromEntity(entity: Entity, tickDelta: Double): Vec3d {
        val d0 = entity.lastRenderX + (entity.x - entity.lastRenderX) * tickDelta
        val d1 = entity.lastRenderY + (entity.y - entity.lastRenderY) * tickDelta
        val d2 = entity.lastRenderZ + (entity.z - entity.lastRenderZ) * tickDelta
        val cameraPos = mc.cameraEntity ?: return Vec3d.ZERO
        val d3 = d0 + cameraPos.pos.x
        val d4 = d1 + cameraPos.pos.y
        val d5 = d2 + cameraPos.pos.z
        return Vec3d(d3, d4, d5)
    }

    fun updateMatrix() {
        val entity = mc.cameraEntity ?: player ?: return
        val viewerPos = projectViewFromEntity(entity, RenderUtils3D.partialTicks.toDouble())
        val relativeCamPos = mc.cameraEntity?.pos

        MatrixUtils.loadProjectionMatrix().getMatrix(projectionMatrix)
        MatrixUtils.loadModelViewMatrix().getMatrix(modelMatrix)

        projectPos = viewerPos.add(relativeCamPos)
        camPos = viewerPos
    }
}
