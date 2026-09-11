package dev.mahourigan.fooddiary.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Three swatches in, a whole scheme out.
 *
 * The built-in ten are hand-picked and checked as fixed sets by [PaletteTest].
 * These colours are not: they come from whatever someone drags a slider to, so
 * what can be tested is the *contract*, and the contract has two halves.
 *
 * **Always true, whatever you pick.** The severity ramp climbs, every band
 * stands off the paper, and the live-row edge is visible. Those three carry
 * meaning rather than mood — a severity band that stops climbing has stopped
 * saying anything — so they are relit against the chosen paper rather than
 * taken as given.
 *
 * **Yours to get wrong.** Text contrast. It is measured and reported and then
 * applied regardless, which is what was asked for, so the tests here check that
 * the warning appears rather than that the situation cannot arise.
 */
class CustomPaletteTest {

    /** A spread of grounds, inks and accents, including several bad ideas. */
    private val palettes = listOf(
        "the default" to CustomPalette(),
        "near-black paper" to CustomPalette(ground = 0xFF101014, ink = 0xFFE8E6DF, accent = 0xFF6FA8DC),
        "hot pink accent" to CustomPalette(ground = 0xFFFFFFFF, ink = 0xFF000000, accent = 0xFFFF1493),
        "mid grey paper" to CustomPalette(ground = 0xFF808080, ink = 0xFF111111, accent = 0xFFFFD700),
        "a red accent" to CustomPalette(ground = 0xFFFBF3E4, ink = 0xFF2B2118, accent = 0xFFB03030),
        "an unsaturated everything" to CustomPalette(ground = 0xFFEEEEEE, ink = 0xFF444444, accent = 0xFF777777),
        "very dark paper, dim ink" to CustomPalette(ground = 0xFF07070A, ink = 0xFF3A3A44, accent = 0xFF224466),
        "yellow paper" to CustomPalette(ground = 0xFFFFF8C0, ink = 0xFF3A3200, accent = 0xFFCC8800),
    )

    private fun each(check: (String, CustomPalette) -> Unit) =
        palettes.forEach { (name, palette) -> check(name, palette) }

    // ---- The guarantees -----------------------------------------------------

    @Test
    fun `severity always climbs away from the paper`() {
        // The lesson that cost four bugs in the hand-picked palettes: an amber
        // at "moderate" is brighter than a red at "severe", because yellow
        // hues carry far more luminance. Choosing contrast first and hue
        // second makes the dip impossible rather than merely unlikely.
        each { name, palette ->
            val ground = palette.groundColor
            palette.ramp().zipWithNext().forEach { (dimmer, brighter) ->
                val a = contrastOf(ground, dimmer)
                val b = contrastOf(ground, brighter)
                assertTrue("$name: severity stalls at $a:1 then $b:1", b > a * 1.1f)
            }
        }
    }

    @Test
    fun `every severity band stands off the paper`() {
        // Same floors as the built-in ten: 2:1 for mild, which is meant to be
        // quiet, and 3:1 for the rest, WCAG's floor for a non-text shape.
        each { name, palette ->
            palette.ramp().forEachIndexed { i, band ->
                val floor = if (i == 0) 2.0f else 3.0f
                val ratio = contrastOf(palette.groundColor, band)
                assertTrue("$name: severity $i is only $ratio:1", ratio >= floor)
            }
        }
    }

    @Test
    fun `the live edge survives whatever accent you choose`() {
        // Deliberately relit rather than used as picked. The edge is what says
        // an episode is still open, and a pale accent on pale paper loses it.
        each { name, palette ->
            val ratio = contrastOf(palette.groundColor, palette.toSkin().palette.liveEdge)
            assertTrue("$name: live edge is only $ratio:1", ratio >= 2.0f)
        }
    }

    @Test
    fun `an accent that is nearly the paper still gets a usable edge`() {
        val invisible = CustomPalette(ground = 0xFFEDEFE6, ink = 0xFF22301F, accent = 0xFFEBEDE4)
        assertTrue(
            contrastOf(invisible.groundColor, invisible.toSkin().palette.liveEdge) >= 2.0f,
        )
    }

