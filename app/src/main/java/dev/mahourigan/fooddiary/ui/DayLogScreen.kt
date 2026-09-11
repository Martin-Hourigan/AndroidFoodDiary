package dev.mahourigan.fooddiary.ui

import dev.mahourigan.fooddiary.domain.CyclePhase
import dev.mahourigan.fooddiary.domain.DayLog
import dev.mahourigan.fooddiary.domain.ExerciseLevel
import dev.mahourigan.fooddiary.domain.Severity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * The confounders.
 *
 * Every field optional, and an untouched day is stored as nothing rather than as
 * zeroes — the analysis has to be able to tell "didn't answer" from "no stress",
 * and a form that quietly defaults would destroy that distinction.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayLogScreen(
    state: UiState,
    onSave: (DayLog) -> Unit,
    onBack: () -> Unit,
) {
    val existing = state.dayLog
    var sleepHours by remember { mutableStateOf(existing.sleepHours?.let { trimZero(it) } ?: "") }
    var sleepQuality by remember { mutableStateOf(existing.sleepQuality) }
    var stress by remember { mutableStateOf(existing.stress) }
    var exercise by remember { mutableStateOf(existing.exercise) }
    var unwell by remember { mutableStateOf(existing.unwell) }
    var cyclePhase by remember { mutableStateOf(existing.cyclePhase) }
    var note by remember { mutableStateOf(existing.note) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(formatDayHeading(state.date, state.now.toLocalDate())) },
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
            Text(
                "Alcohol and caffeine aren't here — they're food, so they come in through the meal log.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = sleepHours,
                onValueChange = { sleepHours = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Hours slept") },
                singleLine = true,
            )

            Spacer(Modifier.height(14.dp))
            Text("Slept how well", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            SeverityPicker(selected = sleepQuality, onSelect = { sleepQuality = if (sleepQuality == it) null else it })

            Spacer(Modifier.height(14.dp))
            Text("Stress", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            SeverityPicker(selected = stress, onSelect = { stress = if (stress == it) null else it })

            Spacer(Modifier.height(14.dp))
            Text("Exercise", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ExerciseLevel.entries.forEach { level ->
                    FilterChip(
                        selected = exercise == level,
                        onClick = { exercise = if (exercise == level) null else level },
                        label = { Text(level.label, style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Text("Other", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = unwell, onClick = { unwell = !unwell }, label = { Text("Unwell") })
                CyclePhase.entries.forEach { phase ->
                    FilterChip(
                        selected = cyclePhase == phase,
                        onClick = { cyclePhase = if (cyclePhase == phase) null else phase },
                        label = { Text(phase.label) },
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Anything else about today") },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(18.dp))
            Button(
                onClick = {
                    onSave(
                        DayLog(
                            date = state.date,
                            sleepHours = sleepHours.toDoubleOrNull(),
                            sleepQuality = sleepQuality,
                            stress = stress,
                            exercise = exercise,
                            unwell = unwell,
                            cyclePhase = cyclePhase,
                            note = note,
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save") }
        }
    }
}

private fun trimZero(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
