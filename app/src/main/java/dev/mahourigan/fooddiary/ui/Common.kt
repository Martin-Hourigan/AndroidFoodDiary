package dev.mahourigan.fooddiary.ui

import dev.mahourigan.fooddiary.domain.Attribute
import dev.mahourigan.fooddiary.domain.Attributes
import dev.mahourigan.fooddiary.domain.MealItem
import dev.mahourigan.fooddiary.domain.Severity
import dev.mahourigan.fooddiary.ui.theme.LocalDiaryPalette
import dev.mahourigan.fooddiary.domain.describe
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

fun formatTime(at: LocalDateTime): String = at.format(TIME)

/**
 * A duration as a person would say it.
 *
 * Minutes disappear once there are hours to talk about — "3h 12m" of bloating is
 * false precision, and "3h" is what you'd actually tell someone.
 */
fun formatDuration(minutes: Long): String = when {
    minutes < 1 -> "just now"
    minutes < 60 -> "${minutes}m"
    minutes < 60 * 24 -> {
        val hours = minutes / 60
        val rest = minutes % 60
        if (hours < 3 && rest > 0) "${hours}h ${rest}m" else "${hours}h"
    }
    else -> {
        val days = minutes / (60 * 24)
        val hours = (minutes % (60 * 24)) / 60
        if (hours > 0) "${days}d ${hours}h" else "${days}d"
    }
}

fun formatDayHeading(date: LocalDate, today: LocalDate): String = when (date) {
    today -> "Today"
    today.minusDays(1) -> "Yesterday"
    today.plusDays(1) -> "Tomorrow"
    else -> buildString {
        append(date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()))
        append(' ')
        append(date.dayOfMonth)
        append(' ')
        append(date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()))
        if (date.year != today.year) append(" ${date.year}")
    }
}

fun minutesBetween(from: LocalDateTime, to: LocalDateTime): Long =
    Duration.between(from, to).toMinutes().coerceAtLeast(0)

/**
 * One item as a phrase: what it was, how it was made, how much.
 *
 * The modifiers have to appear somewhere readable. Recording that the bread was
 * sourdough and then showing "wheat bread" is worse than not asking — you'd have
 * no way to check what a day actually says.
 */
fun describeItem(state: UiState, item: MealItem): String {
    val name = state.nameOf(item.ingredientId)
    val ingredient = state.ingredient(item.ingredientId)
    val portion = if (ingredient != null) item.portion.describe(ingredient) else ""
    val made = item.modifiers
        .sortedBy { Attributes.label(it) }
        .joinToString(", ") { Attributes.label(it).lowercase() }

    return buildString {
        append(name)
        if (made.isNotEmpty()) append(" ($made)")
        if (portion.isNotEmpty()) append(" $portion")
    }
}

/**
 * Colour by how bad it is.
 *
 * Deliberately not a red-to-green scale: nothing here is *good*, so the scale
 * runs from the theme's own quiet surface up to its error colour rather than
 * implying a healthy end.
 */
@Composable
fun severityColor(severity: Severity): Color = when (severity) {
    Severity.MILD -> MaterialTheme.colorScheme.surfaceVariant
    Severity.MODERATE -> MaterialTheme.colorScheme.secondaryContainer
    Severity.SEVERE -> MaterialTheme.colorScheme.tertiaryContainer
    Severity.VERY_SEVERE -> MaterialTheme.colorScheme.errorContainer
}

/**
 * The colour of the band beside a symptom in the timeline.
 *
 * Saturated, unlike [severityColor]: the container colours are meant to sit
 * behind text and are far too pale to read as a 4dp bar, which leaves the
 * timeline's only visual encoding of severity invisible.
 */
@Composable
fun severityBandColor(severity: Severity): Color =
    LocalDiaryPalette.current.severity[severity.ordinal]

@Composable
fun severityContentColor(severity: Severity): Color = when (severity) {
    Severity.MILD -> MaterialTheme.colorScheme.onSurfaceVariant
    Severity.MODERATE -> MaterialTheme.colorScheme.onSecondaryContainer
    Severity.SEVERE -> MaterialTheme.colorScheme.onTertiaryContainer
    Severity.VERY_SEVERE -> MaterialTheme.colorScheme.onErrorContainer
}

