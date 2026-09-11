@file:UseSerializers(
    dev.mahourigan.fooddiary.data.LocalDateSerializer::class,
    dev.mahourigan.fooddiary.data.LocalDateTimeSerializer::class,
)

package dev.mahourigan.fooddiary.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * A kind of symptom. Same string-id approach as [Attribute], for the same
 * reasons: readable storage, forward compatibility, and your own additions
 * being first-class rather than a special case.
 */
@Serializable
@JvmInline
value class SymptomType(val id: String) {

    val isCustom: Boolean get() = id.startsWith(Attribute.CUSTOM_PREFIX)

    companion object {
        fun custom(name: String) = SymptomType(Attribute.CUSTOM_PREFIX + name.trim())
    }
}

enum class SymptomGroup(val label: String) {
    GUT("Gut"),
    BODY("Rest of the body"),
    CUSTOM("Your own"),
}

data class SymptomInfo(
    val type: SymptomType,
    val label: String,
    val group: SymptomGroup,
)

object SymptomTypes {

    val Bloating = SymptomType("bloating")
    val Distension = SymptomType("distension")
    val Gas = SymptomType("gas")
    val Cramping = SymptomType("cramping")
    val Pain = SymptomType("pain")
    val Reflux = SymptomType("reflux")
    val Nausea = SymptomType("nausea")
    val Urgency = SymptomType("urgency")
    val Constipation = SymptomType("constipation")
    val Incomplete = SymptomType("incomplete")
    val Rumbling = SymptomType("rumbling")

    val Fatigue = SymptomType("fatigue")
    val BrainFog = SymptomType("brain-fog")
    val Headache = SymptomType("headache")
    val JointAche = SymptomType("joint-ache")
    val SkinFlare = SymptomType("skin-flare")
    val LowMood = SymptomType("low-mood")
    val PoorSleep = SymptomType("poor-sleep")

    val known: List<SymptomInfo> = listOf(
        SymptomInfo(Bloating, "Bloating", SymptomGroup.GUT),
        SymptomInfo(Distension, "Visibly distended", SymptomGroup.GUT),
        SymptomInfo(Gas, "Wind", SymptomGroup.GUT),
        SymptomInfo(Cramping, "Cramping", SymptomGroup.GUT),
        SymptomInfo(Pain, "Pain", SymptomGroup.GUT),
        SymptomInfo(Reflux, "Reflux", SymptomGroup.GUT),
        SymptomInfo(Nausea, "Nausea", SymptomGroup.GUT),
        SymptomInfo(Urgency, "Urgency", SymptomGroup.GUT),
        SymptomInfo(Constipation, "Constipation", SymptomGroup.GUT),
        SymptomInfo(Incomplete, "Didn't finish", SymptomGroup.GUT),
        SymptomInfo(Rumbling, "Rumbling", SymptomGroup.GUT),

        // Often the clearer signal of the two. Bloating is noisy and close to
        // constant; a reliable next-day fatigue crash is not.
        SymptomInfo(Fatigue, "Fatigue", SymptomGroup.BODY),
        SymptomInfo(BrainFog, "Brain fog", SymptomGroup.BODY),
        SymptomInfo(Headache, "Headache", SymptomGroup.BODY),
        SymptomInfo(JointAche, "Joint ache", SymptomGroup.BODY),
        SymptomInfo(SkinFlare, "Skin flare-up", SymptomGroup.BODY),
        SymptomInfo(LowMood, "Low mood", SymptomGroup.BODY),
        SymptomInfo(PoorSleep, "Slept badly", SymptomGroup.BODY),
    )

    private val byId: Map<SymptomType, SymptomInfo> = known.associateBy { it.type }

    fun label(type: SymptomType): String = when {
        type.isCustom -> type.id.removePrefix(Attribute.CUSTOM_PREFIX)
        else -> byId[type]?.label ?: type.id
    }

    fun group(type: SymptomType): SymptomGroup = when {
        type.isCustom -> SymptomGroup.CUSTOM
        else -> byId[type]?.group ?: SymptomGroup.CUSTOM
    }

