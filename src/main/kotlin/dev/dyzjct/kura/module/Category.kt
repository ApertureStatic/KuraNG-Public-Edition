package dev.dyzjct.kura.module

enum class Category(
    private val categoryName: String,
    val isHUD: Boolean,
    val isHidden: Boolean
) {
    COMBAT("Combat", false),
    MISC("Misc", false),
    MOVEMENT("Movement", false),
    PLAYER("Player", false),
    RENDER("Render", false),
    CLIENT("Client", false),
    HUD("HUD", true),
    HIDDEN("Hidden", false, true);

    constructor(categoryName: String, isHUDCategory: Boolean) : this(categoryName, isHUDCategory, false)
}
