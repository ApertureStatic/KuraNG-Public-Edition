package base.utils.keyboard

import net.minecraft.client.MinecraftClient
import net.minecraft.client.util.InputUtil
import net.minecraft.util.Util
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor

object KeyboardUtils {
    fun getKeyName(keyCode: Int): String {
        val key = InputUtil.fromKeyCode(keyCode, -1)
        val translationKey = key.translationKey

        // Check if the translation key starts with "key.keyboard."
        if (translationKey.startsWith("key.keyboard.")) {
            // Calculate the index to start the substring
            val startIndex = "key.keyboard.".length

            // Make sure the startIndex is within the valid range
            if (startIndex < translationKey.length) {
                return translationKey.substring(startIndex)
            }
        }

        // If the translation key doesn't match the expected format, return an alternative value
        return "Unknown"
    }

    private fun isKeyBindingDown(keyCode: Int): Boolean {
        return InputUtil.isKeyPressed(MinecraftClient.getInstance().window.handle, keyCode)
    }

    val isShiftDown: Boolean
        get() = isKeyBindingDown(42) || isKeyBindingDown(54)

    @JvmStatic
    val isCtrlDown: Boolean
        get() = if (Util.getOperatingSystem() == Util.OperatingSystem.OSX) {
            isKeyBindingDown(219) || isKeyBindingDown(220)
        } else isKeyBindingDown(29) || isKeyBindingDown(157)
    val isAltDown: Boolean
        get() = isKeyBindingDown(56) || isKeyBindingDown(184)

    @JvmStatic
    fun isDown(key: Int): Boolean {
        return isKeyBindingDown(key)
    }

    @JvmStatic
    val clipboardString: String
        get() {
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            return runCatching {
                clipboard.getData(DataFlavor.stringFlavor).toString()
            }.getOrElse { "" }
        }
}