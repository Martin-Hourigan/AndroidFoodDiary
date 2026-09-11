package dev.mahourigan.fooddiary.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class SymptomEpisodeTest {

    private val monday = LocalDate.of(2026, 9, 7)

    private fun at(day: LocalDate, hour: Int, minute: Int = 0): LocalDateTime = day.atTime(hour, minute)

    private fun episode(
        start: LocalDateTime,
        end: LocalDateTime? = null,
        severity: Severity = Severity.MODERATE,
    ) = SymptomEntry(
        id = "s1",
        type = SymptomTypes.Bloating,
        startedAt = start,
        endedAt = end,
        severityPoints = listOf(SeverityPoint(start, severity)),
    )

    // ---- Duration -----------------------------------------------------------

    @Test
    fun `a closed episode has the duration between its timestamps`() {
        val entry = episode(at(monday, 16, 30), at(monday, 18, 45))
        assertEquals(135L, entry.durationMinutes(at(monday, 22)))
        assertFalse(entry.isOpen)
    }

    @Test
    fun `an open episode runs to now`() {
        val entry = episode(at(monday, 14, 0))
        assertTrue(entry.isOpen)
        assertEquals(120L, entry.durationMinutes(at(monday, 16, 0)))
    }

    @Test
    fun `closing it records the real duration`() {
        val entry = episode(at(monday, 14, 0)).closedAt(at(monday, 15, 30))
        assertEquals(90L, entry.durationMinutes(at(monday, 20)))
    }

    @Test
    fun `an end before the start is clamped rather than going negative`() {
        val entry = episode(at(monday, 14, 0)).closedAt(at(monday, 13, 0))
        assertEquals(0L, entry.durationMinutes(at(monday, 20)))
    }

    // ---- Across midnight and across days ------------------------------------

    @Test
    fun `an episode across midnight shows on both days`() {
        val entry = episode(at(monday, 22, 0), at(monday.plusDays(1), 7, 0))
        val now = at(monday.plusDays(1), 9, 0)

        assertTrue(entry.spans(monday, now))
        assertTrue(entry.spans(monday.plusDays(1), now))
        assertFalse(entry.spans(monday.minusDays(1), now))
        assertFalse(entry.spans(monday.plusDays(2), now))
        assertEquals(9 * 60L, entry.durationMinutes(now))
    }

    @Test
    fun `a three day flare is kept whole, not truncated at midnight`() {
        // Auto-closing at end of day would have been simpler and would have
        // destroyed exactly this. A multi-day flare is the data, not a bug.
        val entry = episode(at(monday, 10, 0), at(monday.plusDays(3), 8, 0))
        val now = at(monday.plusDays(3), 12, 0)

        assertEquals(70 * 60L, entry.durationMinutes(now))
        (0..3).forEach { offset ->
            assertTrue("day $offset should show it", entry.spans(monday.plusDays(offset.toLong()), now))
        }
    }

    @Test
    fun `an open episode spans every day up to today`() {
        val entry = episode(at(monday, 20, 0))
        val now = at(monday.plusDays(2), 11, 0)

        assertTrue(entry.spans(monday, now))
        assertTrue(entry.spans(monday.plusDays(1), now))
        assertTrue(entry.spans(monday.plusDays(2), now))
    }

    // ---- Severity over time -------------------------------------------------

    @Test
    fun `severity in force is the last point at or before the moment`() {
        val entry = episode(at(monday, 12, 0), at(monday, 20, 0), Severity.MILD)
            .withSeverityChange(Severity.SEVERE, at(monday, 15, 0))
            .withSeverityChange(Severity.MODERATE, at(monday, 18, 0))

        assertEquals(Severity.MILD, entry.severityAt(at(monday, 13, 0)))
        assertEquals(Severity.SEVERE, entry.severityAt(at(monday, 16, 0)))
        assertEquals(Severity.MODERATE, entry.severityAt(at(monday, 19, 0)))
        assertEquals(Severity.SEVERE, entry.peakSeverity)
    }

    @Test
    fun `a correction at the same instant replaces rather than stacks`() {
        val start = at(monday, 12, 0)
        val entry = episode(start, at(monday, 14, 0), Severity.MILD)
            .withSeverityChange(Severity.SEVERE, start)

        assertEquals(1, entry.severityPoints.size)
        assertEquals(Severity.SEVERE, entry.severityAt(start))
    }

    // ---- Windows and load ---------------------------------------------------

    @Test
    fun `overlap counts only the minutes inside the window`() {
        val entry = episode(at(monday, 13, 0), at(monday, 17, 0))
        val now = at(monday, 20, 0)

        // Window 12:00–15:00 catches two of the four hours.
        assertEquals(120L, entry.overlapMinutes(at(monday, 12, 0), at(monday, 15, 0), now))
        // A window entirely before it catches nothing.
        assertEquals(0L, entry.overlapMinutes(at(monday, 9, 0), at(monday, 12, 0), now))
    }

    @Test
    fun `load is severity multiplied by hours inside the window`() {
        val entry = episode(at(monday, 13, 0), at(monday, 15, 0), Severity.MODERATE)
        val now = at(monday, 20, 0)

        // Two hours of moderate (weight 2) entirely inside the window.
        assertEquals(4.0, entry.loadIn(at(monday, 12, 0), at(monday, 18, 0), now), 0.0001)
        // Half of it, if the window only covers an hour.
        assertEquals(2.0, entry.loadIn(at(monday, 14, 0), at(monday, 18, 0), now), 0.0001)
    }

    @Test
    fun `a peak in the middle is not scored as though it lasted throughout`() {
        val entry = episode(at(monday, 12, 0), at(monday, 15, 0), Severity.MILD)
            .withSeverityChange(Severity.SEVERE, at(monday, 13, 0))
            .withSeverityChange(Severity.MILD, at(monday, 14, 0))

        // 1h mild + 1h severe + 1h mild = 1 + 4 + 1.
        val load = entry.loadIn(at(monday, 11, 0), at(monday, 16, 0), at(monday, 20, 0))
        assertEquals(6.0, load, 0.0001)
    }

    @Test
    fun `a long flare lands in every lag window after a meal, in proportion`() {
        // 60 hours of moderate bloating starting an hour after dinner. It should
        // contribute to all three windows rather than being pinned to one.
        val dinner = at(monday, 19, 0)
        val entry = episode(dinner.plusHours(1), dinner.plusHours(61), Severity.MODERATE)
        val now = dinner.plusDays(4)

        val early = entry.loadIn(dinner, dinner.plusHours(6), now)
        val next = entry.loadIn(dinner.plusHours(6), dinner.plusHours(24), now)
        val late = entry.loadIn(dinner.plusHours(24), dinner.plusHours(48), now)

        assertEquals(10.0, early, 0.0001) // 5 hours of it
        assertEquals(36.0, next, 0.0001) // 18 hours
        assertEquals(48.0, late, 0.0001) // 24 hours
    }

    // ---- Forgotten timers ---------------------------------------------------

    @Test
    fun `an open episode goes stale after a day`() {
        val entry = episode(at(monday, 9, 0))
        assertFalse(entry.isStale(at(monday, 20, 0)))
        assertTrue(entry.isStale(at(monday.plusDays(1), 12, 0)))
    }

    @Test
    fun `a closed episode is never stale however long it was`() {
        val entry = episode(at(monday, 9, 0), at(monday.plusDays(3), 9, 0))
        assertFalse(entry.isStale(at(monday.plusDays(5), 9, 0)))
    }

    @Test
    fun `an untrusted duration cannot dominate the numbers`() {
        val start = at(monday, 9, 0)
        val forgotten = episode(start, severity = Severity.SEVERE)
            .copy(endedAt = start, durationTrusted = false)

        // Present and counted, but as one nominal hour rather than the 80 the
        // clock would otherwise claim.
        val load = forgotten.loadIn(at(monday, 8, 0), at(monday.plusDays(4), 8, 0), at(monday.plusDays(4), 9, 0))
        assertEquals(Severity.SEVERE.weight, load, 0.0001)
    }

    @Test
    fun `an untrusted episode outside the window still contributes nothing`() {
        val start = at(monday, 9, 0)
        val forgotten = episode(start).copy(endedAt = start, durationTrusted = false)
        assertEquals(0.0, forgotten.loadIn(at(monday, 12, 0), at(monday, 18, 0), at(monday, 20, 0)), 0.0001)
    }

    @Test
    fun `answering a stale episode restores its real duration`() {
        val start = at(monday, 9, 0)
        val forgotten = episode(start).copy(endedAt = start, durationTrusted = false)
        val answered = forgotten.closedAt(at(monday, 14, 0))

        assertTrue(answered.durationTrusted)
        assertEquals(300L, answered.durationMinutes(at(monday, 20, 0)))
    }

    // ---- Starting the clock -------------------------------------------------

    @Test
    fun `starting now leaves it open with one severity point`() {
        val now = at(monday, 14, 20)
        val entry = SymptomEntry.startNow("s9", SymptomTypes.Bloating, Severity.MODERATE, now)

        assertTrue(entry.isOpen)
        assertEquals(now, entry.startedAt)
        assertEquals(1, entry.severityPoints.size)
        assertEquals(Severity.MODERATE, entry.severity)
    }
}
