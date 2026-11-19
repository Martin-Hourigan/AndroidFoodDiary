package dev.mahourigan.fooddiary.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CycleTest {

    private val start = LocalDate.of(2026, 1, 5)

    /** A period of [days] days starting [offset] days after [start]. */
    private fun bleed(offset: Long, days: Int = 5, flow: Flow = Flow.MEDIUM): List<PeriodDay> =
        (0 until days).map { PeriodDay(start.plusDays(offset + it), flow) }

    // ---- Grouping -----------------------------------------------------------

    @Test
    fun `consecutive bleed days are one period`() {
        val periods = Cycles.periods(bleed(0, days = 5))
        assertEquals(listOf(Period(start, start.plusDays(4))), periods)
        assertEquals(5, periods.single().days)
    }

    @Test
    fun `a day missed in the middle does not split a period`() {
        val days = bleed(0, days = 2) + bleed(3, days = 2)
        assertEquals(1, Cycles.periods(days).size)
    }

    @Test
    fun `a longer gap starts a new period`() {
        val days = bleed(0, days = 2) + bleed(6, days = 2)
        assertEquals(2, Cycles.periods(days).size)
    }

    @Test
    fun `spotting neither starts nor stretches a period`() {
        // Mid-cycle spotting counted as a start would split one cycle into two
        // short ones and wreck every estimate after it.
        val days = bleed(0) + listOf(PeriodDay(start.plusDays(14), Flow.SPOTTING))
        assertEquals(1, Cycles.periods(days).size)
        assertEquals(start.plusDays(4), Cycles.periods(days).single().end)
    }

    @Test
    fun `order of entry does not matter`() {
        val days = (bleed(28) + bleed(0)).shuffled(java.util.Random(1))
        assertEquals(listOf(start, start.plusDays(28)), Cycles.periods(days).map { it.start })
    }

    // ---- Prediction ---------------------------------------------------------

    @Test
    fun `one period is not enough to predict anything`() {
        // No textbook 28 standing in for a number of your own.
        assertNull(Cycles.predict(Cycles.periods(bleed(0))))
    }

    @Test
    fun `the prediction is the median laid forward with the spread as its range`() {
        // Cycles of 27, 30, 28: median 28, range 27..30.
        val days = bleed(0) + bleed(27) + bleed(57) + bleed(85)
        val prediction = Cycles.predict(Cycles.periods(days))!!

        assertEquals(3, prediction.cycles)
        assertEquals(28, prediction.typicalLength)
        assertEquals(start.plusDays(85 + 28), prediction.likely)
        assertEquals(start.plusDays(85 + 27), prediction.from)
        assertEquals(start.plusDays(85 + 30), prediction.to)
    }

    @Test
    fun `a month nobody logged is left out of the estimate`() {
        // 28, then 84 (two periods unlogged), then 28. Averaging the 84 in
        // would push the next estimate weeks late.
        val days = bleed(0) + bleed(28) + bleed(112) + bleed(140)
        val prediction = Cycles.predict(Cycles.periods(days))!!
        assertEquals(2, prediction.cycles)
        assertEquals(28, prediction.typicalLength)
    }

    @Test
    fun `only recent cycles count`() {
        // Eight old 35-day cycles, then six recent 26-day ones.
        val starts = (0 until 9).map { it * 35L } + (1..6).map { 8 * 35L + it * 26L }
        val days = starts.flatMap { bleed(it) }
        assertEquals(26, Cycles.predict(Cycles.periods(days))!!.typicalLength)
    }

    // ---- Status -------------------------------------------------------------

    private val regular = bleed(0) + bleed(28) + bleed(56)

    @Test
    fun `nothing before the first period`() {
        assertNull(Cycles.status(regular, start.minusDays(1)))
    }

    @Test
    fun `a logged bleed day is the period and is not an estimate`() {
        val status = Cycles.status(regular, start.plusDays(58))!!
        assertEquals(3, status.day)
        assertEquals(CyclePhase.PERIOD, status.phase)
        assertFalse(status.phaseEstimated)
    }

    @Test
    fun `ovulation is counted back from the next start`() {
        // Next start is day 84; ovulation estimated at 70, window 68..72.
        assertEquals(CyclePhase.FOLLICULAR, Cycles.status(regular, start.plusDays(67))!!.phase)
        assertEquals(CyclePhase.OVULATION, Cycles.status(regular, start.plusDays(68))!!.phase)
        assertEquals(CyclePhase.OVULATION, Cycles.status(regular, start.plusDays(72))!!.phase)
        assertEquals(CyclePhase.LUTEAL, Cycles.status(regular, start.plusDays(73))!!.phase)
        assertTrue(Cycles.status(regular, start.plusDays(73))!!.phaseEstimated)
    }

    @Test
    fun `a past cycle is placed against the period that actually came next`() {
        // A short 21-day cycle, then regular ones. Day 10 of that first cycle
        // is past ovulation for a cycle that really ended on day 21, whatever
        // the later average says.
        val days = bleed(0) + bleed(21) + bleed(49) + bleed(77)
        assertEquals(CyclePhase.LUTEAL, Cycles.status(days, start.plusDays(10))!!.phase)
    }

    @Test
    fun `past the expected range it says late and offers no phase`() {
        // Regular 28s, so the range is 84..84. Day 90 is six days late.
        val status = Cycles.status(regular, start.plusDays(90))!!
        assertEquals(6, status.daysLate)
        assertNull(status.phase)
        assertEquals(35, status.day)
    }

    @Test
    fun `with one period there is a cycle day but no phase`() {
        val status = Cycles.status(bleed(0), start.plusDays(10))!!
        assertEquals(11, status.day)
        assertNull(status.phase)
    }
}
