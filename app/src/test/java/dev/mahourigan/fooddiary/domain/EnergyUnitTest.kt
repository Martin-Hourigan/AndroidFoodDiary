package dev.mahourigan.fooddiary.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Showing energy in kilojoules.
 *
 * A display choice, so the only things that matter are that it round-trips —
 * switching units and back must not quietly rewrite a figure you typed — and
 * that an estimate still reads as an estimate.
 */
class EnergyUnitTest {

    @Test
    fun `a typed figure survives a round trip`() {
        val unit = EnergyUnit.KILOJOULE
        val typed = 2100.0
        assertEquals(typed, unit.from(unit.toKcal(typed)), 0.0001)
    }

    @Test
    fun `kcal is the stored unit and passes through untouched`() {
        assertEquals(500.0, EnergyUnit.KCAL.from(500.0), 0.0)
        assertEquals(500.0, EnergyUnit.KCAL.toKcal(500.0), 0.0)
    }

    @Test
    fun `an estimate stays visibly an estimate in either unit`() {
        val estimate = Energy(kcal = 497.0, assumed = true)

        assertTrue(estimate.describe(EnergyUnit.KCAL).startsWith("~"))
        assertTrue(estimate.describe(EnergyUnit.KILOJOULE).startsWith("~"))
        assertTrue(estimate.describe(EnergyUnit.KILOJOULE).endsWith("kJ"))
    }

    @Test
    fun `a figure you gave is not rounded away`() {
        val exact = Energy(kcal = 500.0, stated = true)
        assertEquals("500 kcal", exact.describe(EnergyUnit.KCAL))
        assertEquals("2092 kJ", exact.describe(EnergyUnit.KILOJOULE))
    }

    @Test
    fun `an estimate is rounded coarsely enough to not look precise`() {
        // ~2090 kJ would imply a precision the estimate doesn't have; the step
        // widens with the unit so both read as equally rough.
        val estimate = Energy(kcal = 500.0, assumed = true)
        assertEquals("~500 kcal", estimate.describe(EnergyUnit.KCAL))
        assertEquals("~2100 kJ", estimate.describe(EnergyUnit.KILOJOULE))
    }
}
