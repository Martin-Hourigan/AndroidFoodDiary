package dev.mahourigan.fooddiary.ui

import dev.mahourigan.fooddiary.domain.DayEvent
import dev.mahourigan.fooddiary.ui.theme.LocalDiaryPalette
import dev.mahourigan.fooddiary.domain.MealEntry
import dev.mahourigan.fooddiary.domain.MedEntry
import dev.mahourigan.fooddiary.domain.Severity
import dev.mahourigan.fooddiary.domain.StoolEntry
import dev.mahourigan.fooddiary.domain.SymptomEntry
import dev.mahourigan.fooddiary.domain.SymptomTypes
import dev.mahourigan.fooddiary.domain.bucket
import dev.mahourigan.fooddiary.domain.describe
import dev.mahourigan.fooddiary.domain.diffFromBases
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Wc
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.LocalDateTime

private val GUTTER = 52.dp

/** The three bottom buttons: one size, set once. */
private val BAR_BUTTON = 56.dp
private val BAR_ICON = 24.dp

/**
 * The day, as one list.
 *
 * Meals, symptoms, stools and medication interleaved in time order rather than
 * split into sections — the entire point of the screen is noticing that the
 * bloating started ninety minutes after lunch, and four separate lists would
 * hide precisely that.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    state: UiState,
    onShowDate: (LocalDate) -> Unit,
    onLogMeal: () -> Unit,
    onLogSymptom: () -> Unit,
    onLogStool: () -> Unit,
    onEditMeal: (MealEntry) -> Unit,
    onDeleteMeal: (MealEntry) -> Unit,
    onStopSymptom: (SymptomEntry) -> Unit,
    onChangeSeverity: (SymptomEntry, Severity) -> Unit,
    onDistrustDuration: (SymptomEntry) -> Unit,
    onDeleteSymptom: (SymptomEntry) -> Unit,
    onDeleteStool: (StoolEntry) -> Unit,
    onOpenDayLog: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(formatDayHeading(state.date, state.now.toLocalDate()))
                        if (!state.isToday) {
                            Text(
                                state.date.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { onShowDate(state.date.minusDays(1)) }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous day")
                    }
                },
                actions = {
                    // Forward past today is pointless: you cannot have eaten
                    // tomorrow, and a diary that lets you log into the future
                    // just collects mistakes.
                    IconButton(
                        onClick = { onShowDate(state.date.plusDays(1)) },
                        enabled = state.date < state.now.toLocalDate(),
                    ) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next day")
                    }
                    // A cog, not a three-dot overflow. The menu behind those
                    // dots held two unlike things — the food library you edit
                    // often and the settings you set once — and neither was
                    // findable. Settings is the one that belongs in a corner.
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        bottomBar = {
            LogBar(onLogSymptom = onLogSymptom, onLogStool = onLogStool, onOpenLibrary = onOpenLibrary)
        },
        floatingActionButton = {
            // Eating is the thing that happens most, so it gets the one control
            // that is always in the same place and never shares a row.
            // Round, not Material 3's default squircle. Everything else on
            // this screen is a rounded rectangle — rows, cards, the buttons
            // below — so a circle is the one shape that reads as a separate
            // kind of thing rather than one more card that happens to float.
            FloatingActionButton(onClick = onLogMeal, shape = CircleShape) {
                Icon(Icons.Outlined.Add, contentDescription = "Log a meal")
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
    ) { padding ->
        val timeline = state.timeline
        val stale = state.open.filter { it.isStale(state.now) }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 88.dp),
        ) {
            // Anything left running comes first, wherever the day has scrolled
            // to. An episode you forgot to close is the one thing on this screen
            // that needs an answer.
            items(stale, key = { "stale-${it.id}" }) { entry ->
                StaleEpisodeCard(
                    entry = entry,
                    now = state.now,
                    onStop = { onStopSymptom(entry) },
                    onStillGoing = { onChangeSeverity(entry, entry.severityAt(state.now)) },
                    onForgot = { onDistrustDuration(entry) },
                )
            }

            if (timeline.isEmpty()) {
                item { EmptyDay(isToday = state.isToday) }
            }

            items(timeline, key = { event -> event.key() }) { event ->
                when (event) {
                    is DayEvent.Meal -> MealRow(
                        entry = event.entry,
                        state = state,
                        onEdit = { onEditMeal(event.entry) },
                        onDelete = { onDeleteMeal(event.entry) },
                    )

                    is DayEvent.Symptom -> SymptomRow(
                        event = event,
                        now = state.now,
                        onStop = { onStopSymptom(event.entry) },
                        onWorse = { onChangeSeverity(event.entry, event.entry.severityAt(state.now).worse()) },
                        onEasing = { onChangeSeverity(event.entry, event.entry.severityAt(state.now).easier()) },
                        onDelete = { onDeleteSymptom(event.entry) },
                    )

                    is DayEvent.Stool -> StoolRow(entry = event.entry, onDelete = { onDeleteStool(event.entry) })

                    is DayEvent.Med -> MedRow(entry = event.entry)
                }
            }

            item { DayTotal(state = state) }

            item { DayLogCard(state = state, onOpen = onOpenDayLog) }
        }
    }
}

private fun DayEvent.key(): String = when (this) {
    is DayEvent.Meal -> "meal-${entry.id}"
    is DayEvent.Symptom -> "symptom-${entry.id}"
    is DayEvent.Stool -> "stool-${entry.id}"
    is DayEvent.Med -> "med-${entry.id}"
}

private fun Severity.worse(): Severity =
    Severity.entries.getOrElse(ordinal + 1) { Severity.VERY_SEVERE }

private fun Severity.easier(): Severity =
    Severity.entries.getOrElse(ordinal - 1) { Severity.MILD }

@Composable
private fun LogBar(onLogSymptom: () -> Unit, onLogStool: () -> Unit, onOpenLibrary: () -> Unit) {
    // Three circles of one size, evenly spread, with the round + centred above
    // them. Same shape and same weight because they are the same kind of thing:
    // three places to go, none of them the main event.
    //
    // Icons alone, no labels. It costs a beginner one guess at "book" and buys
    // a bar that is mostly empty paper, which is the look this app is going
    // for — and the two that matter are unmistakable anyway. Each carries a
    // contentDescription, so nothing is lost to a screen reader.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            // A plain Row in a bottomBar gets no inset handling of its own, so
            // without this the buttons sit under the gesture bar.
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BarButton(onClick = onOpenLibrary) {
            Icon(
                Icons.Outlined.MenuBook,
                contentDescription = "Library",
                modifier = Modifier.size(BAR_ICON),
            )
        }
        BarButton(onClick = onLogSymptom) {
            Icon(
                Icons.Outlined.MonitorHeart,
                contentDescription = "Log a symptom",
                modifier = Modifier.size(BAR_ICON),
            )
        }
        BarButton(onClick = onLogStool) {
            // The restroom door sign, not a drawing of a stool or of a toilet.
            // Material ships neither of those and never has — checked both the
            // bundled set and Material Symbols. Two hand-drawn attempts came
            // first and both were worse than this: they were mine to keep
            // working, they never matched the optical sizing of the two stock
            // icons beside them, and naming the room is the politer distance
            // anyway for a button pressed with company.
            Icon(
                Icons.Outlined.Wc,
                contentDescription = "Log a stool",
                modifier = Modifier.size(BAR_ICON),
            )
        }
    }
}

/** One of the three, so they cannot drift apart in size or shape. */
@Composable
private fun BarButton(onClick: () -> Unit, icon: @Composable () -> Unit) {
    FilledTonalIconButton(
        onClick = onClick,
        // Material 3 rounds these to a squircle now. Round, to match the + and
        // to keep the bar reading as one family of controls.
        shape = CircleShape,
        modifier = Modifier.size(BAR_BUTTON),
    ) { icon() }
}

