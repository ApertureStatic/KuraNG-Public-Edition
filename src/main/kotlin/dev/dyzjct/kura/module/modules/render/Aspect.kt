package dev.dyzjct.kura.module.modules.render

import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module

object Aspect : Module(name = "Aspect", langName = "视角", description = "Set Aspect", category = Category.RENDER, type = Type.Both) {
    val ratio by fsetting("Aspect", 1.7f, 0.1f, 2.5f)
}