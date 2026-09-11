package dev.mahourigan.fooddiary.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The ten built-in palettes, checked as sets rather than as colours.
 *
 * Hand-picked colour has no compiler, and five styles times two modes is fifty
 * scheme entries — far too many to eyeball whenever one changes. These assert
 * the two properties that actually matter: you can read the text, and severity
 * still reads as a climb rather than four pleasant colours.
 */
class PaletteTest {

    private val modes = listOf(false, true)

    /** WCAG relative-luminance contrast ratio. */
    private fun contrast(a: Color, b: Color): Double {
        val la = a.luminance() + 0.05
        val lb = b.luminance() + 0.05
        return if (la > lb) la / lb else lb / la
    }

    // The built-in five only. Custom's colours come out of the diary file, so
    // there is nothing fixed here to assert about them — CustomPaletteTest
    // checks the *derivation* instead, which is the part this app owns.
    private fun each(check: (ColourTheme, Boolean, List<Color>) -> Unit) {
        ColourTheme.builtIn.forEach { style ->
            modes.forEach { dark -> check(style, dark, previewColors(style, dark)) }
        }
    }

    private fun label(style: ColourTheme, dark: Boolean) =
        "$style ${if (dark) "dark" else "light"}"

    @Test
    fun `body text is readable on its own ground`() {
        each { style, dark, colors ->
            val ratio = contrast(colors[0], colors[1])
            assertTrue(
                "${label(style, dark)}: ink on ground is only ${"%.1f".format(ratio)}:1",
                ratio >= 7.0,
            )
        }
    }

    @Test
    fun `every severity colour stands off the ground`() {
        // A severity mark is a 4dp bar, so it carries on its own without
        // text-sized help. 3:1 is the WCAG floor for non-text.
        //
        // Mild is held to 2:1 instead, deliberately. "Barely anything" is
        // supposed to be quiet, and a mild band shouting as loudly as a very
        // severe one would make the ramp useless in the other direction.
        each { style, dark, colors ->
            severityRampOf(style, dark).forEachIndexed { i, severity ->
                val floor = if (i == 0) 2.0 else 3.0
                val ratio = contrast(colors[0], severity)
                assertTrue(
                    "${label(style, dark)}: severity $i is only ${"%.1f".format(ratio)}:1",
                    ratio >= floor,
                )
            }
        }
    }

    @Test
    fun `severity climbs rather than wandering`() {
        // moderate → severe → very severe. Not a hue rule, a separation rule:
        // two adjacent steps that look alike make the band meaningless.
        each { style, dark, colors ->
            severityRampOf(style, dark).zipWithNext().forEach { (dimmer, brighter) ->
                // Monotonic, not merely different: away from the ground with
                // every step, so the band reads as intensity in greyscale.
                val away = { c: Color -> contrast(colors[0], c) }
                assertTrue(
                    "${label(style, dark)}: severity does not climb ($dimmer then $brighter)",
                    away(brighter) > away(dimmer) * 1.1,
                )
            }
        }
    }

    @Test
    fun `a dark mode is actually darker than its light twin`() {
        ColourTheme.builtIn.forEach { style ->
            val light = previewColors(style, dark = false)[0]
            val dark = previewColors(style, dark = true)[0]
            assertTrue("$style: dark ground is not darker", dark.luminance() < light.luminance())
        }
    }

    @Test
    fun `the live-row edge stands off its own ground`() {
        // The edge is the part that has to survive sunlight and colour
        // blindness, so it can't be a whisper against the paper.
        ColourTheme.builtIn.forEach { style ->
            modes.forEach { dark ->
                val ground = previewColors(style, dark)[0]
                assertTrue(
                    "${label(style, dark)}: live edge is too faint",
                    contrast(ground, liveEdgeOf(style, dark)) >= 2.0,
                )
            }
        }
    }
}
