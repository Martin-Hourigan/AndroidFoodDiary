package dev.mahourigan.fooddiary.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The figure the calorie box arrives pre-filled with.
 *
 * It has to be the *same* number the totals show, or the box would sit there
 * reading 4900 under a day total reading 4911 and you'd have no idea which the
 * app believed.
 */
class EnergyDefaultTest {

    @Test
    fun `an exact total pre-fills exactly`() {
        // 1000 kcal is exactly 4184 kJ, so neither side sits on a rounding
        // boundary and the test is asserting the conversion, not my arithmetic.
        val exact = Energy(kcal = 1000.0, stated = false, assumed = false, unknownItems = 0)
        assertEquals(1000L, exact.amountIn(EnergyUnit.KCAL))
        assertEquals(4184L, exact.amountIn(EnergyUnit.KILOJOULE))
    }

    @Test
    fun `an estimate pre-fills at the rounding it is shown at`() {
        val guess = Energy(kcal = 497.0, assumed = true)
        assertEquals(500L, guess.amountIn(EnergyUnit.KCAL))
        assertEquals(2100L, guess.amountIn(EnergyUnit.KILOJOULE))
    }

    @Test
    fun `the box and the label can never disagree`() {
        // describe() is built on amountIn(), so this is structural rather than
        // a coincidence worth re-checking by eye.
        listOf(
            Energy(kcal = 1174.0),
            Energy(kcal = 497.0, assumed = true),
            Energy(kcal = 812.5, stated = true),
        ).forEach { energy ->
            EnergyUnit.entries.forEach { unit ->
                val shown = energy.describe(unit)
                val boxed = energy.amountIn(unit).toString()
                assertEquals(
                    "$shown should contain $boxed",
                    true,
                    shown.contains(boxed),
                )
            }
        }
    }

    @Test
    fun `nothing to work it out from pre-fills nothing`() {
        assertNull(Energy(kcal = null, unknownItems = 3).amountIn(EnergyUnit.KCAL))
    }
}
