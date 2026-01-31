package org.haokee.recorder.ui.component

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmTimePickerDialog(
    onDismiss: () -> Unit,
    onTimeSelected: (LocalDateTime) -> Unit
) {
    val view = LocalView.current
    val currentTime = LocalDateTime.now()
    var selectedDate by remember { mutableStateOf(currentTime.toLocalDate()) }
    var selectedHour by remember { mutableStateOf(currentTime.hour) }
    var selectedMinute by remember { mutableStateOf(currentTime.minute) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置提醒时间") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "选择提醒时间",
                    style = MaterialTheme.typography.bodyLarge
                )

                // Simple time picker
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Hour selector
                    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        Text("小时", style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(
                            value = selectedHour.toString(),
                            onValueChange = { value ->
                                value.toIntOrNull()?.let {
                                    if (it in 0..23) selectedHour = it
                                }
                            },
                            modifier = Modifier.width(80.dp),
                            singleLine = true
                        )
                    }

                    Text(":", style = MaterialTheme.typography.headlineMedium)

                    // Minute selector
                    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        Text("分钟", style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(
                            value = selectedMinute.toString().padStart(2, '0'),
                            onValueChange = { value ->
                                value.toIntOrNull()?.let {
                                    if (it in 0..59) selectedMinute = it
                                }
                            },
                            modifier = Modifier.width(80.dp),
                            singleLine = true
                        )
                    }
                }

                Text(
                    "提醒时间: ${selectedHour}:${selectedMinute.toString().padStart(2, '0')}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    val alarmTime = LocalDateTime.of(
                        selectedDate,
                        java.time.LocalTime.of(selectedHour, selectedMinute)
                    )
                    onTimeSelected(alarmTime)
                    onDismiss()
                }
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onDismiss()
                }
            ) {
                Text("取消")
            }
        }
    )
}
