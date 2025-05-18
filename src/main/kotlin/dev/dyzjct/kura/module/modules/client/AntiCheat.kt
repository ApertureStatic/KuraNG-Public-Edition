package dev.dyzjct.kura.module.modules.client

import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module

object AntiCheat : Module(
    name = "AntiCheat",
    category = Category.CLIENT,
    description = "AntiCheat",
) {
    val grimRotation by bsetting("GrimRotation", false)
    val noSpamRotation by bsetting("NoSpamRotation", false)

    val forceSync by bsetting("ForceSync", true)
    var look by bsetting("Look", false)
    val rotateTime by fsetting("LookTime", 0.5f, 0f..1f, 0.01f)
    val fov by fsetting("Fov", 10.0f, 0.0f..180.0f, 0.1f)
    val steps by fsetting("Steps", 0.6f, 0f..1f, 0.01f)
    val blockCheck by bsetting("BlockCheck", false)
    val oldNCP by bsetting("OldNCP", false)
    val movefix by bsetting("Movefix", false)
}