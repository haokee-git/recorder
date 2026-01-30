package org.haokee.recorder.ui.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.dp
import org.haokee.recorder.data.model.ThoughtColor

@Composable
fun ColorPickerDialog(
    currentColor: ThoughtColor?,
    onDismiss: () -> Unit,
    onColorSelected: (ThoughtColor?) -> Unit
) {
    var selectedColor by remember { mutableStateOf(currentColor) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("选择颜色")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Color grid - Row 1
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ThoughtColor.entries.take(4).forEach { color ->
                        PickerColorCircle(
                            color = color,
                            isSelected = selectedColor == color,
                            onClick = { selectedColor = color }
                        )
                    }
                }
                // Color grid - Row 2
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ThoughtColor.entries.drop(4).forEach { color ->
                        PickerColorCircle(
                            color = color,
                            isSelected = selectedColor == color,
                            onClick = { selectedColor = color }
                        )
                    }
                }
                // Color grid - Row 3 with "No Color" option
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // No color option
                    NoColorCircle(
                        isSelected = selectedColor == null,
                        onClick = { selectedColor = null }
                    )
                    // Empty placeholders to maintain grid alignment
                    Spacer(modifier = Modifier.size(56.dp))
                    Spacer(modifier = Modifier.size(56.dp))
                    Spacer(modifier = Modifier.size(56.dp))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onColorSelected(selectedColor)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
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

@Composable
private fun PickerColorCircle(
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

    // Inner corner radius should be smaller to match the border
    val innerCornerRadius = (cornerRadius - 1.dp).coerceAtLeast(0.dp)

    // Animate check progress (0 to 1)
    val checkProgress by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "checkProgress"
    )

    // Darken color for border
    val borderColor = remember(color) {
        Color(
            red = color.color.red * 0.7f,
            green = color.color.green * 0.7f,
            blue = color.color.blue * 0.7f
        )
    }

    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(cornerRadius))
            .background(borderColor)
            .padding(1.dp)
            .clip(RoundedCornerShape(innerCornerRadius))
            .background(color.color)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Animated check mark from left to right
        Canvas(modifier = Modifier.size(32.dp)) {
            if (checkProgress > 0f) {
                val checkPath = Path().apply {
                    // Check mark path
                    moveTo(size.width * 0.2f, size.height * 0.5f)
                    lineTo(size.width * 0.4f, size.height * 0.7f)
                    lineTo(size.width * 0.8f, size.height * 0.3f)
                }

                // Clip the path based on progress
                clipRect(
                    left = 0f,
                    top = 0f,
                    right = size.width * checkProgress,
                    bottom = size.height
                ) {
                    drawPath(
                        path = checkPath,
                        color = Color.White,
                        style = Stroke(
                            width = 4.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun NoColorCircle(
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // Animate corner radius: circle (28.dp) to rounded rect (8.dp)
    val cornerRadius by animateDpAsState(
        targetValue = if (isSelected) 8.dp else 28.dp,
        animationSpec = tween(durationMillis = 200),
        label = "cornerRadius"
    )

    // Inner corner radius should be smaller to match the border
    val innerCornerRadius = (cornerRadius - 1.dp).coerceAtLeast(0.dp)

    // Animate check progress (0 to 1)
    val checkProgress by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "checkProgress"
    )

    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color.Red)
            .padding(1.dp)
            .clip(RoundedCornerShape(innerCornerRadius))
            .background(Color.White)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Draw diagonal slash
        Canvas(modifier = Modifier.size(32.dp)) {
            drawLine(
                color = Color.Red,
                start = Offset(size.width * 0.2f, size.height * 0.2f),
                end = Offset(size.width * 0.8f, size.height * 0.8f),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Animated check mark from left to right
            if (checkProgress > 0f) {
                val checkPath = Path().apply {
                    // Check mark path
                    moveTo(size.width * 0.2f, size.height * 0.5f)
                    lineTo(size.width * 0.4f, size.height * 0.7f)
                    lineTo(size.width * 0.8f, size.height * 0.3f)
                }

                // Clip the path based on progress
                clipRect(
                    left = 0f,
                    top = 0f,
                    right = size.width * checkProgress,
                    bottom = size.height
                ) {
                    drawPath(
                        path = checkPath,
                        color = Color.Red,
                        style = Stroke(
                            width = 4.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
        }
    }
}
