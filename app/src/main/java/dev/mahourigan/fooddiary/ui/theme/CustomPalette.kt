package dev.mahourigan.fooddiary.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Three colours you choose, and about twenty the app works out from them.
 *
 * The split is deliberate. Paper, ink and one accent are choices a person can
 * hold in their head and actually make; primaryContainer against
 * onSecondaryContainer is not, and offering nineteen pickers would mean either
 * a very long screen or a diary you cannot read.
 *
 * **What is guaranteed and what is yours.** The severity ramp and the live-row
 * edge are derived to fixed contrast targets against your paper, whatever
 * colours you pick, because those two carry meaning rather than mood — a
 * severity band that stops climbing has stopped saying anything. Text contrast
 * is the other way round: it is checked, it is reported in plain numbers, and
 * then it is yours. [ignoreContrast] is how you say you have read the warning
 * and want it anyway.
 */
@Serializable
data class CustomPalette(
    /** Paper. Its lightness alone decides whether the app is a light or dark one. */
    val ground: Long = 0xFFEDEFE6L,
    /** Body text, and everything derived by fading toward the paper. */
    val ink: Long = 0xFF22301FL,
    /** Buttons, the live edge, the tint on an open episode. */
    val accent: Long = 0xFF7A3B52L,
    /** Set once you've seen the warnings below and want it regardless. */
    val ignoreContrast: Boolean = false,
) {
    val groundColor: Color get() = Color(ground)
    val inkColor: Color get() = Color(ink)
    val accentColor: Color get() = Color(accent)

    /**
     * Whether this reads as a dark theme.
     *
     * Asked of the paper, not of a setting. A custom palette has no light and
     * dark twin — you picked the ground, so the ground is the ground — and the
     * status-bar icons and the derived shades all key off this instead.
     */
    val isDark: Boolean get() = groundColor.luminance() < 0.5f
}

// ---------------------------------------------------------------------------
// Colour arithmetic
//
// Kept here as plain functions over Compose's Color so the palette tests can
// run them on the JVM, the same way they already do for the built-in ten.
// ---------------------------------------------------------------------------

/** WCAG relative-luminance contrast ratio. */
internal fun contrastOf(a: Color, b: Color): Float {
    val la = a.luminance() + 0.05f
    val lb = b.luminance() + 0.05f
    return if (la > lb) la / lb else lb / la
}

/** [t] of the way from [a] to [b]. */
internal fun mix(a: Color, b: Color, t: Float): Color = Color(
    red = a.red + (b.red - a.red) * t,
    green = a.green + (b.green - a.green) * t,
    blue = a.blue + (b.blue - a.blue) * t,
)

/** Whichever of the two can be read on top of [on]. */
internal fun readableOn(on: Color, a: Color, b: Color): Color =
    if (contrastOf(on, a) >= contrastOf(on, b)) a else b

/**
 * Text for [on], preferring the palette’s own two colours and falling back to
 * plain black or white when neither is legible.
 *
 * The fallback is not decoration. Pick a near-black paper and a dim grey ink
 * and *both* of your colours disappear against a dark accent — the label on a
 * button would come out at 2:1 whichever way it went. Your ink on your paper
 * stays exactly as chosen and is merely reported; a colour nobody chose, on a
 * surface nobody chose, has no such excuse.
 */
internal fun legibleOn(on: Color, a: Color, b: Color): Color {
    val best = readableOn(on, a, b)
    if (contrastOf(on, best) >= 3f) return best
    return readableOn(on, Color.White, Color.Black)
}

private fun Color.withAlpha(a: Float) = copy(alpha = a)

/** Hue in degrees and saturation, so a colour can be relit without shifting. */
private fun Color.hue(): Float {
    val mx = max(red, max(green, blue))
    val mn = min(red, min(green, blue))
    val d = mx - mn
    if (d < 1e-6f) return 0f
    val h = when (mx) {
        red -> ((green - blue) / d) % 6f
        green -> (blue - red) / d + 2f
        else -> (red - green) / d + 4f
    } * 60f
    return if (h < 0f) h + 360f else h
}

