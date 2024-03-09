package melon.utils.graphics

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer

object RenderBufferUtil {
    @Synchronized
    fun createDirectByteBuffer(buffer: Int): ByteBuffer {
        return ByteBuffer.allocateDirect(buffer).order(ByteOrder.nativeOrder())
    }

    fun createDirectIntBuffer(buffer: Int): IntBuffer {
        return createDirectByteBuffer(buffer shl 2).asIntBuffer()
    }

    fun createDirectFloatBuffer(buffer: Int): FloatBuffer {
        return createDirectByteBuffer(buffer shl 2).asFloatBuffer()
    }
}