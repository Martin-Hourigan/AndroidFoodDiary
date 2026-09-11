package dev.mahourigan.fooddiary.ui.theme

import kotlinx.serialization.Serializable

/**
 * The colours and the paper. Not the lettering.
 *
 * These five began as whole identities — palette, typefaces and paper in one
 * choice — on the reasoning that swapping only colours would make five
 * near-identical apps. That reasoning was right about the *defaults* and wrong
 * as a rule: it also meant the only way to read the diary in Petrock was to
 * accept Oblivion's browns. The pairing survives as [TypeFace.pairFor], which
 * is what a diary written before the split is read back with, and what each
 * theme still starts you on.
 *
 * [CUSTOM] is the one whose colours come from the file rather than from here;
 * see [CustomPalette].
 *
 * Stored by name in settings, so adding one here is safe and removing one is
 * not: an unknown name falls back to [HERBARIUM] rather than failing to load
 * the diary.
 */
@Serializable
enum class ColourTheme(
    val label: String,
    /** One line, shown under the name in the picker. */
    val blurb: String,
) {
    HERBARIUM(
        "Herbarium",
        "Sage and forest ink. The plain one.",
    ),
    VELLUM(
        "Vellum & Quill",
        "Parchment, iron gall, rubric red.",
    ),
    GRIMOIRE(
        "Grimoire",
        "Dark, with gold leaf and a wax seal for severity.",
    ),
    HERBAL(
        "Herbal",
        "Rag paper and woodcut ink, off a printed herbal.",
    ),
    CYRODIIL(
        "Cyrodiil",
        "Oblivion's menus. Parchment, bronze and a heavy border.",
    ),
    CUSTOM(
        "Custom",
        "Your own paper, ink and accent.",
    ),
    ;

    /** Whether the light/dark switch means anything here. */
    val hasBothModes: Boolean get() = this != CUSTOM

    companion object {
        val DEFAULT = HERBARIUM

        /**
         * The five with a palette written into the app.
         *
         * [CUSTOM] is excluded wherever the built-in ten are checked as a set:
         * its colours are whatever is in the diary file, so there is nothing
         * fixed to assert about them, and it has no light twin to compare a
         * dark one against.
         */
        val builtIn: List<ColourTheme> get() = entries.filter { it != CUSTOM }
    }
}
