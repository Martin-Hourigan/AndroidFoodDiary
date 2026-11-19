@file:UseSerializers(
    dev.mahourigan.fooddiary.data.LocalDateSerializer::class,
    dev.mahourigan.fooddiary.data.LocalDateTimeSerializer::class,
)

package dev.mahourigan.fooddiary.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.LocalDate
import java.time.LocalDateTime

enum class ExerciseLevel(val label: String) {
    NONE("None"),
    LIGHT("Light"),
    MODERATE("Moderate"),
    HARD("Hard"),
}

/**
 * Where in a cycle a day falls. Worked out by [Cycles] from logged bleed days,
 * not picked: see [PeriodDay] for why.
 */
enum class CyclePhase(val label: String) {
    PERIOD("Period"),
    FOLLICULAR("Follicular"),
    OVULATION("Ovulation"),
    LUTEAL("Luteal"),
}

/**
 * The things that aren't food but move symptoms anyway.
 *
 * Without these the suspect ranking will cheerfully blame last night's dinner
 * for a bad night's sleep, and you'll cut a food for nothing.
 *
 * Alcohol and caffeine are deliberately *not* here: they're eaten, so they're
 * ingredients with attributes and arrive through the meal log. Logging them
 * twice would be work for worse data.
 *
 * Every field is optional. A day with nothing filled in is a day you didn't
 * answer, not a day with no stress.
 */
@Serializable
data class DayLog(
    val date: LocalDate = LocalDate.MIN,
    val sleepHours: Double? = null,
    val sleepQuality: Severity? = null,
    val stress: Severity? = null,
    val exercise: ExerciseLevel? = null,
    val unwell: Boolean = false,

    /**
     * The phase as it used to be picked by hand. No longer asked: the phase is
     * now derived from [PeriodDay]s. Older answers other than "period" are kept
     * as they were written — the log is evidence — and "period" itself moved
     * across to a bleed day on upgrade.
     */
    val cyclePhase: CyclePhase? = null,
    val note: String = "",
) {
    val isEmpty: Boolean
        get() = sleepHours == null && sleepQuality == null && stress == null &&
            exercise == null && !unwell && cyclePhase == null && note.isBlank()
}

/**
 * Something taken, at a time.
 *
 * Timed rather than a daily flag, because an antispasmodic swallowed *after*
 * symptoms start must not end up looking like their cause.
 */
@Serializable
data class MedEntry(
    val id: String = "",
    val at: LocalDateTime = LocalDateTime.MIN,
    val name: String = "",
    val dose: String = "",
    val notes: String = "",
)
