package dev.mahourigan.fooddiary.ui.theme

import dev.mahourigan.fooddiary.domain.TextSize
import dev.mahourigan.fooddiary.R
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------------------
// Typefaces
//
// Mode-independent on purpose. A style's identity lives in its typefaces, not
// its colours — which is exactly why light and dark can be a colour swap and
// still both read as the same style.
//
// The variable fonts are one file each, instanced per weight. The two IM Fell
// cuts and Petrock are single static weights, which is authentic to both — a
// 1670s type had no weight axis, and Petrock only ships regular and light.
// ---------------------------------------------------------------------------

private fun variable(resId: Int, vararg weights: FontWeight) = FontFamily(
    weights.map { weight ->
        Font(
            resId = resId,
            weight = weight,
            variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
        )
    },
)

private val Archivo = variable(R.font.archivo, FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold)
private val Fraunces = variable(R.font.fraunces, FontWeight.Normal, FontWeight.SemiBold, FontWeight.Bold)
private val EbGaramond = variable(R.font.eb_garamond, FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold)
private val GrenzeGotisch = variable(R.font.grenze_gotisch, FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold)
private val ImFell = FontFamily(Font(R.font.im_fell_english))
private val Cardo = FontFamily(Font(R.font.cardo))
private val Petrock = FontFamily(Font(R.font.kingthings_petrock))

/**
 * A type scale built from a display face and a body face.
 *
 * [scale] exists because these faces disagree about how big a point is: Petrock
 * and IM Fell both run small and light for their size, and left at the same
 * numbers as Archivo the ingredient lines would quietly become unreadable.
 */
private fun typography(
    display: FontFamily,
    body: FontFamily,
    displayScale: Float = 1f,
    bodyScale: Float = 1f,
    displayWeight: FontWeight = FontWeight.SemiBold,
    titleTracking: Float = 0f,
): Typography {
    fun sp(v: Float) = (v * displayScale).sp
    fun bp(v: Float) = (v * bodyScale).sp
    val base = Typography()
    return Typography(
        headlineSmall = base.headlineSmall.copy(fontFamily = display, fontWeight = displayWeight, fontSize = sp(24f), letterSpacing = titleTracking.sp),
        titleLarge = base.titleLarge.copy(fontFamily = display, fontWeight = displayWeight, fontSize = sp(21f), letterSpacing = titleTracking.sp),
        titleMedium = base.titleMedium.copy(fontFamily = display, fontWeight = displayWeight, fontSize = sp(16f)),
        titleSmall = base.titleSmall.copy(fontFamily = display, fontWeight = displayWeight, fontSize = sp(14f)),
        bodyLarge = base.bodyLarge.copy(fontFamily = body, fontSize = bp(16f)),
        bodyMedium = base.bodyMedium.copy(fontFamily = body, fontSize = bp(14f)),
        bodySmall = base.bodySmall.copy(fontFamily = body, fontSize = bp(12.5f)),
        labelLarge = base.labelLarge.copy(fontFamily = body, fontWeight = FontWeight.Medium, fontSize = bp(14f)),
        labelMedium = base.labelMedium.copy(fontFamily = body, fontWeight = FontWeight.Medium, fontSize = bp(12f)),
        labelSmall = base.labelSmall.copy(fontFamily = body, fontWeight = FontWeight.Medium, fontSize = bp(11f)),
    )
}

/**
 * The typefaces a style uses.
 *
 * Internal rather than private because the Settings picker renders each option
 * in the fonts it is offering, which needs this without applying the theme.
 */
/** The bundled file for a face. */
private fun familyOf(face: TypeFace): FontFamily = when (face) {
    TypeFace.FRAUNCES -> Fraunces
    TypeFace.ARCHIVO -> Archivo
    TypeFace.IM_FELL -> ImFell
    TypeFace.GRENZE -> GrenzeGotisch
    TypeFace.CARDO -> Cardo
    TypeFace.GARAMOND -> EbGaramond
    TypeFace.PETROCK -> Petrock
}

