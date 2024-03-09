package base.utils

import net.minecraft.client.MinecraftClient
import org.lwjgl.glfw.GLFW

object MouseUtil {
    fun isLeftMouseButtonDown(downType: MouseDownType): Boolean {
        val windowHandle = MinecraftClient.getInstance().window.handle
        return GLFW.glfwGetMouseButton(
            windowHandle, when (downType) {
                MouseDownType.Left -> GLFW.GLFW_MOUSE_BUTTON_1

                MouseDownType.Right -> GLFW.GLFW_MOUSE_BUTTON_2

                MouseDownType.Middle -> GLFW.GLFW_MOUSE_BUTTON_3
            }
        ) == GLFW.GLFW_PRESS
    }

    enum class MouseDownType {
        Left, Right, Middle
    }
}