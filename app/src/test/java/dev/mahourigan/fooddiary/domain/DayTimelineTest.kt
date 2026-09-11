package dev.mahourigan.fooddiary.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DayTimelineTest {

    private val monday = LocalDate.of(2026, 9, 7)
    private val tuesday = monday.plusDays(1)

    private val lunch = MealEntry(id = "lunch", at = monday.atTime(12, 40), items = listOf(MealItem("tomato")))
    private val dinner = MealEntry(id = "dinner", at = monday.atTime(19, 30), items = listOf(MealItem("tomato")))

    private val afterLunch = SymptomEntry(
        id = "bloat",
        type = SymptomTypes.Bloating,
        startedAt = monday.atTime(14, 10),
        endedAt = monday.atTime(17, 0),
        severityPoints = listOf(SeverityPoint(monday.atTime(14, 10), Severity.MODERATE)),
    )

    private val overnight = SymptomEntry(
        id = "overnight",
        type = SymptomTypes.Cramping,
        startedAt = monday.atTime(22, 0),
        endedAt = tuesday.atTime(7, 0),
        severityPoints = listOf(SeverityPoint(monday.atTime(22, 0), Severity.SEVERE)),
    )

    private val stool = StoolEntry(id = "stool", at = monday.atTime(8, 15), bristol = 6)

    private fun timeline(date: LocalDate, now: java.time.LocalDateTime = tuesday.atTime(9, 0)) = dayTimeline(
        meals = listOf(lunch, dinner),
        symptoms = listOf(afterLunch, overnight),
        stools = listOf(stool),
        meds = emptyList(),
        date = date,
        now = now,
    )

    @Test
    fun `the day reads in time order, all types interleaved`() {
        // The reason the screen is one list: seeing the bloat land between lunch
        // and dinner is the entire point.
        val ids = timeline(monday).map { event ->
            when (event) {
                is DayEvent.Meal -> event.entry.id
                is DayEvent.Symptom -> event.entry.id
                is DayEvent.Stool -> event.entry.id
                is DayEvent.Med -> event.entry.id
            }
        }
        assertEquals(listOf("stool", "lunch", "bloat", "dinner", "overnight"), ids)
    }

    @Test
    fun `an episode carried into the next day still appears there`() {
        val ids = timeline(tuesday).map { (it as DayEvent.Symptom).entry.id }
        assertEquals(listOf("overnight"), ids)
    }

    @Test
    fun `a carried-over episode sorts to the top of the day it lands in`() {
        val event = timeline(tuesday).first() as DayEvent.Symptom
        assertTrue(event.startedEarlier)
        assertEquals(tuesday.atStartOfDay(), event.sortAt)
    }

    @Test
    fun `the day it began does not claim it started earlier`() {
        val event = timeline(monday).last() as DayEvent.Symptom
        assertEquals("overnight", event.entry.id)
        assertFalse(event.startedEarlier)
        assertTrue(event.continuesLater)
    }

    @Test
    fun `a day with nothing on it is empty rather than absent`() {
        assertTrue(timeline(monday.minusDays(3)).isEmpty())
    }

    @Test
    fun `open episodes are listed separately for pinning`() {
        val open = SymptomEntry(
            id = "open",
            startedAt = tuesday.atTime(8, 0),
            severityPoints = listOf(SeverityPoint(tuesday.atTime(8, 0), Severity.MILD)),
        )
        val pinned = openEpisodes(listOf(afterLunch, overnight, open))
        assertEquals(listOf("open"), pinned.map { it.id })
    }
}