/**
 * The type scale, from two independently chosen faces and a size.
 *
 * Headings scale by the display face's own factor and body text by the body
 * face's, so Petrock's eighteen per cent follows Petrock into whatever pairing
 * it lands in without dragging a grotesque up with it. The size setting
 * multiplies both, which is why Medium has to be exactly 1.
 */
internal fun typeFor(
    display: TypeFace = TypeFace.DEFAULT_DISPLAY,
    body: TypeFace = TypeFace.DEFAULT_BODY,
    textSize: TextSize = TextSize.DEFAULT,
): Typography = typography(
    display = familyOf(display),
    body = familyOf(body),
    displayScale = display.scale * textSize.factor,
    bodyScale = body.scale * textSize.factor,
    displayWeight = FontWeight(display.displayWeight),
    titleTracking = display.tracking,
)

// ---------------------------------------------------------------------------
// Paper
// ---------------------------------------------------------------------------

/**
 * One soft blotch of colour, placed in fractions of the screen.
 *
 * Parchment is uneven translucency, not a flat fill, and a handful of these
 * layered over a solid ground is enough to stop the paper styles looking like
 * a colour swap. Fractional so it works on any screen.
 */
@Immutable
data class PaperWash(
    val color: Color,
    val centerX: Float,
    val centerY: Float,
    val radius: Float,
)

/**
 * Everything a style needs that Material's own scheme has nowhere to put.
 */
@Immutable
data class DiaryPalette(
    val style: ColourTheme,
    val washes: List<PaperWash> = emptyList(),
    /** Hairline rules between rows. Material's outlineVariant is often too cold. */
    val rule: Color,

    /**
     * The wash behind whichever row is *live* — an open symptom episode.
     *
     * This is where Oblivion's selection glow goes on a phone. There is no
     * cursor to follow and no hover to catch, but the app does have a genuine
     * "this is the current thing, and it's waiting on you": a bloating episode
     * that started at 2am and is still running. That's what the highlight was
     * always saying, so it lands there rather than on a tap that lasts 200ms.
     */
    val liveTint: Color,

    /**
     * The edge beside a live row.
     *
     * Colour alone can't carry "this is still open" — it fails in sunlight and
     * for anyone colour-blind — so the tint always travels with a bar whose
     * presence is the signal and whose colour is only the decoration.
     */
    val liveEdge: Color,

    /**
     * Mild, moderate, severe, very severe — as a ramp, not four nice colours.
     *
     * This used to read off `primary` / `tertiary` / `error`, which was
     * convenient and wrong: those slots are also the accent, so they can't be
     * tuned for legibility as a series. Two of them landed at the same
     * luminance, which means the band only distinguished itself by hue, which
     * is the one cue a colour-blind reader doesn't get.
     *
     * So these climb in *brightness* — darker with severity on a light ground,
     * lighter on a dark one — and the band reads as intensity even in
     * greyscale.
     */
    val severity: List<Color>,
)

val LocalDiaryPalette = staticCompositionLocalOf {
    DiaryPalette(
        style = ColourTheme.HERBARIUM,
        rule = Color(0xFFCDD5C2),
        liveTint = Color(0x1F3E5C3A),
        liveEdge = Color(0xFF3E5C3A),
        severity = listOf(Color(0xFF7E8B76), Color(0xFF4A6B44), Color(0xFF7A3B52), Color(0xFF5E1616)),
    )
}

/** The washes as a brush stack, for a background that isn't flat. */
fun List<PaperWash>.brushes(size: Size): List<Brush> = map { wash ->
    Brush.radialGradient(
        colors = listOf(wash.color, Color.Transparent),
        center = Offset(size.width * wash.centerX, size.height * wash.centerY),
        radius = maxOf(size.width, size.height) * wash.radius,
    )
}

// ---------------------------------------------------------------------------
// The palettes — five styles, two modes each.
//
// Each palette carries its own four-step severity ramp, and every ramp has to
// climb in brightness rather than merely change hue — see DiaryPalette.severity
// for why.
//
// The trap, which caught three of these before the tests did: an amber or gold
// at "moderate" is *brighter* than a red at "severe", because yellow hues carry
// far more luminance than reds at the same apparent saturation. Anywhere gold
// wants to sit below red in a ramp it has to drop to bronze first.
// ---------------------------------------------------------------------------

