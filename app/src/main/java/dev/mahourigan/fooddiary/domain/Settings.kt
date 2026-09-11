package dev.mahourigan.fooddiary.domain

import dev.mahourigan.fooddiary.ui.theme.ColourTheme
import dev.mahourigan.fooddiary.ui.theme.CustomPalette
import dev.mahourigan.fooddiary.ui.theme.TypeFace
import dev.mahourigan.fooddiary.ui.theme.ThemeMode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * How energy is shown.
 *
 * Only ever a display choice. Everything is stored as kcal per 100 g, because
 * that's the figure on most packets here and because one stored unit means the
 * log can't end up half in one and half in the other. Switching this converts
 * what you see, including the field you type into.
 */
@Serializable
enum class EnergyUnit(val label: String, val suffix: String, val per100Label: String) {
    KCAL("Calories", "kcal", "kcal per 100g"),
    KILOJOULE("Kilojoules", "kJ", "kJ per 100g");

    /** kcal in, this unit out. */
    fun from(kcal: Double): Double = when (this) {
        KCAL -> kcal
        KILOJOULE -> kcal * KJ_PER_KCAL
    }

    /** This unit in, kcal out — for anything typed in. */
    fun toKcal(value: Double): Double = when (this) {
        KCAL -> value
        KILOJOULE -> value / KJ_PER_KCAL
    }

    /**
     * How coarse an estimate should look.
     *
     * Rounding an estimate to the nearest 10 kcal reads as approximate. The same
     * rounding in kJ would be spurious precision, so it widens with the number.
     */
    val estimateStep: Int get() = when (this) {
        KCAL -> 10
        KILOJOULE -> 50
    }

    companion object {
        const val KJ_PER_KCAL = 4.184
    }
}

@Serializable
data class Settings(
    val energyUnit: EnergyUnit = EnergyUnit.KCAL,

    /**
     * The colours and the paper. Kept under its old name in the file so a
     * diary written before colour and lettering were separable still reads.
     */
    @SerialName("style")
    val colours: ColourTheme = ColourTheme.DEFAULT,

    /**
     * Light or dark within that theme. Ignored by [ColourTheme.CUSTOM], which
     * has one ground and no twin.
     */
    val themeMode: ThemeMode = ThemeMode.DEFAULT,

    /** The face headings are set in. */
    val displayFace: TypeFace = TypeFace.DEFAULT_DISPLAY,

    /** The face everything else is set in. */
    val bodyFace: TypeFace = TypeFace.DEFAULT_BODY,

    /**
     * The three swatches behind [ColourTheme.CUSTOM].
     *
     * Always present, so switching to Custom and back is lossless and there is
     * something to show in the editor before you have chosen anything.
     */
    val custom: CustomPalette = CustomPalette(),

    /** How big the type is set, on top of whatever the phone is already doing. */
    val textSize: TextSize = TextSize.DEFAULT,
)

/**
 * How big the type runs.
 *
 * A multiplier over each style's own scale rather than a set of point sizes,
 * so the relationships inside a style survive: a heading stays a heading, and
 * Petrock keeps the 18% it needs over Fraunces. One number, applied once, at
 * the point the type scale is built.
 *
 * This multiplies with the phone's own font-size setting rather than replacing
 * it — everything is in `sp`, so someone who has already turned the system
 * scale up gets both. That is the right way round: the system setting is the
 * one that follows them between apps, and this is a nudge on top for a diary
 * read at arm's length in a kitchen.
 */
@Serializable
enum class TextSize(val label: String, val factor: Float) {
    SMALL("Small", 0.88f),
    MEDIUM("Medium", 1f),
    LARGE("Large", 1.18f);

    companion object {
        val DEFAULT = MEDIUM
    }
}
