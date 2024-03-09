package melon.utils.text

import melon.utils.graphics.color.EnumTextColor
import net.minecraft.util.Formatting

fun formatValue(value: String) = Formatting.GRAY format "[$value]"

fun formatValue(value: Char) = Formatting.GRAY format "[$value]"

fun formatValue(value: Any) = Formatting.GRAY format "[$value]"

fun formatValue(value: Int) = Formatting.GRAY format "($value)"

infix fun Formatting.format(value: Any) = "$this$value${Formatting.RESET}"

infix fun Formatting.format(value: Int) = "$this$value${Formatting.RESET}"

infix fun EnumTextColor.format(value: Any) = "$this$value${Formatting.RESET}"