internal data class Skin(val colors: ColorScheme, val palette: DiaryPalette)

// ---- Herbarium ------------------------------------------------------------

private fun herbariumLight() = Skin(
    colors = lightColorScheme(
        primary = Color(0xFF3E5C3A), onPrimary = Color(0xFFEDEFE6),
        primaryContainer = Color(0xFFD6DEC8), onPrimaryContainer = Color(0xFF1B2A18),
        secondary = Color(0xFF5A6B52), onSecondary = Color(0xFFEDEFE6),
        secondaryContainer = Color(0xFFDDE3D2), onSecondaryContainer = Color(0xFF22301F),
        tertiary = Color(0xFF7A3B52), onTertiary = Color(0xFFF4EEF0),
        tertiaryContainer = Color(0xFFE7D5DC), onTertiaryContainer = Color(0xFF3A1B26),
        error = Color(0xFF8C2F2F), onError = Color(0xFFF6EDEA),
        errorContainer = Color(0xFFEBD3CE), onErrorContainer = Color(0xFF3E1412),
        background = Color(0xFFEDEFE6), onBackground = Color(0xFF22301F),
        surface = Color(0xFFEDEFE6), onSurface = Color(0xFF22301F),
        surfaceVariant = Color(0xFFE0E4D5), onSurfaceVariant = Color(0xFF67765F),
        outline = Color(0xFF9AA791), outlineVariant = Color(0xFFCDD5C2),
    ),
    palette = DiaryPalette(
        ColourTheme.HERBARIUM,
        rule = Color(0xFFCDD5C2),
        liveTint = Color(0x1F3E5C3A),
        liveEdge = Color(0xFF3E5C3A),
        severity = listOf(Color(0xFF7E8B76), Color(0xFF4A6B44), Color(0xFF7A3B52), Color(0xFF5E1616)),
    ),
)

private fun herbariumDark() = Skin(
    colors = darkColorScheme(
        primary = Color(0xFF9DB394), onPrimary = Color(0xFF1B2417),
        primaryContainer = Color(0xFF2E3C2A), onPrimaryContainer = Color(0xFFCEDCC4),
        secondary = Color(0xFF8A9683), onSecondary = Color(0xFF1B2417),
        secondaryContainer = Color(0xFF283121), onSecondaryContainer = Color(0xFFDFE5D6),
        tertiary = Color(0xFFC98BA3), onTertiary = Color(0xFF2A1620),
        tertiaryContainer = Color(0xFF44252F), onTertiaryContainer = Color(0xFFEDCCD8),
        error = Color(0xFFD98177), onError = Color(0xFF2A1310),
        errorContainer = Color(0xFF48201C), onErrorContainer = Color(0xFFF2C9C2),
        background = Color(0xFF171B15), onBackground = Color(0xFFDFE5D6),
        surface = Color(0xFF171B15), onSurface = Color(0xFFDFE5D6),
        surfaceVariant = Color(0xFF232A20), onSurfaceVariant = Color(0xFF8A9683),
        outline = Color(0xFF66705F), outlineVariant = Color(0xFF2C332A),
    ),
    palette = DiaryPalette(
        ColourTheme.HERBARIUM,
        washes = listOf(
            PaperWash(Color(0x1F4E6647), 0.2f, 0.1f, 0.55f),
            PaperWash(Color(0x2E101409), 0.8f, 0.7f, 0.6f),
        ),
        rule = Color(0x1FDFE5D6),
        liveTint = Color(0x2E9DB394),
        liveEdge = Color(0xFF9DB394),
        severity = listOf(Color(0xFF5E6858), Color(0xFF86A07C), Color(0xFFD48FA8), Color(0xFFF2A79A)),
    ),
)

// ---- Vellum & Quill -------------------------------------------------------