    fun byGroup(): Map<SymptomGroup, List<SymptomInfo>> = known.groupBy { it.group }
}

/**
 * How bad it is, in four words.
 *
 * Not a 0–10 slider: ten points of self-reported precision is an illusion —
 * nobody rates the same bloat a 6 twice — and the noise goes straight into the
 * analysis. Four levels you can pick consistently beat ten numbers you can't.
 *
 * The weights are deliberately super-linear. "Very severe" is not four milds,
 * and a linear scale lets a fortnight of background niggles outweigh the one
 * evening that actually told you something.
 */
enum class Severity(val label: String, val weight: Double) {
    MILD("Mild", 1.0),
    MODERATE("Moderate", 2.0),
    SEVERE("Severe", 4.0),
    VERY_SEVERE("Very severe", 7.0),
}

/** A severity, and when it started applying. */
@Serializable
data class SeverityPoint(
    val at: LocalDateTime = LocalDateTime.MIN,
    val severity: Severity = Severity.MILD,
)

/**
 * A symptom episode: a start, maybe an end, and how bad it was along the way.
 *
 * Two ways in, one shape. Starting a timer sets [startedAt] to now and leaves
 * [endedAt] null; filling one in afterwards sets both. Neither is a second-class
 * entry, so nothing downstream has to know which way it was recorded.
 *
 * Duration is never stored, only derived. A stored duration and an edited end
 * time drift apart, and then the scoring runs on whichever happens to be stale.
 *
 * Episodes crossing midnight or running for days need nothing special — two
 * timestamps span whatever they span. A three-day flare is genuine data and the
 * app never truncates one to tidy the display.
 */
