package org.haokee.recorder.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.haokee.recorder.data.model.ThoughtColor

@Composable
fun ColorFilterDialog(
    selectedColors: List<ThoughtColor>,
    onDismiss: () -> Unit,
    onColorsSelected: (List<ThoughtColor>) -> Unit
) {
    var currentSelection by remember { mutableStateOf(selectedColors.toSet()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("筛选颜色")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Quick actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            currentSelection = ThoughtColor.entries.toSet()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("全选")
                    }
                    OutlinedButton(
                        onClick = {
                            currentSelection = emptySet()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("清除")
                    }
                }

                // Color grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ThoughtColor.entries.take(4).forEach { color ->
                        FilterColorCircle(
                            color = color,
                            isSelected = color in currentSelection,
                            onClick = {
                                currentSelection = if (color in currentSelection) {
                                    currentSelection - color
                                } else {
                                    currentSelection + color
                                }
                            }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ThoughtColor.entries.drop(4).forEach { color ->
                        FilterColorCircle(
                            color = color,
                            isSelected = color in currentSelection,
                            onClick = {
                                currentSelection = if (color in currentSelection) {
                                    currentSelection - color
                                } else {
                                    currentSelection + color
                                }
                            }
                        )
                    }
                }

                Text(
                    text = if (currentSelection.isEmpty())
                        "未选择（显示所有）"
                    else
                        "已选择 ${currentSelection.size} 种颜色",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onColorsSelected(currentSelection.toList())
                    onDismiss()
                }
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun FilterColorCircle(
    color: ThoughtColor,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(color.color)
            .then(
                if (isSelected) Modifier.border(
                    width = 3.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                ) else Modifier.border(
                    width = 1.dp,
                    color = Color.Gray.copy(alpha = 0.3f),
                    shape = CircleShape
                )
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "已选择",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