private fun vellumLight() = Skin(
    colors = lightColorScheme(
        primary = Color(0xFF4A5A7A), onPrimary = Color(0xFFEFE6CE),
        primaryContainer = Color(0xFFD9CFB2), onPrimaryContainer = Color(0xFF23304A),
        secondary = Color(0xFF7E6A4C), onSecondary = Color(0xFFEFE6CE),
        secondaryContainer = Color(0xFFDCCFAE), onSecondaryContainer = Color(0xFF3A2C1C),
        tertiary = Color(0xFF9A5A22), onTertiary = Color(0xFFF3E8D2),
        tertiaryContainer = Color(0xFFE2CDA6), onTertiaryContainer = Color(0xFF43260C),
        error = Color(0xFF96301F), onError = Color(0xFFF3E8D2),
        errorContainer = Color(0xFFE0C6B2), onErrorContainer = Color(0xFF3E120A),
        background = Color(0xFFE6D8B8), onBackground = Color(0xFF3A2C1C),
        surface = Color(0xFFE6D8B8), onSurface = Color(0xFF3A2C1C),
        surfaceVariant = Color(0xFFDCCDAA), onSurfaceVariant = Color(0xFF7E6A4C),
        outline = Color(0xFFA8916C), outlineVariant = Color(0xFFCFBE99),
    ),
    palette = DiaryPalette(
        ColourTheme.VELLUM,
        washes = listOf(
            PaperWash(Color(0x66FFF9E2), 0.14f, 0.08f, 0.6f),
            PaperWash(Color(0x33A88A58), 0.86f, 0.22f, 0.5f),
            PaperWash(Color(0x4DFFF7DE), 0.72f, 0.62f, 0.55f),
            PaperWash(Color(0x2B96784A), 0.22f, 0.48f, 0.45f),
            PaperWash(Color(0x2696784A), 0.08f, 0.88f, 0.4f),
        ),
        rule = Color(0x4D3A2C1C),
        liveTint = Color(0x2296301F),
        liveEdge = Color(0xFF96301F),
        severity = listOf(Color(0xFFA8916C), Color(0xFFB4653A), Color(0xFF96301F), Color(0xFF5E1408)),
    ),
)

/** Hide by candlelight: the same skin, the same two inks, no daylight on it. */
private fun vellumDark() = Skin(
    colors = darkColorScheme(
        primary = Color(0xFF8FA0C4), onPrimary = Color(0xFF1B2334),
        primaryContainer = Color(0xFF2C3750), onPrimaryContainer = Color(0xFFCBD5E8),
        secondary = Color(0xFFA8916C), onSecondary = Color(0xFF241C12),
        secondaryContainer = Color(0xFF33291B), onSecondaryContainer = Color(0xFFE4D5B4),
        tertiary = Color(0xFFD1934F), onTertiary = Color(0xFF2A1B08),
        tertiaryContainer = Color(0xFF432C10), onTertiaryContainer = Color(0xFFF0D3A9),
        error = Color(0xFFD4635C), onError = Color(0xFF2C110D),
        errorContainer = Color(0xFF4A1C17), onErrorContainer = Color(0xFFF3C2BC),
        background = Color(0xFF241C12), onBackground = Color(0xFFE4D5B4),
        surface = Color(0xFF241C12), onSurface = Color(0xFFE4D5B4),
        surfaceVariant = Color(0xFF302518), onSurfaceVariant = Color(0xFF9A8663),
        outline = Color(0xFF75643F), outlineVariant = Color(0xFF3A2E1E),
    ),
    palette = DiaryPalette(
        ColourTheme.VELLUM,
        washes = listOf(
            PaperWash(Color(0x38614B2A), 0.14f, 0.08f, 0.6f),
            PaperWash(Color(0x4D18110A), 0.86f, 0.26f, 0.55f),
            PaperWash(Color(0x2E70552E), 0.7f, 0.66f, 0.5f),
            PaperWash(Color(0x4212100A), 0.24f, 0.5f, 0.45f),
        ),
        rule = Color(0x24E4D5B4),
        liveTint = Color(0x33D4635C),
        liveEdge = Color(0xFFD4635C),
        severity = listOf(Color(0xFF75643F), Color(0xFFA0703C), Color(0xFFD4635C), Color(0xFFF09A94)),
    ),
)