@Composable
private fun EmptyDay(isToday: Boolean) {
    Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            if (isToday) "Nothing logged yet today." else "Nothing logged on this day.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** The left column: a time, or nothing when the row is a continuation. */
@Composable
private fun Gutter(text: String, emphasis: Boolean = false) {
    Text(
        text,
        modifier = Modifier.width(GUTTER),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = if (emphasis) FontWeight.SemiBold else FontWeight.Normal,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun MealRow(
    entry: MealEntry,
    state: UiState,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
    ) {
        // A guessed time is shown as one. Reading "08:00" back in three weeks
        // and believing you looked at a clock is exactly how a diary quietly
        // becomes fiction.
        Gutter(
            if (entry.timeApproximate) "~${formatTime(entry.at)}" else formatTime(entry.at),
            emphasis = true,
        )

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    entry.title(state.byId),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                val energy = state.energyOf(entry)
                if (energy.isKnown) {
                    Text(
                        energy.describe(state.energyUnit),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // The recipe diff, read as a change rather than as a full list —
            // the entry itself always holds every ingredient.
            val diff = entry.diffFromBases()
            if (diff != null && !diff.isUnchanged) {
                val changes = buildList {
                    diff.added.forEach { add("+ " + state.nameOf(it.ingredientId).lowercase()) }
                    diff.removed.forEach { add("− " + state.nameOf(it.ingredientId).lowercase()) }
                    diff.reportioned.forEach { item ->
                        val ingredient = state.ingredient(item.ingredientId)
                        val portion = if (ingredient != null) item.portion.describe(ingredient) else ""
                        add(state.nameOf(item.ingredientId).lowercase() + " " + portion)
                    }
                }
                Text(
                    changes.joinToString("   "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else if (entry.isAdHoc) {
                Text(
                    entry.items.joinToString(", ") { describeItem(state, it) },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (entry.notes.isNotBlank()) {
                Text(
                    entry.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(4.dp))
            AttributeRow(state.attributesOf(entry.items), max = 6)
        }

        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Meal options")
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(text = { Text("Edit") }, onClick = { menuOpen = false; onEdit() })
                DropdownMenuItem(text = { Text("Delete") }, onClick = { menuOpen = false; onDelete() })
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun SymptomRow(
    event: DayEvent.Symptom,
    now: LocalDateTime,
    onStop: () -> Unit,
    onWorse: () -> Unit,
    onEasing: () -> Unit,
    onDelete: () -> Unit,
) {
    val entry = event.entry
    val severity = entry.severityAt(now)
    var menuOpen by remember { mutableStateOf(false) }
    val palette = LocalDiaryPalette.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            // A running episode is the one row on this screen that is *current*
            // and waiting on you, so it gets the treatment a game gives the
            // thing under the cursor: a wash, and an edge to say so without
            // relying on the colour being seen.
            .then(
                if (entry.isOpen) {
                    Modifier
                        .background(palette.liveTint)
                        .drawBehind {
                            drawRect(
                                color = palette.liveEdge,
                                size = size.copy(width = 3.dp.toPx()),
                            )
                        }
                } else {
                    Modifier
                },
            )
            .padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
    ) {
        Gutter(if (event.startedEarlier) "" else formatTime(entry.startedAt), emphasis = true)

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // A band rather than a dot: the point of an episode is that it
                // lasted, and a coloured bar is the only way that reads at a
                // glance next to a meal.
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(18.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(severityBandColor(severity)),
                )
                Spacer(Modifier.width(8.dp))
                Text(SymptomTypes.label(entry.type), style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.width(6.dp))
                Text(
                    severity.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                buildString {
                    // Said from the reader's point of view. "Since 16:30
                    // yesterday" is what you need on day two; a bare start time
                    // would look like it happened this morning.
                    if (event.startedEarlier) {
                        append("since ${formatTime(entry.startedAt)} ")
                        append(if (entry.startedAt.toLocalDate() == event.sortAt.toLocalDate().minusDays(1)) "yesterday" else entry.startedAt.toLocalDate().toString())
                        append(" · ")
                    }
                    val minutes = entry.durationMinutes(now)
                    // "just now and going" is not a sentence. A timer started
                    // seconds ago should say so and then get out of the way.
                    if (entry.isOpen && minutes < 1) {
                        append("just started, timing")
                    } else {
                        append(formatDuration(minutes))
                        if (entry.isOpen) append(" and going")
                    }
                    if (!entry.isOpen && !event.startedEarlier) {
                        append(", until ${formatTime(entry.effectiveEnd(now))}")
                    }
                    if (!entry.durationTrusted) append(" · duration not counted")
                    if (event.continuesLater) append(" · continues")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (entry.notes.isNotBlank()) {
                Text(entry.notes, style = MaterialTheme.typography.bodySmall)
            }

            if (entry.isOpen) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = onStop) { Text("Stopped now") }
                    TextButton(onClick = onWorse) { Text("Worse") }
                    TextButton(onClick = onEasing) { Text("Easing") }
                }
            }
        }

        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Symptom options")
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(text = { Text("Delete") }, onClick = { menuOpen = false; onDelete() })
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun StoolRow(entry: StoolEntry, onDelete: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
    ) {
        Gutter(formatTime(entry.at), emphasis = true)
        Column(modifier = Modifier.weight(1f)) {
            Text("Type ${entry.bristol}", style = MaterialTheme.typography.titleSmall)
            Text(
                StoolEntry.describe(entry.bristol) + (entry.urgency?.let { " · ${it.label} urgency" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (entry.notes.isNotBlank()) Text(entry.notes, style = MaterialTheme.typography.bodySmall)
        }
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Options")
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(text = { Text("Delete") }, onClick = { menuOpen = false; onDelete() })
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun MedRow(entry: MedEntry) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
        Gutter(formatTime(entry.at))
        Column {
            Text(entry.name, style = MaterialTheme.typography.titleSmall)
            if (entry.dose.isNotBlank()) {
                Text(entry.dose, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

/**
 * The prompt for an episode left running.
 *
 * Auto-closing at midnight would have been easier, and would have thrown away
 * every genuine multi-day flare. So it asks instead, and until it's answered the
 * duration simply doesn't count.
 */
@Composable
private fun StaleEpisodeCard(
    entry: SymptomEntry,
    now: LocalDateTime,
    onStop: () -> Unit,
    onStillGoing: () -> Unit,
    onForgot: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                "${SymptomTypes.label(entry.type)} has been running for ${formatDuration(entry.durationMinutes(now))}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Text(
                "Still going, or did you forget to close it? Until this is answered the duration isn't counted.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onStillGoing) { Text("Still going") }
                TextButton(onClick = onStop) { Text("Stopped now") }
                TextButton(onClick = onForgot) { Text("I forgot") }
            }
        }
    }
}

/**
 * The day's calories, with the caveat attached rather than in a footnote.
 *
 * Assembled from typical servings, a day's total is easily ±20%. It's worth
 * having for "dinner was twice lunch" and useless for anything needing to be
 * right, so the number says which one it is.
 */
@Composable
private fun DayTotal(state: UiState) {
    val meals = state.timeline.filterIsInstance<DayEvent.Meal>().map { it.entry }
    if (meals.isEmpty()) return

    val each = meals.map { state.energyOf(it) }
    val known = each.filter { it.isKnown }
    if (known.isEmpty()) return

    val total = known.sumOf { it.kcal!! }
    val estimated = known.any { !it.isExact }
    val missing = each.count { !it.isKnown } + known.sumOf { it.unknownItems }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("${state.energyUnit.label} today", style = MaterialTheme.typography.titleSmall)
            Text(
                when {
                    missing > 0 -> "Estimated, and $missing thing${if (missing == 1) "" else "s"} had no data."
                    estimated -> "Estimated from usual servings — treat it as roughly right."
                    else -> "From amounts you gave."
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val unit = state.energyUnit
        val shown = unit.from(total)
        val step = unit.estimateStep
        Text(
            if (estimated || missing > 0) {
                "~${Math.round(shown / step) * step}"
            } else {
                "${Math.round(shown)}"
            },
            style = MaterialTheme.typography.titleMedium,
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun DayLogCard(state: UiState, onOpen: () -> Unit) {
    val day = state.dayLog
    val summary = buildList {
        day.sleepHours?.let { add("slept ${formatAmountShort(it)}h") }
        day.sleepQuality?.let { add("sleep ${it.label.lowercase()}") }
        day.stress?.let { add("stress ${it.label.lowercase()}") }
        day.exercise?.let { add(it.label.lowercase() + " exercise") }
        day.cyclePhase?.let { add(it.label.lowercase()) }
        if (day.unwell) add("unwell")
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(12.dp).clickable(onClick = onOpen),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("Sleep, stress and the rest", style = MaterialTheme.typography.titleSmall)
            Text(
                // Saying what these are *for* here, once, is worth it: they look
                // like padding until you know the ranking needs them.
                summary.joinToString(" · ").ifBlank {
                    "Not filled in. Without these, a bad night's sleep gets blamed on dinner."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatAmountShort(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
