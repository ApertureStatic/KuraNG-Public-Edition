package melon.system.render.graphic

import melon.system.util.interfaces.MinecraftWrapper
import net.minecraft.entity.Entity
import net.minecraft.util.math.Vec3d
import org.joml.Matrix4f
import org.joml.Vector4f
import team.exception.melon.graphics.MatrixUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder

object ProjectionUtils: MinecraftWrapper {
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
        val relativeX = (projectPos.x - vec3d.x).toFloat()
        val relativeY = (projectPos.y - vec3d.y).toFloat()
        val relativeZ = (projectPos.z - vec3d.z).toFloat()
        val vector4f = Vector4f(relativeX, relativeY, relativeZ, 1.0f)

        transformVec4(vector4f, modelMatrix)
        transformVec4(vector4f, projectionMatrix)

        if (vector4f.w > 0.0f) {
            vector4f.x *= -100000.0f
            vector4f.y *= -100000.0f
        } else {
            val invert = 1.0f / vector4f.w
            vector4f.x *= invert
            vector4f.y *= invert
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
        val entity = mc.cameraEntity ?: player?: return
        val viewerPos = projectViewFromEntity(entity, RenderUtils3D.partialTicks.toDouble())
        val relativeCamPos = mc.cameraEntity?.pos

        MatrixUtils.loadProjectionMatrix().getMatrix(projectionMatrix)
        MatrixUtils.loadModelViewMatrix().getMatrix(modelMatrix)

        projectPos = viewerPos.add(relativeCamPos)
        camPos = viewerPos
    }
}