// ---- Grimoire -------------------------------------------------------------

/**
 * The daylight grimoire: an aged, foxed ivory page.
 *
 * Gold does not survive being moved onto a light ground — it stops being metal
 * and turns into yellow — so it goes to burnished bronze, which is what gold
 * leaf actually looks like on paper in the daytime.
 */
private fun grimoireLight() = Skin(
    colors = lightColorScheme(
        primary = Color(0xFF8A6420), onPrimary = Color(0xFFF0E9D9),
        primaryContainer = Color(0xFFE0D0A8), onPrimaryContainer = Color(0xFF3A2A08),
        secondary = Color(0xFF7A6B54), onSecondary = Color(0xFFF0E9D9),
        secondaryContainer = Color(0xFFDED5C0), onSecondaryContainer = Color(0xFF241E16),
        tertiary = Color(0xFF9A4A22), onTertiary = Color(0xFFF4EDDD),
        tertiaryContainer = Color(0xFFE4C9B0), onTertiaryContainer = Color(0xFF41190A),
        error = Color(0xFF8E2A2C), onError = Color(0xFFF4EDDD),
        errorContainer = Color(0xFFE2C4C0), onErrorContainer = Color(0xFF3C1011),
        background = Color(0xFFE8E0CE), onBackground = Color(0xFF241E16),
        surface = Color(0xFFE8E0CE), onSurface = Color(0xFF241E16),
        surfaceVariant = Color(0xFFDCD3BE), onSurfaceVariant = Color(0xFF7A6B54),
        outline = Color(0xFFA3957B), outlineVariant = Color(0xFFCCC2A9),
    ),
    palette = DiaryPalette(
        ColourTheme.GRIMOIRE,
        washes = listOf(
            PaperWash(Color(0x4DFFF8E4), 0.22f, 0.1f, 0.55f),
            PaperWash(Color(0x2E7A5A2E), 0.8f, 0.42f, 0.4f),
            PaperWash(Color(0x247A5A2E), 0.18f, 0.72f, 0.35f),
            PaperWash(Color(0x2E9C8452), 0.78f, 0.9f, 0.5f),
        ),
        rule = Color(0x2E241E16),
        liveTint = Color(0x2E8A6420),
        liveEdge = Color(0xFF8A6420),
        severity = listOf(Color(0xFFA3957B), Color(0xFF9A6A22), Color(0xFF8E2A2C), Color(0xFF521015)),
    ),
)

private fun grimoireDark() = Skin(
    colors = darkColorScheme(
        primary = Color(0xFFC89B4A), onPrimary = Color(0xFF1C1814),
        primaryContainer = Color(0xFF3A2E18), onPrimaryContainer = Color(0xFFE8CE93),
        secondary = Color(0xFF8E8068), onSecondary = Color(0xFF1C1814),
        secondaryContainer = Color(0xFF2C251B), onSecondaryContainer = Color(0xFFD9CBA8),
        tertiary = Color(0xFFC4612F), onTertiary = Color(0xFF1C1814),
        tertiaryContainer = Color(0xFF3E2211), onTertiaryContainer = Color(0xFFE9B48C),
        error = Color(0xFFC24D4F), onError = Color(0xFF2A0F11),
        errorContainer = Color(0xFF44161A), onErrorContainer = Color(0xFFE9B0AE),
        background = Color(0xFF1C1814), onBackground = Color(0xFFD9CBA8),
        surface = Color(0xFF1C1814), onSurface = Color(0xFFD9CBA8),
        surfaceVariant = Color(0xFF2A231B), onSurfaceVariant = Color(0xFF8E8068),
        outline = Color(0xFF6B5F4A), outlineVariant = Color(0xFF3A322A),
    ),
    palette = DiaryPalette(
        ColourTheme.GRIMOIRE,
        washes = listOf(
            PaperWash(Color(0x5C58462C), 0.18f, 0.1f, 0.55f),
            PaperWash(Color(0x805C4428), 0.84f, 0.34f, 0.5f),
            PaperWash(Color(0x42604A2C), 0.66f, 0.8f, 0.55f),
            PaperWash(Color(0x80281E14), 0.3f, 0.6f, 0.45f),
        ),
        rule = Color(0x24D9CBA8),
        liveTint = Color(0x2EC89B4A),
        liveEdge = Color(0xFFC89B4A),
        severity = listOf(Color(0xFF6B5F4A), Color(0xFF8A6528), Color(0xFFC4612F), Color(0xFFE06A6C)),
    ),
)

