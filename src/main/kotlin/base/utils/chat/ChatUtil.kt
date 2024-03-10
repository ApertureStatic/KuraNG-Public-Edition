package base.utils.chat

import dev.dyzjct.kura.Kura
import dev.dyzjct.kura.mixins.IChatHud
import net.minecraft.client.MinecraftClient
import net.minecraft.text.Text


@Suppress("UNUSED")
object ChatUtil {
    var SECTIONSIGN = "\u00A7"
    var BLACK = SECTIONSIGN + "0"
    var DARK_BLUE = SECTIONSIGN + "1"
    var DARK_GREEN = SECTIONSIGN + "2"
    var DARK_AQUA = SECTIONSIGN + "3"
    var DARK_RED = SECTIONSIGN + "4"
    var DARK_PURPLE = SECTIONSIGN + "5"
    var GOLD = SECTIONSIGN + "6"
    var GRAY = SECTIONSIGN + "7"
    var DARK_GRAY = SECTIONSIGN + "8"
    var BLUE = SECTIONSIGN + "9"
    var GREEN = SECTIONSIGN + "a"
    var AQUA = SECTIONSIGN + "b"
    var RED = SECTIONSIGN + "c"
    var LIGHT_PURPLE = SECTIONSIGN + "d"
    var YELLOW = SECTIONSIGN + "e"
    var WHITE = SECTIONSIGN + "f"
    var OBFUSCATED = SECTIONSIGN + "k"
    var BOLD = SECTIONSIGN + "l"
    var STRIKE_THROUGH = SECTIONSIGN + "m"
    var UNDER_LINE = SECTIONSIGN + "n"
    var ITALIC = SECTIONSIGN + "o"
    var RESET = SECTIONSIGN + "r"
    var colorMSG = SECTIONSIGN + "r"
    var colorKANJI = SECTIONSIGN + "d"
    var colorWarn = SECTIONSIGN + "6" + SECTIONSIGN + "l"
    var colorError = SECTIONSIGN + "4" + SECTIONSIGN + "l"
    var colorBracket = SECTIONSIGN + "7"
    var msgCount = 0
    var tempMsg: String? = null

    private fun bracketBuilder(kanji: String): String {
        return "$RESET$colorBracket[$RESET$kanji$colorBracket] $RESET"
    }

    fun sendWarnMessage(message: String) {
        sendMessage(bracketBuilder(colorWarn + "WARN") + RESET + colorMSG + message)
    }

    fun sendErrorMessage(message: String) {
        sendMessage(bracketBuilder(colorError + "ERROR") + RESET + colorMSG + message)
    }

    fun sendMessage(messageArray: Array<String?>) {
        for (message in messageArray) {
            if (message == null) continue
            sendMessage(message)
        }
    }

    fun sendMessage(message: String) {
        tempMsg?.let { tempMsg ->
            if (tempMsg == message) {
                msgCount++
                MinecraftClient.getInstance().inGameHud?.let { gameHUD ->
                    val text = Text.literal(
                        "${bracketBuilder(AQUA + Kura.MOD_NAME)} ${
                            message.replace(
                                "§",
                                SECTIONSIGN
                            )
                        } [x$msgCount]"
                    )
                    gameHUD.chatHud?.addMessage(text)
                }
            } else {
                msgCount = 0
                MinecraftClient.getInstance().inGameHud?.let { gameHUD ->
                    val text =
                        Text.literal("${bracketBuilder(AQUA + Kura.MOD_NAME)} ${message.replace("§", SECTIONSIGN)}")
                    gameHUD.chatHud?.addMessage(text)
                }
            }
        } ?: {
            MinecraftClient.getInstance().inGameHud?.let { gameHUD ->
                val text = Text.literal("${bracketBuilder(AQUA + Kura.MOD_NAME)} ${message.replace("§", SECTIONSIGN)}")
                gameHUD.chatHud?.addMessage(text)
            }
        }
        tempMsg = message
    }

    fun sendNoSpamMessage(message: String) {
        val text = Text.literal("${bracketBuilder(AQUA + Kura.MOD_NAME)} ${message.replace("§", SECTIONSIGN)}")
        (MinecraftClient.getInstance().inGameHud.chatHud as IChatHud).kuraAddMessage(text, text.hashCode())
    }

    fun sendMessageWithID(message: String, id: Int) {
        val text = Text.literal("${bracketBuilder(AQUA + Kura.MOD_NAME)} ${message.replace("§", SECTIONSIGN)}")
        (MinecraftClient.getInstance().inGameHud.chatHud as IChatHud).kuraAddMessage(text, id)
    }
}
