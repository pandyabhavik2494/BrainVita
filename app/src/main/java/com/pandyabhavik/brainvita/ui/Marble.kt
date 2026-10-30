package com.pandyabhavik.brainvita.ui

import androidx.compose.ui.graphics.Color
import com.pandyabhavik.brainvita.ui.theme.Palette

/**
 * The finish of the marbles: the dome colour, its highlight and the darker rim. The glass ones get a
 * deep rim and a bright highlight so they read as polished spheres on any board.
 */
enum class Marble(val id: String, val label: String, val base: Color, val light: Color, val dark: Color) {
    BRASS("brass", "Brass", Palette.brass, Palette.brassLight, Palette.brassDark),
    IVORY("ivory", "Ivory", Color(0xFFEDE3CF), Color(0xFFFFFBF0), Color(0xFFB8A98C)),
    JADE("jade", "Jade", Color(0xFF3F9A72), Color(0xFFCFF5E2), Color(0xFF17442F)),
    SAPPHIRE("sapphire", "Sapphire", Color(0xFF2F62B5), Color(0xFFC7DCFF), Color(0xFF122A5C)),
    RUBY("ruby", "Ruby", Color(0xFFB4243B), Color(0xFFFFC4CC), Color(0xFF560D18)),
    OBSIDIAN("obsidian", "Obsidian", Color(0xFF3B3E46), Color(0xFFB4B9C6), Color(0xFF15161A));

    companion object {
        fun byId(id: String?): Marble = entries.firstOrNull { it.id == id } ?: BRASS
    }
}

/** The wood (or stone) the board is cut from. Holes are derived from [dark]. */
enum class BoardFinish(val id: String, val label: String, val base: Color, val light: Color, val dark: Color, val grain: Color, val lip: Color) {
    WALNUT("walnut", "Walnut", Palette.walnut, Palette.walnutLight, Palette.walnutDark, Palette.grain, Palette.holeLip),
    OAK("oak", "Oak", Color(0xFFB07E48), Color(0xFFD4A467), Color(0xFF7D5529), Color(0x30503010), Color(0x70FFF0D0)),
    EBONY("ebony", "Ebony", Color(0xFF2C2420), Color(0xFF443832), Color(0xFF161210), Color(0x2E6A4A3A), Color(0x55D8C0A8)),
    CHERRY("cherry", "Cherry", Color(0xFF8A3E2C), Color(0xFFB05A42), Color(0xFF55231A), Color(0x2C3A1008), Color(0x66FFD8C4)),
    SLATE("slate", "Slate", Color(0xFF4E5560), Color(0xFF6A727E), Color(0xFF30353D), Color(0x1A1C2028), Color(0x55DDE4EE));

    /** Holes: darker than the darkest wood so they always read as depth. */
    val holeDeep: Color get() = Color(dark.red * 0.45f, dark.green * 0.45f, dark.blue * 0.45f)
    val holeShadow: Color get() = Color(dark.red * 0.75f, dark.green * 0.75f, dark.blue * 0.75f)

    companion object {
        fun byId(id: String?): BoardFinish = entries.firstOrNull { it.id == id } ?: WALNUT
    }
}

/** The cloth the board sits on: a lighter centre fading to a darker edge. */
enum class Backdrop(val id: String, val label: String, val centre: Color, val edge: Color) {
    BAIZE("baize", "Baize", Palette.baizeLight, Palette.baize),
    MIDNIGHT("midnight", "Midnight", Color(0xFF1E2A4A), Color(0xFF0F1526)),
    CHARCOAL("charcoal", "Charcoal", Color(0xFF34373C), Color(0xFF1A1C1F)),
    BURGUNDY("burgundy", "Burgundy", Color(0xFF4A1E2A), Color(0xFF260D14)),
    FOREST("forest", "Forest", Color(0xFF17332F), Color(0xFF0B1B18));

    /** For sheets and dialogs: the edge colour, lifted a little. */
    val surface: Color get() = Color(
        edge.red + (centre.red - edge.red) * 0.7f,
        edge.green + (centre.green - edge.green) * 0.7f,
        edge.blue + (centre.blue - edge.blue) * 0.7f,
    )

    companion object {
        fun byId(id: String?): Backdrop = entries.firstOrNull { it.id == id } ?: BAIZE
    }
}