// ---- Herbal ---------------------------------------------------------------

private fun herbalLight() = Skin(
    colors = lightColorScheme(
        primary = Color(0xFF4A5D3A), onPrimary = Color(0xFFEFE4C9),
        primaryContainer = Color(0xFFD9DBBC), onPrimaryContainer = Color(0xFF232E19),
        secondary = Color(0xFF7C6E52), onSecondary = Color(0xFFEFE4C9),
        secondaryContainer = Color(0xFFE2D6B8), onSecondaryContainer = Color(0xFF2B2419),
        tertiary = Color(0xFF9C5A22), onTertiary = Color(0xFFF5EBD5),
        tertiaryContainer = Color(0xFFE6D0AC), onTertiaryContainer = Color(0xFF43260C),
        error = Color(0xFF8B3A2B), onError = Color(0xFFF5EBD5),
        errorContainer = Color(0xFFE4CBBB), onErrorContainer = Color(0xFF3A150E),
        background = Color(0xFFEFE4C9), onBackground = Color(0xFF2B2419),
        surface = Color(0xFFEFE4C9), onSurface = Color(0xFF2B2419),
        surfaceVariant = Color(0xFFE4D8BB), onSurfaceVariant = Color(0xFF7C6E52),
        outline = Color(0xFFA6997A), outlineVariant = Color(0xFFD5C8A8),
    ),
    palette = DiaryPalette(
        ColourTheme.HERBAL,
        washes = listOf(
            PaperWash(Color(0x59FFFAE8), 0.2f, 0.12f, 0.55f),
            PaperWash(Color(0x268B5F34), 0.78f, 0.4f, 0.35f),
            PaperWash(Color(0x1F8B5F34), 0.21f, 0.68f, 0.3f),
            PaperWash(Color(0x2BA08456), 0.8f, 0.86f, 0.5f),
        ),
        rule = Color(0x262B2419),
        liveTint = Color(0x1F4A5D3A),
        liveEdge = Color(0xFF4A5D3A),
        severity = listOf(Color(0xFFA6997A), Color(0xFFA8722E), Color(0xFF8B3A2B), Color(0xFF521A10)),
    ),
)

/** The same book, read by lamplight. Cooler and greener than Vellum's dark. */
private fun herbalDark() = Skin(
    colors = darkColorScheme(
        primary = Color(0xFF96AC7E), onPrimary = Color(0xFF1A2113),
        primaryContainer = Color(0xFF2C3722), onPrimaryContainer = Color(0xFFC9D6B4),
        secondary = Color(0xFF948872), onSecondary = Color(0xFF1A1811),
        secondaryContainer = Color(0xFF2A2619), onSecondaryContainer = Color(0xFFE9DEC2),
        tertiary = Color(0xFFC98F55), onTertiary = Color(0xFF291A09),
        tertiaryContainer = Color(0xFF412A11), onTertiaryContainer = Color(0xFFEED1A8),
        error = Color(0xFFC97A5F), onError = Color(0xFF2A1209),
        errorContainer = Color(0xFF451E12), onErrorContainer = Color(0xFFF0C6B4),
        background = Color(0xFF1A1811), onBackground = Color(0xFFE9DEC2),
        surface = Color(0xFF1A1811), onSurface = Color(0xFFE9DEC2),
        surfaceVariant = Color(0xFF262218), onSurfaceVariant = Color(0xFF948872),
        outline = Color(0xFF6E6650), outlineVariant = Color(0xFF322D20),
    ),
    palette = DiaryPalette(
        ColourTheme.HERBAL,
        washes = listOf(
            PaperWash(Color(0x2E4A5232), 0.2f, 0.12f, 0.55f),
            PaperWash(Color(0x3D110F08), 0.78f, 0.44f, 0.45f),
            PaperWash(Color(0x24564A2C), 0.24f, 0.74f, 0.4f),
        ),
        rule = Color(0x24E9DEC2),
        liveTint = Color(0x2E96AC7E),
        liveEdge = Color(0xFF96AC7E),
        severity = listOf(Color(0xFF6E6650), Color(0xFF6E8358), Color(0xFFC08540), Color(0xFFEC9C86)),
    ),
)