private fun Color.saturationHsl(): Float {
    val mx = max(red, max(green, blue))
    val mn = min(red, min(green, blue))
    val l = (mx + mn) / 2f
    val d = mx - mn
    if (d < 1e-6f) return 0f
    return d / (1f - abs(2f * l - 1f)).coerceAtLeast(1e-6f)
}

private fun hsl(h: Float, s: Float, l: Float): Color {
    val c = (1f - abs(2f * l - 1f)) * s
    val hp = ((h % 360f) + 360f) % 360f / 60f
    val x = c * (1f - abs(hp % 2f - 1f))
    val (r1, g1, b1) = when (hp.toInt()) {
        0 -> Triple(c, x, 0f)
        1 -> Triple(x, c, 0f)
        2 -> Triple(0f, c, x)
        3 -> Triple(0f, x, c)
        4 -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    val m = l - c / 2f
    return Color(
        (r1 + m).coerceIn(0f, 1f),
        (g1 + m).coerceIn(0f, 1f),
        (b1 + m).coerceIn(0f, 1f),
    )
}

/**
 * The same hue and saturation, relit until it sits at [ratio] against [ground].
 *
 * A bisection on HSL lightness rather than a formula, because relative
 * luminance is not linear in lightness and the closed form is not worth the
 * trouble for twenty iterations. Goes darker than the ground on light paper and
 * lighter on dark paper, so the ramp always climbs away from whatever you
 * chose.
 */
internal fun atContrast(hue: Float, saturation: Float, ground: Color, ratio: Float): Color {
    // Whichever direction has further to go, not whichever side of a fixed
    // threshold the paper sits on. Mid greys are darker than they look — #808080
    // is 0.216 in relative luminance, not 0.5 — so a lightness threshold sends
    // the ramp the cramped way and it runs out of room at the top.
    val l = ground.luminance() + 0.05f
    val darker = (l / 0.05f) >= (1.05f / l)
    var lo = if (darker) 0f else lightnessOf(ground)
    var hi = if (darker) lightnessOf(ground) else 1f
    var best = hsl(hue, saturation, if (darker) 0.2f else 0.8f)
    repeat(22) {
        val midL = (lo + hi) / 2f
        val candidate = hsl(hue, saturation, midL)
        val got = contrastOf(ground, candidate)
        best = candidate
        if (got > ratio) {
            // Too far from the paper: come back toward it.
            if (darker) lo = midL else hi = midL
        } else {
            if (darker) hi = midL else lo = midL
        }
    }
    return best
}

/**
 * A colour as hue, saturation and lightness, for the three sliders.
 *
 * Round-trips through [fromHsl] closely but not exactly — 8-bit channels
 * cannot hold every HSL triple — which is why the editor keeps the slider
 * positions in its own state while a drag is in progress rather than
 * re-deriving them from the colour on every frame. Without that, dragging
 * saturation to zero loses the hue and the slider jumps home.
 */
internal fun toHsl(colour: Color): FloatArray =
    floatArrayOf(colour.hue(), colour.saturationHsl(), colour.lightness())

/** The inverse, as the packed ARGB the settings file stores. */
internal fun fromHsl(hue: Float, saturation: Float, lightness: Float): Long {
    val c = hsl(hue, saturation, lightness)
    val r = (c.red * 255f + 0.5f).toInt().coerceIn(0, 255)
    val g = (c.green * 255f + 0.5f).toInt().coerceIn(0, 255)
    val b = (c.blue * 255f + 0.5f).toInt().coerceIn(0, 255)
    return 0xFF000000L or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong()
}

private fun Color.lightness(): Float = lightnessOf(this)

/** HSL lightness, which is not relative luminance and must not be confused with it. */
private fun lightnessOf(c: Color): Float {
    val mx = max(c.red, max(c.green, c.blue))
    val mn = min(c.red, min(c.green, c.blue))
    return (mx + mn) / 2f
}

// ---------------------------------------------------------------------------
// Warnings
// ---------------------------------------------------------------------------

/** Something worth saying about a set of swatches, in numbers rather than adjectives. */
data class ContrastWarning(val what: String, val detail: String)

/**
 * What is wrong with these three, if anything.
 *
 * Reported, never enforced. The thresholds are WCAG's: 7:1 is what the built-in
 * ten are held to for body text, 4.5:1 is the ordinary floor, and 3:1 is the
 * floor for something that is only a shape rather than a word.
 */
fun CustomPalette.warnings(): List<ContrastWarning> {
    val out = mutableListOf<ContrastWarning>()
    val inkOnGround = contrastOf(groundColor, inkColor)
    if (inkOnGround < 4.5f) {
        out += ContrastWarning(
            "Ink on paper is ${fmt(inkOnGround)}:1",
            "Below 4.5:1 body text is hard work for most people and unreadable for " +
                "some. Move the ink further from the paper.",
        )
    } else if (inkOnGround < 7f) {
        out += ContrastWarning(
            "Ink on paper is ${fmt(inkOnGround)}:1",
            "Readable, but under the 7:1 the five built-in themes hold to. Fine in " +
                "good light, tiring in a dim kitchen.",
        )
    }
    val accentOnGround = contrastOf(groundColor, accentColor)
    if (accentOnGround < 3f) {
        out += ContrastWarning(
            "Accent on paper is ${fmt(accentOnGround)}:1",
            "This colours buttons and icons, which need 3:1 to be seen as shapes. " +
                "The live-row edge is relit automatically, so that one still works.",
        )
    }
    if (contrastOf(inkColor, accentColor) < 1.6f) {
        out += ContrastWarning(
            "Ink and accent are nearly the same",
            "A button will not look any different from the text beside it.",
        )
    }
    return out
}

private fun fmt(v: Float): String {
    val tenths = (v * 10).toInt()
    return "${tenths / 10}.${tenths % 10}"
}

// ---------------------------------------------------------------------------
// Derivation
// ---------------------------------------------------------------------------

/**
 * The severity ramp's contrast targets against the paper.
 *
 * Fixed ratios rather than fixed colours, which is what finally settles an
 * argument this app has had before: an amber at "moderate" is brighter than a
 * red at "severe", because yellow hues carry far more luminance, and hand-
 * picking four pleasant colours kept producing ramps that dipped in the middle.
 * Choosing the contrast first and the hue second cannot dip — each step is a
 * fixed multiple further from the paper than the last, so the band reads as a
 * climb in greyscale and for anyone colour-blind, whatever three colours you
 * started from.
 *
 * Mild sits at 2.4 rather than 3, because "barely anything" is supposed to be
 * quiet.
 */
private const val SEVERITY_MILD = 2.4f
private const val SEVERITY_FLOOR = 3.2f
private const val SEVERITY_TOP = 9.0f

/**
 * The four targets, fitted to how much room the paper actually has.
 *
 * A light or dark paper has plenty: black on near-white reaches 20:1, so the
 * ladder is used as written. A *mid* paper has almost none — mid grey tops out
 * near 5:1 in the better of the two directions and 4.6:1 at the very worst
 * lightness — and asking for 9:1 there gets white four times over, which is a
 * ramp that has stopped climbing.
 *
 * So the top rung is capped at what is reachable and the upper three are
 * spread geometrically beneath it, which keeps every step a fixed multiple of
 * the last rather than a fixed distance. Even against the worst possible
 * ground that leaves each band comfortably clear of the one below.
 */
private fun severityTargets(ground: Color): List<Float> {
    val l = ground.luminance() + 0.05f
    val reach = max(l / 0.05f, 1.05f / l) * 0.97f
    val top = min(SEVERITY_TOP, reach)
    val step = kotlin.math.sqrt(top / SEVERITY_FLOOR)
    return listOf(
        min(SEVERITY_MILD, SEVERITY_FLOOR / 1.25f),
        SEVERITY_FLOOR,
        SEVERITY_FLOOR * step,
        top,
    )
}

/** How far the live edge must stand off the paper to survive sunlight. */
private const val LIVE_EDGE_TARGET = 2.6f

internal fun CustomPalette.toSkin(): Skin {
    val ground = groundColor
    val ink = inkColor
    val accent = accentColor
    val dark = isDark

    // Fading ink toward paper gives every muted shade at once, and keeps them
    // in the same family as the two colours actually chosen.
    fun toward(t: Float) = mix(ink, ground, t)

    val surfaceVariant = mix(ground, ink, 0.07f)
    val onSurfaceVariant = toward(0.35f)
    val outline = toward(0.55f)
    val rule = toward(0.78f)

    val primaryContainer = mix(accent, ground, 0.80f)
    val secondaryContainer = mix(accent, ground, 0.86f)
    val tertiaryContainer = mix(accent, ground, 0.72f)

    // Error stays red whatever the accent is. It is the one hue in the app that
    // means something on its own, and borrowing the accent for it would let a
    // red-accented theme make every warning invisible.
    val error = atContrast(hue = 8f, saturation = 0.62f, ground = ground, ratio = 5.0f)
    val errorContainer = mix(error, ground, 0.80f)

    val targets = severityTargets(ground)
    val severity = listOf(
        atContrast(ink.hue(), 0.12f, ground, targets[0]),
        atContrast(accent.hue(), accent.saturationHsl().coerceIn(0.25f, 0.7f), ground, targets[1]),
        atContrast(12f, 0.60f, ground, targets[2]),
        atContrast(2f, 0.72f, ground, targets[3]),
    )

    // The edge is relit rather than taken as given: it is the marker that says
    // an episode is still open, and a pale accent on pale paper would lose it.
    val liveEdge = atContrast(
        accent.hue(),
        accent.saturationHsl().coerceAtLeast(0.2f),
        ground,
        LIVE_EDGE_TARGET,
    )

    val secondary = mix(accent, ink, 0.45f)

    // Named arguments throughout, and the light/dark builder chosen by the
    // paper. Positionally these two take about thirty colours in an order
    // nobody remembers, and getting one pair the wrong way round produces a
    // scheme that looks almost right.
    val base = if (dark) darkColorScheme() else lightColorScheme()
    val scheme: ColorScheme = base.copy(
        primary = accent,
        onPrimary = legibleOn(accent, ground, ink),
        primaryContainer = primaryContainer,
        onPrimaryContainer = legibleOn(primaryContainer, ink, ground),
        inversePrimary = mix(accent, ground, 0.5f),
        secondary = secondary,
        onSecondary = legibleOn(secondary, ground, ink),
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = legibleOn(secondaryContainer, ink, ground),
        tertiary = accent,
        onTertiary = legibleOn(accent, ground, ink),
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = legibleOn(tertiaryContainer, ink, ground),
        background = ground,
        onBackground = ink,
        surface = ground,
        onSurface = ink,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        surfaceTint = accent,
        inverseSurface = ink,
        inverseOnSurface = ground,
        outline = outline,
        outlineVariant = rule,
        error = error,
        onError = legibleOn(error, ground, ink),
        errorContainer = errorContainer,
        onErrorContainer = legibleOn(errorContainer, ink, ground),
    )

    return Skin(
        colors = scheme,
        palette = DiaryPalette(
            ColourTheme.CUSTOM,
            rule = rule,
            liveTint = accent.withAlpha(if (dark) 0.20f else 0.12f),
            liveEdge = liveEdge,
            severity = severity,
        ),
    )
}
