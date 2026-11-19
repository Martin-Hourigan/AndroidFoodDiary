package dev.mahourigan.fooddiary.ui

import dev.mahourigan.fooddiary.domain.CycleStatus
import dev.mahourigan.fooddiary.domain.Cycles
import dev.mahourigan.fooddiary.domain.Flow
import dev.mahourigan.fooddiary.domain.Period
import dev.mahourigan.fooddiary.domain.Prediction
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val MONTH = DateTimeFormatter.ofPattern("MMMM yyyy")
private val SHORT = DateTimeFormatter.ofPattern("d MMM")

/**
 * The cycle: a month you can tap a day of, and what that day's flow was.
 *
 * Logging is one tap on a chip, saved as it is tapped, with no Save button —
 * it is something done most mornings for a week, and a form would be a second
 * tap every one of those mornings for nothing.
 *
 * Only bleed days are asked for. The phase, the cycle day and the prediction
 * are all counted from them; see [Cycles].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CycleScreen(
    state: UiState,
    onSetFlow: (LocalDate, Flow?) -> Unit,
    onBack: () -> Unit,
) {
    val today = state.now.toLocalDate()
    var selected by remember { mutableStateOf(minOf(state.date, today)) }
    var month by remember { mutableStateOf(YearMonth.from(selected)) }

    val periodDays = state.snapshot.periodDays
    val periods = remember(periodDays) { Cycles.periods(periodDays) }
    val prediction = remember(periods) { Cycles.predict(periods) }
    val flows = remember(periodDays) { periodDays.associate { it.date to it.flow } }
    val status = remember(periodDays, selected) { Cycles.status(periodDays, selected) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cycle") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
        ) {
            Summary(status = status, prediction = prediction, today = today, hasPeriods = periods.isNotEmpty())

            Spacer(Modifier.height(12.dp))
            MonthHeader(
                month = month,
                // One month past today, so a predicted start just over the
                // month boundary can still be seen.
                canGoForward = month <= YearMonth.from(today),
                onChange = { month = it },
            )
            Spacer(Modifier.height(4.dp))
            MonthGrid(
                month = month,
                today = today,
                selected = selected,
                flows = flows,
                prediction = prediction,
                onSelect = { selected = it },
            )

            Spacer(Modifier.height(16.dp))
            Text(formatDayHeading(selected, today), style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            val current = flows[selected]
            val logged = selected in flows
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = !logged,
                    onClick = { if (logged) onSetFlow(selected, null) },
                    label = { Text("None") },
                )
                Flow.entries.forEach { flow ->
                    FilterChip(
                        selected = logged && current == flow,
                        onClick = { onSetFlow(selected, flow) },
                        label = { Text(flow.label) },
                    )
                }
            }
            if (logged && current == null) {
                Text(
                    "Marked as a period day before flow was recorded. Pick one to fill it in.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (periods.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                Text("Recent periods", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                History(periods = periods)
            }
        }
    }
}

/** "Day 12 · follicular (estimated)", for this screen and the day card. */
fun describeCycleDay(status: CycleStatus): String = buildString {
    append("cycle day ${status.day}")
    when {
        status.daysLate > 0 -> append(" · ${status.daysLate} ${if (status.daysLate == 1) "day" else "days"} later than usual")
        status.phase != null -> {
            append(" · ${status.phase.label.lowercase()}")
            if (status.phaseEstimated) append(", estimated")
        }
    }
}