// ---- Cyrodiil -------------------------------------------------------------

private fun cyrodiilLight() = Skin(
    colors = lightColorScheme(
        primary = Color(0xFF8A5F1A), onPrimary = Color(0xFFF0E4C6),
        primaryContainer = Color(0xFFE8C57E), onPrimaryContainer = Color(0xFF3A2708),
        secondary = Color(0xFF6E5B41), onSecondary = Color(0xFFF0E4C6),
        secondaryContainer = Color(0xFFCFBC93), onSecondaryContainer = Color(0xFF33261A),
        tertiary = Color(0xFFA8562A), onTertiary = Color(0xFFF3E7CB),
        tertiaryContainer = Color(0xFFE0C193), onTertiaryContainer = Color(0xFF43200B),
        error = Color(0xFF8E2B22), onError = Color(0xFFF3E7CB),
        errorContainer = Color(0xFFDCBBA5), onErrorContainer = Color(0xFF3C0F0A),
        background = Color(0xFFD8C79F), onBackground = Color(0xFF33261A),
        surface = Color(0xFFD8C79F), onSurface = Color(0xFF33261A),
        surfaceVariant = Color(0xFFCDBB92), onSurfaceVariant = Color(0xFF6E5B41),
        outline = Color(0xFF9C8760), outlineVariant = Color(0xFFC0AC83),
    ),
    palette = DiaryPalette(
        ColourTheme.CYRODIIL,
        washes = listOf(
            PaperWash(Color(0x57FFF6D6), 0.5f, 0.02f, 0.55f),
            PaperWash(Color(0x29785C32), 0.5f, 0.98f, 0.6f),
            PaperWash(Color(0x24543C1C), 0.1f, 0.5f, 0.4f),
            PaperWash(Color(0x24543C1C), 0.9f, 0.5f, 0.4f),
        ),
        rule = Color(0x4D33261A),
        // The amber wash and gold edge, straight off the menu.
        liveTint = Color(0x4DE0A93E),
        liveEdge = Color(0xFF8A5F1A),
        severity = listOf(Color(0xFF8F7A55), Color(0xFF8A5A18), Color(0xFF8E2B22), Color(0xFF5A1410)),
    ),
)

/**
 * Oblivion's other screen.
 *
 * The main menu and loading screens are dark stone with gold lettering, so the
 * dark side of this style isn't invented — it's the same game. Cooler and
 * greyer than Grimoire's charred brown, which is what keeps the two apart on a
 * dark ground where the typefaces are doing most of the work.
 */
