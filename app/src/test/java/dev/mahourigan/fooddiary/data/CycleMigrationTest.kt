package dev.mahourigan.fooddiary.data

import dev.mahourigan.fooddiary.domain.CyclePhase
import dev.mahourigan.fooddiary.domain.Flow
import dev.mahourigan.fooddiary.domain.PeriodDay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.LocalDate

/** 17 -> 18: the hand-picked phase chip gives way to logged bleed days. */
class CycleMigrationTest {

    @get:Rule
    val folder = TemporaryFolder()

    private suspend fun repository(raw: String): LocalDiaryRepository {
        val file = File(folder.newFolder(), "diary.json")
        file.writeText(raw)
        return LocalDiaryRepository(file).apply { load() }
    }

    private val v17 = """
        {
          "schemaVersion": 17,
          "seedVersion": 99, "categorySeedVersion": 99, "recipeSeedVersion": 99,
          "days": [
            { "date": "2026-10-01", "cyclePhase": "PERIOD" },
            { "date": "2026-10-02", "cyclePhase": "PERIOD", "stress": "MILD" },
            { "date": "2026-10-15", "cyclePhase": "LUTEAL" }
          ]
        }
    """.trimIndent()

    @Test
    fun `a day marked period becomes a bleed day with no flow guessed`() = runTest {
        val loaded = repository(v17).snapshot.value

        assertEquals(
            listOf(
                PeriodDay(LocalDate.of(2026, 10, 1), flow = null),
                PeriodDay(LocalDate.of(2026, 10, 2), flow = null),
            ),
            loaded.periodDays,
        )
        // A day that was only the chip is gone; one with more on it keeps the rest.
        assertNull(loaded.days.firstOrNull { it.date == LocalDate.of(2026, 10, 1) })
        assertNull(loaded.dayLog(LocalDate.of(2026, 10, 2)).cyclePhase)
        // Other phases were never a bleed and are kept as written.
        assertEquals(CyclePhase.LUTEAL, loaded.dayLog(LocalDate.of(2026, 10, 15)).cyclePhase)
    }

    @Test
    fun `setting a flow replaces the day and clearing it removes it`() = runTest {
        val repo = repository(v17)
        val day = LocalDate.of(2026, 10, 1)

        repo.setFlow(day, Flow.HEAVY)
        assertEquals(Flow.HEAVY, repo.snapshot.value.periodDays.single { it.date == day }.flow)

        repo.setFlow(day, null)
        assertNull(repo.snapshot.value.periodDays.firstOrNull { it.date == day })
    }
}
