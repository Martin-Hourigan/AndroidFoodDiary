@file:UseSerializers(
    dev.mahourigan.fooddiary.data.LocalDateSerializer::class,
)

package dev.mahourigan.fooddiary.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** How heavy a bleed day was. Four words, for the same reason [Severity] is four. */
enum class Flow(val label: String) {
    SPOTTING("Spotting"),
    LIGHT("Light"),
    MEDIUM("Medium"),
    HEAVY("Heavy"),
}

/**
 * One day of bleeding.
 *
 * The only thing the cycle half of the app records. Cycle day, cycle length and
 * phase are all worked out from these dates rather than asked for, because
 * almost nobody can say on a given Tuesday whether they are follicular or
 * luteal, and a guessed phase is worse data than none.
 *
 * [flow] is null only for a day carried over from the old phase chip, which
 * said "period" and nothing about how much.
 */
@Serializable
data class PeriodDay(
    val date: LocalDate = LocalDate.MIN,
    val flow: Flow? = null,
)

/** One period: its first and last bleed day, inclusive. */
data class Period(val start: LocalDate, val end: LocalDate) {
    val days: Int get() = ChronoUnit.DAYS.between(start, end).toInt() + 1

    operator fun contains(date: LocalDate): Boolean = date in start..end
}

/**
 * When the next period is likely to start: a range, never a single day.
 *
 * [from] to [to] is the spread of your own recent cycle lengths laid forward
 * from the last start, and [likely] is the median. One cycle of evidence gives
 * a range of one day, which is why [cycles] travels with it — "based on 1
 * cycle" is a claim you can weigh.
 */
data class Prediction(
    val likely: LocalDate,
    val from: LocalDate,
    val to: LocalDate,
    val cycles: Int,
    val typicalLength: Int,
)

/** Where a given day sits. */
data class CycleStatus(
    /** 1 on the first day of a period. */
    val day: Int,
    val phase: CyclePhase?,
    /** True when [phase] comes from a prediction rather than a logged bleed. */
    val phaseEstimated: Boolean,
    /** Days past the latest the next period was expected; 0 when not late. */
    val daysLate: Int,
)

/**
 * The arithmetic over [PeriodDay]s. Pure counting, like everything else here —
 * no model, no population average standing in for your own numbers.
 */
object Cycles {

    /**
     * Two bleed days this far apart or closer belong to one period.
     *
     * A day skipped in the middle of a period is almost always a day you didn't
     * open the app, not a period that stopped and restarted.
     */
    const val JOIN_GAP_DAYS = 2

    /**
     * Cycle lengths outside this are left out of predictions.
     *
     * Not a judgement on the cycle. A 70-day "cycle" in a diary is far more often
     * a month nobody logged than a real one, and one of those would drag every
     * estimate a fortnight late. It still shows in the history.
     */
    val PLAUSIBLE_LENGTH = 15..60

    /** How many recent cycles a prediction looks at. Old ones describe an older you. */
    const val RECENT_CYCLES = 6

    /**
     * The luteal phase is the steady half: about two weeks before the next
     * period, whatever the cycle length. So ovulation is counted back from the
     * predicted start, not forward from the last one.
     */
    const val LUTEAL_DAYS = 14

    /** Days either side of the estimated ovulation that count as the window. */
    const val OVULATION_SPREAD = 2

    /**
     * Bleed days grouped into periods, oldest first.
     *
     * Spotting neither starts nor stretches a period. Mid-cycle spotting is
     * common, and counting it as a start would split one cycle into two short
     * ones and make every estimate after it wrong.
     */
    fun periods(days: List<PeriodDay>): List<Period> {
        val bleeding = days.filter { it.flow != Flow.SPOTTING }.map { it.date }.distinct().sorted()
        val periods = mutableListOf<Period>()
        for (date in bleeding) {
            val last = periods.lastOrNull()
            if (last != null && ChronoUnit.DAYS.between(last.end, date) <= JOIN_GAP_DAYS) {
                periods[periods.lastIndex] = last.copy(end = date)
            } else {
                periods += Period(date, date)
            }
        }
        return periods
    }

    /** Start-to-start lengths, one per completed cycle, oldest first. */
    fun lengths(periods: List<Period>): List<Int> =
        periods.zipWithNext { a, b -> ChronoUnit.DAYS.between(a.start, b.start).toInt() }

    /**
     * The next start, from the median of your recent plausible cycles.
     *
     * Median rather than mean so a single odd cycle moves the range but not the
     * middle of it. Null until there is at least one complete cycle — with only
     * one period logged there is nothing of yours to go on, and a textbook 28
     * days would be a number that looks like it came from you and didn't.
     */
    fun predict(periods: List<Period>): Prediction? {
        val last = periods.lastOrNull() ?: return null
        val recent = lengths(periods).filter { it in PLAUSIBLE_LENGTH }.takeLast(RECENT_CYCLES)
        if (recent.isEmpty()) return null
        val sorted = recent.sorted()
        val median = if (sorted.size % 2 == 1) {
            sorted[sorted.size / 2]
        } else {
            // Round half up: an even split between 28 and 29 is shown as 29,
            // the later of the two, so the range opens no earlier than it should.
            (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2] + 1) / 2
        }
        return Prediction(
            likely = last.start.plusDays(median.toLong()),
            from = last.start.plusDays(sorted.first().toLong()),
            to = last.start.plusDays(sorted.last().toLong()),
            cycles = recent.size,
            typicalLength = median,
        )
    }

    /**
     * Where [date] sits in its cycle, or null before the first logged period.
     *
     * A logged bleed is a fact; every other phase is an estimate from the
     * prediction and says so. Past days are placed against the period that
     * actually came next, so last month's luteal phase is counted back from
     * last month's real start rather than from a guess.
     */
    fun status(days: List<PeriodDay>, date: LocalDate): CycleStatus? {
        val periods = periods(days)
        val index = periods.indexOfLast { !it.start.isAfter(date) }
        if (index < 0) return null
        val current = periods[index]
        val day = ChronoUnit.DAYS.between(current.start, date).toInt() + 1

        if (date in current) {
            return CycleStatus(day = day, phase = CyclePhase.PERIOD, phaseEstimated = false, daysLate = 0)
        }

        val prediction = predict(periods.subList(0, index + 1))
        val nextStart = periods.getOrNull(index + 1)?.start ?: prediction?.likely
        val daysLate = if (index == periods.lastIndex && prediction != null && date > prediction.to) {
            ChronoUnit.DAYS.between(prediction.to, date).toInt()
        } else {
            0
        }

        // Late, or no cycle of evidence yet: the cycle day is still true, but
        // there is no honest phase to give.
        if (nextStart == null || daysLate > 0) {
            return CycleStatus(day = day, phase = null, phaseEstimated = true, daysLate = daysLate)
        }

        val ovulation = nextStart.minusDays(LUTEAL_DAYS.toLong())
        val phase = when {
            date < ovulation.minusDays(OVULATION_SPREAD.toLong()) -> CyclePhase.FOLLICULAR
            date <= ovulation.plusDays(OVULATION_SPREAD.toLong()) -> CyclePhase.OVULATION
            else -> CyclePhase.LUTEAL
        }
        return CycleStatus(day = day, phase = phase, phaseEstimated = true, daysLate = 0)
    }
}