@Composable
private fun Summary(status: CycleStatus?, prediction: Prediction?, today: LocalDate, hasPeriods: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                status?.let { describeCycleDay(it).replaceFirstChar(Char::uppercase) } ?: "Nothing logged yet",
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                when {
                    prediction != null && prediction.to >= today -> buildString {
                        append("Next period likely ")
                        append(formatRange(prediction.from, prediction.to))
                        append(" — from ${prediction.cycles} ${if (prediction.cycles == 1) "cycle" else "cycles"}")
                        append(", usually ${prediction.typicalLength} days.")
                    }
                    prediction != null -> "Expected by ${prediction.to.format(SHORT)}. Log it when it comes."
                    hasPeriods -> "Log the start of your next period and the app can estimate the one after."
                    else -> "Tap a day and its flow. Phases and estimates are counted from these; nothing is assumed."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (status?.phaseEstimated == true && status.phase != null) {
                // Said once, here, because it is the honest limit of the whole
                // screen: dates alone cannot see ovulation.
                Text(
                    "Phases outside a period are counted back from the next start, not measured.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MonthHeader(month: YearMonth, canGoForward: Boolean, onChange: (YearMonth) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onChange(month.minusMonths(1)) }) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month")
        }
        Text(
            month.format(MONTH),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
        )
        IconButton(onClick = { onChange(month.plusMonths(1)) }, enabled = canGoForward) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Next month")
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    today: LocalDate,
    selected: LocalDate,
    flows: Map<LocalDate, Flow?>,
    prediction: Prediction?,
    onSelect: (LocalDate) -> Unit,
) {
    val first = month.atDay(1)
    // Monday first, as the calendars here are.
    val lead = (first.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
    val cells = List(lead) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }

    Row(modifier = Modifier.fillMaxWidth()) {
        DayOfWeek.entries.forEach { day ->
            Text(
                day.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
        }
    }
    cells.chunked(7).forEach { week ->
        Row(modifier = Modifier.fillMaxWidth()) {
            (0 until 7).forEach { column ->
                val date = week.getOrNull(column)
                Box(modifier = Modifier.weight(1f).aspectRatio(1f).padding(2.dp), contentAlignment = Alignment.Center) {
                    if (date != null) {
                        DayCell(
                            date = date,
                            isToday = date == today,
                            isSelected = date == selected,
                            // Logging bleeding in the future is a mistake waiting
                            // to happen, same as logging tomorrow's dinner.
                            enabled = !date.isAfter(today),
                            logged = date in flows,
                            flow = flows[date],
                            predicted = prediction != null && date > today &&
                                date in prediction.from..prediction.to,
                            onClick = { onSelect(date) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    isToday: Boolean,
    isSelected: Boolean,
    enabled: Boolean,
    logged: Boolean,
    flow: Flow?,
    predicted: Boolean,
    onClick: () -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    val fill = if (logged) primary.copy(alpha = flowAlpha(flow)) else Color.Transparent
    val content = when {
        logged && flowAlpha(flow) >= 0.6f -> MaterialTheme.colorScheme.onPrimary
        enabled -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    }

    var modifier = Modifier.fillMaxSize().clip(CircleShape).background(fill)
    // A predicted day is an outline, never a fill: it hasn't happened, and it
    // must not be mistaken at a glance for a day that has.
    if (predicted) modifier = modifier.border(1.dp, primary.copy(alpha = 0.6f), CircleShape)
    if (isSelected) modifier = modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
    if (enabled) modifier = modifier.clickable(onClick = onClick)

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            color = content,
        )
    }
}

/** Heavier flow, deeper colour. An unrecorded flow shows as medium. */
private fun flowAlpha(flow: Flow?): Float = when (flow) {
    Flow.SPOTTING -> 0.22f
    Flow.LIGHT -> 0.45f
    Flow.MEDIUM, null -> 0.7f
    Flow.HEAVY -> 1f
}

@Composable
private fun History(periods: List<Period>) {
    val lengths = Cycles.lengths(periods)
    // Newest first, and only the last year or so; the full record is in the file.
    periods.withIndex().reversed().take(12).forEach { (index, period) ->
        val cycle = lengths.getOrNull(index)
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            Text(
                formatRange(period.start, period.end),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                buildString {
                    append("${period.days} ${if (period.days == 1) "day" else "days"}")
                    if (cycle != null) {
                        append(" · cycle $cycle")
                        if (cycle !in Cycles.PLAUSIBLE_LENGTH) append(" (left out of estimates)")
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

private fun formatRange(from: LocalDate, to: LocalDate): String =
    if (from == to) from.format(SHORT) else "${from.format(SHORT)} – ${to.format(SHORT)}"