    @Test
    fun `error stays red even when the accent is green`() {
        // Borrowing the accent for error would let a green-accented diary make
        // every warning look like a confirmation.
        val green = CustomPalette(ground = 0xFFEDEFE6, ink = 0xFF22301F, accent = 0xFF2E7D32)
        val error = green.toSkin().colors.error
        assertTrue("error is not reddest in red: $error", error.red > error.green && error.red > error.blue)
    }

    @Test
    fun `text always lands on a background it can be seen against`() {
        // Not the user's ink on the user's paper — that one is their call —
        // but the derived pairs, which nobody chose and so must be right.
        each { name, palette ->
            val c = palette.toSkin().colors
            listOf(
                "onPrimary" to (c.primary to c.onPrimary),
                "onPrimaryContainer" to (c.primaryContainer to c.onPrimaryContainer),
                "onSecondaryContainer" to (c.secondaryContainer to c.onSecondaryContainer),
                "onError" to (c.error to c.onError),
            ).forEach { (role, pair) ->
                val ratio = contrastOf(pair.first, pair.second)
                assertTrue("$name: $role is only $ratio:1", ratio >= 3.0f)
            }
        }
    }

    // ---- What is left to the person picking --------------------------------

    @Test
    fun `unreadable ink is reported rather than corrected`() {
        val faint = CustomPalette(ground = 0xFFEDEFE6, ink = 0xFFB8BFB0, accent = 0xFF7A3B52)
        assertTrue(faint.warnings().any { it.what.startsWith("Ink on paper") })
        // Still applied exactly as chosen. Reporting is the whole intervention.
        assertEquals(Color(0xFFB8BFB0), faint.toSkin().colors.onSurface)
    }

    @Test
    fun `a faint accent is reported too`() {
        val faint = CustomPalette(ground = 0xFFEDEFE6, ink = 0xFF22301F, accent = 0xFFE2E6DA)
        assertTrue(faint.warnings().any { it.what.startsWith("Accent on paper") })
    }

    @Test
    fun `a sound set of swatches has nothing to say about it`() {
        assertTrue(CustomPalette().warnings().isEmpty())
    }

    @Test
    fun `the default swatches meet the same bar as the built-in ten`() {
        // Custom starts on Herbarium's three, so a fresh custom theme is not a
        // downgrade from what it replaced.
        val d = CustomPalette()
        assertTrue(contrastOf(d.groundColor, d.inkColor) >= 7.0f)
    }

    // ---- Light and dark -----------------------------------------------------

    @Test
    fun `the paper alone decides whether this is a dark theme`() {
        assertFalse(CustomPalette(ground = 0xFFEDEFE6).isDark)
        assertTrue(CustomPalette(ground = 0xFF101014).isDark)
    }

    @Test
    fun `the mode switch cannot override a custom ground`() {
        // FoodDiaryTheme ignores ThemeMode for Custom; this pins the same rule
        // one level down, where the colours are actually chosen.
        val dark = CustomPalette(ground = 0xFF101014, ink = 0xFFE8E6DF, accent = 0xFF6FA8DC)
        listOf(false, true).forEach { asked ->
            val ground = skinFor(ColourTheme.CUSTOM, dark = asked, custom = dark).colors.background
            assertTrue("asked dark=$asked and got $ground", ground.luminance() < 0.1f)
        }
    }

    // ---- Slider round trip --------------------------------------------------

    @Test
    fun `a colour survives the three sliders`() {
        listOf(0xFFEDEFE6L, 0xFF22301FL, 0xFF7A3B52L, 0xFFFF1493L, 0xFF000000L, 0xFFFFFFFFL)
            .forEach { original ->
                val (h, s, l) = toHsl(Color(original)).let { Triple(it[0], it[1], it[2]) }
                val back = fromHsl(h, s, l)
                // Within one step of 8-bit, which is all HSL can promise.
                listOf(16, 8, 0).forEach { shift ->
                    val a = (original shr shift) and 0xFF
                    val b = (back shr shift) and 0xFF
                    assertTrue("channel drifted from $a to $b", kotlin.math.abs(a - b) <= 1)
                }
            }
    }

    private fun CustomPalette.ramp() = toSkin().palette.severity
}