private fun cyrodiilDark() = Skin(
    colors = darkColorScheme(
        primary = Color(0xFFE8C57E), onPrimary = Color(0xFF241E12),
        primaryContainer = Color(0xFF3E3520), onPrimaryContainer = Color(0xFFF2DCAB),
        secondary = Color(0xFFA89A80), onSecondary = Color(0xFF1C1B18),
        secondaryContainer = Color(0xFF2C2A24), onSecondaryContainer = Color(0xFFDCCFA9),
        tertiary = Color(0xFFCE7C4A), onTertiary = Color(0xFF2A1708),
        tertiaryContainer = Color(0xFF43260F), onTertiaryContainer = Color(0xFFEFC7A3),
        error = Color(0xFFC4544A), onError = Color(0xFF2A0F0C),
        errorContainer = Color(0xFF471A15), onErrorContainer = Color(0xFFF0BDB6),
        background = Color(0xFF1C1B18), onBackground = Color(0xFFDCCFA9),
        surface = Color(0xFF1C1B18), onSurface = Color(0xFFDCCFA9),
        surfaceVariant = Color(0xFF282621), onSurfaceVariant = Color(0xFF9A9078),
        outline = Color(0xFF75705E), outlineVariant = Color(0xFF34322B),
    ),
    palette = DiaryPalette(
        ColourTheme.CYRODIIL,
        washes = listOf(
            PaperWash(Color(0x2E4E4634), 0.5f, 0.02f, 0.55f),
            PaperWash(Color(0x4D100F0C), 0.5f, 0.98f, 0.6f),
            PaperWash(Color(0x3D141310), 0.08f, 0.5f, 0.4f),
            PaperWash(Color(0x3D141310), 0.92f, 0.5f, 0.4f),
        ),
        rule = Color(0x2EDCCFA9),
        liveTint = Color(0x3DE0A93E),
        liveEdge = Color(0xFFE8C57E),
        severity = listOf(Color(0xFF75705E), Color(0xFF9C7C3A), Color(0xFFCE7C4A), Color(0xFFF08A7E)),
    ),
)

// ---------------------------------------------------------------------------

internal fun skinFor(
    theme: ColourTheme,
    dark: Boolean,
    custom: CustomPalette = CustomPalette(),
): Skin = when (theme) {
    ColourTheme.HERBARIUM -> if (dark) herbariumDark() else herbariumLight()
    ColourTheme.VELLUM -> if (dark) vellumDark() else vellumLight()
    ColourTheme.GRIMOIRE -> if (dark) grimoireDark() else grimoireLight()
    ColourTheme.HERBAL -> if (dark) herbalDark() else herbalLight()
    ColourTheme.CYRODIIL -> if (dark) cyrodiilDark() else cyrodiilLight()
    // No light or dark twin: you chose the paper, so the paper decides.
    ColourTheme.CUSTOM -> custom.toSkin()
}

/** The edge drawn beside a live row, for the palette tests. */
fun liveEdgeOf(theme: ColourTheme, dark: Boolean, custom: CustomPalette = CustomPalette()): Color =
    skinFor(theme, dark, custom).palette.liveEdge

/** The mild → very severe ramp, for the palette tests. */
fun severityRampOf(theme: ColourTheme, dark: Boolean, custom: CustomPalette = CustomPalette()): List<Color> =
    skinFor(theme, dark, custom).palette.severity

/** Ground, ink, then three accents. For the picker's previews. */
fun previewColors(theme: ColourTheme, dark: Boolean, custom: CustomPalette = CustomPalette()): List<Color> =
    with(skinFor(theme, dark, custom).colors) {
        listOf(background, onBackground, primary, tertiary, error)
    }

@Composable
fun FoodDiaryTheme(
    theme: ColourTheme = ColourTheme.DEFAULT,
    mode: ThemeMode = ThemeMode.SYSTEM,
    displayFace: TypeFace = TypeFace.DEFAULT_DISPLAY,
    bodyFace: TypeFace = TypeFace.DEFAULT_BODY,
    textSize: TextSize = TextSize.DEFAULT,
    custom: CustomPalette = CustomPalette(),
    content: @Composable () -> Unit,
) {
    val dark = when {
        // A custom palette has one ground and no twin, so the switch above it
        // is hidden and its answer ignored rather than quietly half-applied.
        theme == ColourTheme.CUSTOM -> custom.isDark
        mode == ThemeMode.LIGHT -> false
        mode == ThemeMode.DARK -> true
        else -> isSystemInDarkTheme()
    }
    val skin = skinFor(theme, dark, custom)
    CompositionLocalProvider(LocalDiaryPalette provides skin.palette) {
        MaterialTheme(
            colorScheme = skin.colors,
            typography = typeFor(displayFace, bodyFace, textSize),
            content = content,
        )
    }
}
