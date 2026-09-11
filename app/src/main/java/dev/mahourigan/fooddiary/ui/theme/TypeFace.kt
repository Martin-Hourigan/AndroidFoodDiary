package dev.mahourigan.fooddiary.ui.theme

import kotlinx.serialization.Serializable

/**
 * One typeface, with the corrections it needs to sit at the same apparent size
 * as the others.
 *
 * The scale lives on the face rather than on a style, which is the change that
 * made picking headings and body text separately possible at all. Petrock's
 * eighteen per cent was never a property of Cyrodiil — it is a property of
 * Petrock, which draws small and light for its point size. Move it onto the
 * face and any pairing inherits the right correction automatically.
 *
 * [displayWeight] and [tracking] only apply when a face is used for headings.
 * They are the weight and letter-spacing that face wants at heading sizes, and
 * they differ per face for the same reason the scale does: blackletter needs
 * air between letters, a grotesque does not.
 */
@Serializable
enum class TypeFace(
    val label: String,
    /** One line for the picker. What it is, and what it's like to read. */
    val note: String,
    /** This face's own size correction, so faces can be swapped freely. */
    val scale: Float,
    /** Weight for headings set in this face, as a Compose weight number. */
    val displayWeight: Int,
    /** Extra letter-spacing for headings, in sp. */
    val tracking: Float,
    /**
     * Whether a paragraph of this is bearable.
     *
     * Not enforced — you can set the whole diary in blackletter if you want to,
     * and the picker will let you. It only earns a line of warning, because the
     * face that looks best on the Style screen is not always the one you can
     * read a list of ingredients in at seven in the morning.
     */
    val readableAsBody: Boolean = true,
) {
    FRAUNCES("Fraunces", "A modern serif with some spring in it. The plain one.", 1f, 600, 0f),
    ARCHIVO("Archivo", "A grotesque. Nothing to look at, and easy to read small.", 1f, 600, 0f),
    IM_FELL("IM Fell", "Seventeenth-century English type, uneven on purpose.", 1.1f, 400, 0.3f),
    GRENZE(
        "Grenze Gotisch",
        "Blackletter. Handsome as a heading, hard work as a paragraph.",
        1.06f, 500, 0.4f,
        readableAsBody = false,
    ),
    CARDO("Cardo", "A quiet scholarly serif. What sits under blackletter.", 1.06f, 600, 0f),
    GARAMOND("EB Garamond", "Old-style and warm, off a printed herbal.", 1.1f, 600, 0f),
    PETROCK("Kingthings Petrock", "Oblivion's menu lettering.", 1.18f, 400, 0f);

    companion object {
        val DEFAULT_DISPLAY = FRAUNCES
        val DEFAULT_BODY = ARCHIVO

        /**
         * What each colour theme used to be set in, for reading a diary written
         * before the two were separable. Someone on Cyrodiil keeps Petrock.
         */
        fun pairFor(theme: ColourTheme): Pair<TypeFace, TypeFace> = when (theme) {
            ColourTheme.HERBARIUM -> FRAUNCES to ARCHIVO
            ColourTheme.VELLUM -> IM_FELL to IM_FELL
            ColourTheme.GRIMOIRE -> GRENZE to CARDO
            ColourTheme.HERBAL -> GARAMOND to GARAMOND
            ColourTheme.CYRODIIL -> PETROCK to PETROCK
            ColourTheme.CUSTOM -> DEFAULT_DISPLAY to DEFAULT_BODY
        }
    }
}
