package com.pandyabhavik.brainvita.ui

import androidx.compose.ui.graphics.Color
import com.pandyabhavik.brainvita.ui.theme.Palette

/** The finish of the pegs: three tones for the rim, the dome and its highlight. All sit well on walnut. */
enum class Marble(val id: String, val label: String, val base: Color, val light: Color, val dark: Color) {
    BRASS("brass", "Brass", Palette.brass, Palette.brassLight, Palette.brassDark),
    IVORY("ivory", "Ivory", Color(0xFFEDE3CF), Color(0xFFFFFBF0), Color(0xFFB8A98C)),
    JADE("jade", "Jade", Color(0xFF5FA98A), Color(0xFFBEEBD4), Color(0xFF2E6A52)),
    SAPPHIRE("sapphire", "Sapphire", Color(0xFF3F6FB8), Color(0xFFB9D2F7), Color(0xFF203D6E)),
    RUBY("ruby", "Ruby", Color(0xFFB8374A), Color(0xFFF3B3BC), Color(0xFF6E1B28)),
    OBSIDIAN("obsidian", "Obsidian", Color(0xFF33353B), Color(0xFF8F939C), Color(0xFF111215));

    companion object {
        fun byId(id: String?): Marble = entries.firstOrNull { it.id == id } ?: BRASS
    }
}
