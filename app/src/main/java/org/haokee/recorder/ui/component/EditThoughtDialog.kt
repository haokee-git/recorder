package org.haokee.recorder.ui.component

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import org.haokee.recorder.data.model.Thought

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditThoughtDialog(
    thought: Thought,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    val view = LocalView.current
    var title by remember { mutableStateOf(thought.title ?: "") }
    var content by remember { mutableStateOf(thought.content ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("编辑感言")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("标题") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("内容") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    maxLines = 10
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onSave(title, content)
                    onDismiss()
                },
                enabled = title.isNotBlank() || content.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            Button(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("取消")
            }
        }
    )
}