@Serializable
data class SymptomEntry(
    val id: String = "",
    val type: SymptomType = SymptomTypes.Bloating,
    val startedAt: LocalDateTime = LocalDateTime.MIN,

    /** Null while it's still going. */
    val endedAt: LocalDateTime? = null,

    /** Always at least one point, the first at [startedAt]. */
    val severityPoints: List<SeverityPoint> = emptyList(),

    /**
     * False once an open episode has gone stale unanswered.
     *
     * The failure this guards against is starting a timer and forgetting it, then
     * having an 80-hour phantom episode dominate every number in the app.
     * Auto-closing at midnight would fix that by destroying exactly the
     * multi-day data worth having, so instead the duration stops being trusted
     * until you say what happened, and the scoring falls back to
     * [UNTRUSTED_MINUTES]. Answer the prompt and the real duration comes back.
     */
    val durationTrusted: Boolean = true,

    val notes: String = "",
) {
    val isOpen: Boolean get() = endedAt == null

    val severity: Severity get() = severityPoints.firstOrNull()?.severity ?: Severity.MILD

    /** The worst it got — what a day summary should lead with. */
    val peakSeverity: Severity
        get() = severityPoints.maxByOrNull { it.severity.weight }?.severity ?: severity

    /** Where the episode ends for arithmetic: its end, or now if it's still open. */
    fun effectiveEnd(now: LocalDateTime): LocalDateTime =
        endedAt ?: maxOf(now, startedAt)

    fun durationMinutes(now: LocalDateTime): Long =
        Duration.between(startedAt, effectiveEnd(now)).toMinutes().coerceAtLeast(0)

    /** The severity in force at a moment: the last point at or before it. */
    fun severityAt(moment: LocalDateTime): Severity =
        severityPoints.lastOrNull { !it.at.isAfter(moment) }?.severity ?: severity

    /** True if the episode covers any part of [date] — a spanning one shows on every day it touches. */
    fun spans(date: LocalDate, now: LocalDateTime): Boolean {
        val end = effectiveEnd(now)
        return !startedAt.toLocalDate().isAfter(date) && !end.toLocalDate().isBefore(date)
    }

    /** Open and untouched long enough that we should stop believing the clock. */
    fun isStale(now: LocalDateTime): Boolean =
        isOpen && durationTrusted && durationMinutes(now) > STALE_AFTER_MINUTES

    /** Minutes of this episode that fall inside a window. */
    fun overlapMinutes(from: LocalDateTime, to: LocalDateTime, now: LocalDateTime): Long {
        val start = maxOf(startedAt, from)
        val end = minOf(effectiveEnd(now), to)
        return Duration.between(start, end).toMinutes().coerceAtLeast(0)
    }

    /**
     * The episode's contribution to a window, in severity-hours.
     *
     * Intensity spread over time rather than an event at a point, which is what
     * makes the long ones behave: a 60-hour flare lands in all three lag windows
     * after the meal before it, in proportion, instead of being pinned to one
     * and vanishing from the others.
     *
     * An untrusted duration contributes its severity over a nominal
     * [UNTRUSTED_MINUTES] instead — still counted, still visible, but unable to
     * outweigh a fortnight of real entries on the strength of a forgotten tap.
     */
    fun loadIn(from: LocalDateTime, to: LocalDateTime, now: LocalDateTime): Double {
        if (!durationTrusted) {
            val overlaps = startedAt < to && effectiveEnd(now) > from
            return if (overlaps) peakSeverity.weight * (UNTRUSTED_MINUTES / 60.0) else 0.0
        }

        val start = maxOf(startedAt, from)
        val end = minOf(effectiveEnd(now), to)
        if (!start.isBefore(end)) return 0.0

        // Walk the severity changes so a flare that peaked in the middle is not
        // scored as though it were severe throughout.
        val boundaries = buildList {
            add(start)
            severityPoints.map { it.at }.filter { it > start && it < end }.sorted().forEach { add(it) }
            add(end)
        }

        return boundaries.zipWithNext().sumOf { (segmentStart, segmentEnd) ->
            val minutes = Duration.between(segmentStart, segmentEnd).toMinutes()
            severityAt(segmentStart).weight * (minutes / 60.0)
        }
    }

    companion object {
        /** How long an open episode may sit untouched before its duration stops counting. */
        const val STALE_AFTER_MINUTES = 24 * 60L

        /** What an untrusted episode is scored as instead: present, and an hour long. */
        const val UNTRUSTED_MINUTES = 60L

        fun startNow(
            id: String,
            type: SymptomType,
            severity: Severity,
            now: LocalDateTime,
            notes: String = "",
        ) = SymptomEntry(
            id = id,
            type = type,
            startedAt = now,
            endedAt = null,
            severityPoints = listOf(SeverityPoint(now, severity)),
            notes = notes,
        )
    }
}

/** Adds a severity change to an open episode, keeping the points in order. */
fun SymptomEntry.withSeverityChange(severity: Severity, at: LocalDateTime): SymptomEntry {
    // Two taps a minute apart mean the second one is a correction, not a change
    // in the symptom, so it replaces rather than stacks.
    val kept = severityPoints.filterNot { it.at == at }
    return copy(severityPoints = (kept + SeverityPoint(at, severity)).sortedBy { it.at })
}

/** Closes an episode, trusting the duration again now that it's been answered. */
fun SymptomEntry.closedAt(end: LocalDateTime): SymptomEntry =
    copy(endedAt = maxOf(end, startedAt), durationTrusted = true)

/**
 * A bowel movement. Its own type, because Bristol 1–7 is a *shape*, not a
 * severity — averaging it into a symptom load would be meaningless.
 */
@Serializable
data class StoolEntry(
    val id: String = "",
    val at: LocalDateTime = LocalDateTime.MIN,
    val bristol: Int = 4,
    val urgency: Severity? = null,
    val notes: String = "",
) {
    companion object {
        val SCALE: List<String> = listOf(
            "Separate hard lumps",
            "Lumpy and sausage-like",
            "Sausage with cracks",
            "Smooth and soft",
            "Soft blobs, clear edges",
            "Mushy, ragged edges",
            "Liquid, no solid pieces",
        )

        fun describe(bristol: Int): String = SCALE.getOrNull(bristol - 1) ?: "Type $bristol"
    }
}
