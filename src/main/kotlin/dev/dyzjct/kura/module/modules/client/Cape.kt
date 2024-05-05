package dev.dyzjct.kura.module.modules.client

import dev.dyzjct.kura.KuraIdentifier
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import net.minecraft.util.Identifier

object Cape : Module(
    "Cape", langName = "披风", category = Category.CLIENT, safeModule = true
) {
    private val capeEnum by msetting("Type", Cape.Rimuru)

    @Suppress("UNUSED")
    private enum class Cape(
        val path: String
    ) {
        Rimuru("rimuru_cape.png"),
        RimuruTwo("rimuru_two_cape.png"),
        VAPE("vape_cape.png"),
        Chicken("chicken_cape.png"),
        BadLion("badlion_cape.png"),
        Creeper("creeper_cape.png"),
        Dragon("dragon_cape.png"),
        Elaina("elaina_cape.png"),
        FDP("fdp_cape.png"),
        LUNAR_LIGHT("lunar_light_cape.png"),
        LUNAR_DARK("lunar_dark_cape.png"),
        Mojang("mojang_cape.png"),
        Sakura("sakura.png"),
        Long("Long.png"),
        NovoLine("novoline_cape.png"),
        Sagiri("sagiri_cape.png"),
    }

    fun getCape(): Identifier {
        return KuraIdentifier("cape/${(capeEnum as Cape).path}")
    }
}