package org.haokee.recorder.ui.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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

                // Clear button at bottom right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            currentSelection = emptySet()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("清除")
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
            Button(
                onClick = {
                    onColorsSelected(currentSelection.toList())
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
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
    // Animate corner radius: circle (28.dp) to rounded rect (8.dp)
    val cornerRadius by animateDpAsState(
        targetValue = if (isSelected) 8.dp else 28.dp,
        animationSpec = tween(durationMillis = 200),
        label = "cornerRadius"
    )

    // Animate check icon scale
    val checkScale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "checkScale"
    )

    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(cornerRadius))
            .background(color.color)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "已选择",
            tint = Color.White,
            modifier = Modifier
                .size(32.dp)
                .scale(checkScale)
        )
    }
}
