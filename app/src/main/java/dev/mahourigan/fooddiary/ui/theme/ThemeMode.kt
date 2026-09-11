package dev.mahourigan.fooddiary.ui.theme

import kotlinx.serialization.Serializable

/**
 * Light or dark, within whichever style is chosen.
 *
 * Separate from [ColourTheme] because they answer different questions. A style
 * is an identity and lives in the typefaces; a mode is a colour swap over the
 * top of it. Dark Herbarium is still recognisably Herbarium, which is the whole
 * reason this can be two settings rather than ten styles.
 */
@Serializable
enum class ThemeMode(val label: String) {
    /** Whatever the phone is set to. */
    SYSTEM("Match phone"),
    LIGHT("Light"),
    DARK("Dark"),
    ;

    companion object {
        val DEFAULT = SYSTEM
    }
}
