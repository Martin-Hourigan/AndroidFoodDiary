package dev.mahourigan.fooddiary.domain

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * One thing that happened, for the day view.
 *
 * A single interleaved list rather than four sections, because the whole point
 * of the screen is seeing that the bloating started ninety minutes after lunch.
 * Separate lists per type would hide exactly the relationship you're looking for.
 */
sealed interface DayEvent {

    /** Where it sorts within the day. */
    val sortAt: LocalDateTime

    data class Meal(val entry: MealEntry) : DayEvent {
        override val sortAt: LocalDateTime get() = entry.at
    }

    /**
     * A symptom episode overlapping this day.
     *
     * [startedEarlier] is true when the episode began before the day being
     * viewed, which the card says out loud — an episode you started on Monday
     * must not silently look like a Wednesday one, and it must not vanish from
     * Wednesday either.
     */
    data class Symptom(
        val entry: SymptomEntry,
        val startedEarlier: Boolean,
        val continuesLater: Boolean,
        override val sortAt: LocalDateTime,
    ) : DayEvent

    data class Stool(val entry: StoolEntry) : DayEvent {
        override val sortAt: LocalDateTime get() = entry.at
    }

    data class Med(val entry: MedEntry) : DayEvent {
        override val sortAt: LocalDateTime get() = entry.at
    }
}

/**
 * Everything that touches [date], in time order.
 *
 * An episode running across midnight sorts to the start of the day it's carried
 * into, so a bloat that began at 22:00 yesterday sits at the top of today rather
 * than in the middle of the evening.
 */
fun dayTimeline(
    meals: List<MealEntry>,
    symptoms: List<SymptomEntry>,
    stools: List<StoolEntry>,
    meds: List<MedEntry>,
    date: LocalDate,
    now: LocalDateTime,
): List<DayEvent> {
    val dayStart = date.atStartOfDay()

    val events = buildList<DayEvent> {
        meals.filter { it.at.toLocalDate() == date }.forEach { add(DayEvent.Meal(it)) }
        stools.filter { it.at.toLocalDate() == date }.forEach { add(DayEvent.Stool(it)) }
        meds.filter { it.at.toLocalDate() == date }.forEach { add(DayEvent.Med(it)) }

        symptoms.filter { it.spans(date, now) }.forEach { entry ->
            val startedEarlier = entry.startedAt.toLocalDate() < date
            add(
                DayEvent.Symptom(
                    entry = entry,
                    startedEarlier = startedEarlier,
                    continuesLater = entry.effectiveEnd(now).toLocalDate() > date,
                    sortAt = if (startedEarlier) dayStart else entry.startedAt,
                ),
            )
        }
    }

    return events.sortedBy { it.sortAt }
}

/** Open episodes, worst first — these pin above the day rather than sitting in it. */
fun openEpisodes(symptoms: List<SymptomEntry>): List<SymptomEntry> =
    symptoms.filter { it.isOpen }.sortedByDescending { it.startedAt }
