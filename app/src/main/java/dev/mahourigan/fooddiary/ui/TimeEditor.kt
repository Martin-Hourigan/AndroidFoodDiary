package dev.mahourigan.fooddiary.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * When something happened.
 *
 * Offsets first, picker second. Almost everything is logged a short while after
 * it happened — "I got bloated about two hours ago" — so two taps on **−2 h**
 * beats spinning a clock face, and the picker is there for the times it doesn't.
 */
@Composable
fun TimeEditRow(
    at: LocalDateTime,
    today: LocalDate,
    onChange: (LocalDateTime) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "At",
) {
    var picking by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("$label ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            AssistChip(
                onClick = { picking = true },
                label = { Text("${formatDayHeading(at.toLocalDate(), today)}, ${formatTime(at)}") },
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(15L, 60L, 120L, 180L).forEach { minutes ->
                TextButton(onClick = { onChange(at.minusMinutes(minutes)) }) {
                    Text(if (minutes < 60) "−${minutes}m" else "−${minutes / 60}h", style = MaterialTheme.typography.labelMedium)
                }
            }
            TextButton(onClick = { onChange(LocalDateTime.now()) }) {
                Text("Now", style = MaterialTheme.typography.labelMedium)
            }
        }
    }

    if (picking) {
        ClockDialog(
            initial = at,
            onDismiss = { picking = false },
            onPick = { hour, minute ->
                onChange(at.withHour(hour).withMinute(minute))
                picking = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClockDialog(
    initial: LocalDateTime,
    onDismiss: () -> Unit,
    onPick: (hour: Int, minute: Int) -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour = initial.hour,
        initialMinute = initial.minute,
        is24Hour = true,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onPick(state.hour, state.minute) }) { Text("Set") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        text = { TimePicker(state = state, modifier = Modifier.padding(top = 8.dp)) },
    )
}
