package dev.mahourigan.fooddiary.ui

import dev.mahourigan.fooddiary.domain.Severity
import dev.mahourigan.fooddiary.domain.StoolEntry
import dev.mahourigan.fooddiary.domain.SymptomGroup
import dev.mahourigan.fooddiary.domain.SymptomType
import dev.mahourigan.fooddiary.domain.SymptomTypes
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import java.time.LocalDateTime

/**
 * Logging a symptom, both ways round.
 *
 * *"I'm bloated — start timing"* and *"I got bloated about half four and it
 * lasted a couple of hours"* are the same two timestamps underneath, so neither
 * is a second-class entry and nothing downstream has to know which was used.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogSymptomScreen(
    state: UiState,
    onStartNow: (SymptomType, Severity, String) -> Unit,
    onRecord: (SymptomType, Severity, LocalDateTime, LocalDateTime?, String) -> Unit,
    onCancel: () -> Unit,
) {
    var type by remember { mutableStateOf<SymptomType?>(null) }
    var severity by remember { mutableStateOf<Severity?>(null) }
    var overAlready by remember { mutableStateOf(false) }
    var startedAt by remember { mutableStateOf(LocalDateTime.now()) }
    var endedAt by remember { mutableStateOf<LocalDateTime?>(null) }
    var notes by remember { mutableStateOf("") }

    val ready = type != null && severity != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Log a symptom") },
                navigationIcon = {
                    IconButton(onClick = onCancel) { Icon(Icons.Default.Close, contentDescription = "Cancel") }
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
            SymptomTypes.byGroup().forEach { (group, infos) ->
                Text(group.label, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    infos.forEach { info ->
                        FilterChip(
                            selected = type == info.type,
                            onClick = { type = info.type },
                            label = { Text(info.label) },
                        )
                    }
                }
                if (group == SymptomGroup.BODY) {
                    Text(
                        // Worth one line: people log gut symptoms and skip these,
                        // and these are frequently the clearer signal.
                        "Often the clearer signal — bloating is noisy, a reliable next-day crash isn't.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            Text("How bad", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            SeverityPicker(selected = severity, onSelect = { severity = it })

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = !overAlready,
                    onClick = { overAlready = false },
                    label = { Text("Happening now") },
                )
                FilterChip(
                    selected = overAlready,
                    onClick = {
                        overAlready = true
                        endedAt = null
                    },
                    label = { Text("Fill in afterwards") },
                )
            }

            Spacer(Modifier.height(12.dp))

            if (overAlready) {
                TimeEditRow(
                    at = startedAt,
                    today = state.now.toLocalDate(),
                    onChange = { startedAt = it },
                    label = "Started",
                )

                Spacer(Modifier.height(10.dp))
                Text("Lasted", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    // Duration chips only ever compute an end time. A duration is
                    // never stored, so an edited end time can't disagree with it.
                    listOf(
                        "30 min" to 30L,
                        "1 hour" to 60L,
                        "2 hours" to 120L,
                        "4 hours" to 240L,
                        "All evening" to 300L,
                        "Still going" to -1L,
                    ).forEach { (label, minutes) ->
                        val selected = if (minutes < 0) {
                            endedAt == null
                        } else {
                            endedAt == startedAt.plusMinutes(minutes)
                        }
                        FilterChip(
                            selected = selected,
                            onClick = { endedAt = if (minutes < 0) null else startedAt.plusMinutes(minutes) },
                            label = { Text(label) },
                        )
                    }
                }

                endedAt?.let { end ->
                    Spacer(Modifier.height(8.dp))
                    TimeEditRow(
                        at = end,
                        today = state.now.toLocalDate(),
                        onChange = { endedAt = it },
                        label = "Ended",
                    )
                    if (end.isBefore(startedAt)) {
                        Text(
                            // Rather than refusing the entry: an end before the
                            // start almost always means it ran past midnight.
                            "That's before the start. If it ran overnight, move the end date forward a day.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            } else {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Text(
                        "The clock starts now and keeps running — across midnight and across days if it " +
                            "comes to that. Close it from the day view when it stops.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    val chosenType = type ?: return@Button
                    val chosenSeverity = severity ?: return@Button
                    if (overAlready) {
                        onRecord(chosenType, chosenSeverity, startedAt, endedAt, notes)
                    } else {
                        onStartNow(chosenType, chosenSeverity, notes)
                    }
                },
                enabled = ready,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (overAlready) "Save" else "Start timing")
            }
        }
    }
}

/** Bristol, urgency, time. Nothing else — this is a thirty-second job. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogStoolScreen(
    state: UiState,
    onSave: (Int, Severity?, LocalDateTime, String) -> Unit,
    onCancel: () -> Unit,
) {
    var bristol by remember { mutableStateOf(4) }
    var urgency by remember { mutableStateOf<Severity?>(null) }
    var at by remember { mutableStateOf(LocalDateTime.now()) }
    var notes by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Log a movement") },
                navigationIcon = {
                    IconButton(onClick = onCancel) { Icon(Icons.Default.Close, contentDescription = "Cancel") }
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
            TimeEditRow(at = at, today = state.now.toLocalDate(), onChange = { at = it })

            Spacer(Modifier.height(14.dp))
            Text("Bristol scale", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(6.dp))
            (1..7).forEach { type ->
                OutlinedButton(
                    onClick = { bristol = type },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                ) {
                    Text(
                        "$type — ${StoolEntry.describe(type)}",
                        style = if (bristol == type) {
                            MaterialTheme.typography.bodyMedium
                        } else {
                            MaterialTheme.typography.bodySmall
                        },
                        color = if (bristol == type) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Text("Urgency, if any", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            SeverityPicker(selected = urgency, onSelect = { urgency = if (urgency == it) null else it })

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))
            Button(onClick = { onSave(bristol, urgency, at, notes) }, modifier = Modifier.fillMaxWidth()) {
                Text("Save")
            }
        }
    }
}