/** The four levels, as a row you pick from. */
@Composable
fun SeverityPicker(
    selected: Severity?,
    onSelect: (Severity) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Flowing rather than four equal columns: an equal split truncates "Very
    // severe" to "Very sev…", and the one level you most need to read clearly
    // is the one that gets cut.
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Severity.entries.forEach { severity ->
            FilterChip(
                selected = selected == severity,
                onClick = { onSelect(severity) },
                label = {
                    Text(severity.label, maxLines = 1, style = MaterialTheme.typography.labelMedium)
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = severityColor(severity),
                    selectedLabelColor = severityContentColor(severity),
                ),
            )
        }
    }
}

/**
 * What a meal or ingredient carries.
 *
 * Shown wherever a meal is, because it's the thing the app is actually about and
 * it costs nothing to surface: seeing "gluten, fructans, dairy" under tonight's
 * dinner is the whole reason the ingredient library exists.
 */
@Composable
fun AttributeRow(
    attributes: Collection<Attribute>,
    modifier: Modifier = Modifier,
    max: Int = Int.MAX_VALUE,
) {
    if (attributes.isEmpty()) return

    val sorted = attributes.sortedWith(compareBy({ Attributes.group(it).ordinal }, { Attributes.label(it) }))
    val shown = sorted.take(max)
    val hidden = sorted.size - shown.size

    // Tapping a chip explains it. Half these words are chemistry, and a diary
    // that labels your breakfast "mannitol" and then makes you go and look it
    // up somewhere else has handed you homework.
    var explaining by remember { mutableStateOf<Attribute?>(null) }
    var expanded by remember { mutableStateOf(false) }
    val visible = if (expanded) sorted else shown
    val stillHidden = sorted.size - visible.size

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        visible.forEach { attribute ->
            SuggestionChip(
                onClick = { explaining = attribute },
                label = {
                    Text(Attributes.label(attribute), style = MaterialTheme.typography.labelSmall)
                },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
        if (stillHidden > 0) {
            // Reveals the rest rather than being decoration. You cannot tap
            // what is not drawn, so a permanently hidden chip would be a
            // permanently unexplainable one.
            Text(
                "+$stillHidden",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .clickable { expanded = true }
                    .padding(horizontal = 4.dp, vertical = 2.dp),
            )
        }
    }

    explaining?.let { attribute ->
        AttributeExplainer(attribute = attribute, onDismiss = { explaining = null })
    }
}

/**
 * What a word on a chip means, without leaving the day.
 *
 * Three things in order of how often they are wanted: what it is, the
 * non-obvious thing worth knowing, and — behind a tap, because it is the
 * least often wanted and the longest — what trouble with it feels like.
 *
 * Tapping the card closes it, which is the gesture people try first on
 * something this small. The symptoms button is the one place inside that does
 * not, so opening it cannot dismiss the thing you just opened.
 */
@Composable
private fun AttributeExplainer(attribute: Attribute, onDismiss: () -> Unit) {
    var showSymptoms by remember(attribute) { mutableStateOf(false) }

    val label = Attributes.label(attribute)
    val what = Attributes.what(attribute)
    val note = Attributes.note(attribute)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .clickable(onClick = onDismiss),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(label, style = MaterialTheme.typography.titleMedium)
                        Text(
                            Attributes.group(attribute).label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    // An X rather than a "Close" button along the bottom. That
                    // one shared a row with "Symptoms of intolerance", which is
                    // four times as wide, and the loser of that fight wrapped
                    // its own label onto two lines. An icon cannot wrap.
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (what.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(what, style = MaterialTheme.typography.bodyMedium)
                }

                if (note.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (what.isBlank() && note.isBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "One of your own attributes, so there is nothing here the app " +
                            "can tell you about it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(Modifier.height(16.dp))

                if (showSymptoms) {
                    Text(
                        "Trouble with it feels like",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(Attributes.symptoms(attribute), style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(16.dp))
                }

                // On its own, with nothing to be squeezed by. Tapping the card
                // or outside it closes; this is the only control that doesn't.
                if (!showSymptoms) {
                    TextButton(onClick = { showSymptoms = true }) {
                        Text("Symptoms of intolerance")
                    }
                }
            }
        }
    }
}
