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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.haokee.recorder.data.model.Thought
import org.haokee.recorder.data.model.ThoughtColor
import org.haokee.recorder.util.extension.toDisplayString

@Composable
fun TranscribedThoughtItem(
    thought: Thought,
    isSelected: Boolean,
    isPlaying: Boolean,
    playbackProgress: Float = 0f,
    isRecording: Boolean = false,
    onClick: () -> Unit,
    onCheckboxClick: () -> Unit,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .then(
                if (isSelected) Modifier.border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = MaterialTheme.shapes.medium
                ) else Modifier
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box {
            // Color triangle in top-left corner
            thought.color?.let { color ->
                ColorTriangle(
                    color = color,
                    modifier = Modifier.align(Alignment.TopStart)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Checkbox on the left
                AnimatedCheckbox(
                    isSelected = isSelected,
                    onClick = onCheckboxClick
                )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = thought.title ?: "无标题",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = thought.content ?: "",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Waveform visualization
                WaveformView(
                    audioPath = thought.audioPath,
                    progress = if (isPlaying) playbackProgress else 0f,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = thought.createdAt.toDisplayString(),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    thought.alarmTime?.let {
                        Text(
                            text = "提醒: ${it.toDisplayString()}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPlayClick,
                    enabled = !isRecording
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "暂停" else "播放"
                    )
                }
            }
        }
        }
    }
}

@Composable
fun OriginalThoughtItem(
    thought: Thought,
    isSelected: Boolean,
    isPlaying: Boolean,
    playbackProgress: Float = 0f,
    isRecording: Boolean = false,
    onClick: () -> Unit,
    onCheckboxClick: () -> Unit,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .then(
                if (isSelected) Modifier.border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = MaterialTheme.shapes.medium
                ) else Modifier
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box {
            // Color triangle in top-left corner
            thought.color?.let { color ->
                ColorTriangle(
                    color = color,
                    modifier = Modifier.align(Alignment.TopStart)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Checkbox on the left
                AnimatedCheckbox(
                    isSelected = isSelected,
                    onClick = onCheckboxClick
                )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "原始录音",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "未转换",
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Waveform visualization
                WaveformView(
                    audioPath = thought.audioPath,
                    progress = if (isPlaying) playbackProgress else 0f,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = thought.createdAt.toDisplayString(),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPlayClick,
                    enabled = !isRecording
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "暂停" else "播放"
                    )
                }
            }
        }
        }
    }
}

@Composable
fun ExpiredThoughtItem(
    thought: Thought,
    isSelected: Boolean,
    isPlaying: Boolean,
    playbackProgress: Float = 0f,
    isRecording: Boolean = false,
    onClick: () -> Unit,
    onCheckboxClick: () -> Unit,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .then(
                if (isSelected) Modifier.border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = MaterialTheme.shapes.medium
                ) else Modifier
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box {
            // Color triangle in top-left corner
            thought.color?.let { color ->
                ColorTriangle(
                    color = color,
                    modifier = Modifier.align(Alignment.TopStart)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Checkbox on the left
                Box(
                    modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(6.dp)
                    )
                    .clickable(onClick = onCheckboxClick),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "已选中",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (thought.isTranscribed) thought.title ?: "无标题" else "原始录音",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (thought.isTranscribed && thought.content != null) {
                    Text(
                        text = thought.content,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Waveform visualization
                WaveformView(
                    audioPath = thought.audioPath,
                    progress = if (isPlaying) playbackProgress else 0f,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = thought.createdAt.toDisplayString(),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    thought.alarmTime?.let {
                        Text(
                            text = "已过期: ${it.toDisplayString()}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPlayClick) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "暂停" else "播放",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        }
    }
}

@Composable
private fun ColorTriangle(
    color: ThoughtColor,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(32.dp)) {
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, 0f)
            lineTo(0f, size.height)
            close()
        }
        drawPath(
            path = path,
            color = color.color
        )
    }
}

@Composable
private fun AnimatedCheckbox(
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Animate corner radius: circle (12.dp) to rounded rect (6.dp)
    val cornerRadius by animateDpAsState(
        targetValue = if (isSelected) 6.dp else 12.dp,
        animationSpec = tween(durationMillis = 200),
        label = "cornerRadius"
    )

    // Animate border width: 0.dp to 1.dp
    val borderWidth by animateDpAsState(
        targetValue = if (isSelected) 1.dp else 0.dp,
        animationSpec = tween(durationMillis = 200),
        label = "borderWidth"
    )

    // Animate check progress (0 to 1)
    val checkProgress by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "checkProgress"
    )

    // Save primary color for use in Canvas
    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .size(24.dp)
            .then(
                if (borderWidth > 0.dp) Modifier.border(
                    width = borderWidth,
                    color = primaryColor,
                    shape = RoundedCornerShape(cornerRadius)
                ) else Modifier
            )
            .clip(RoundedCornerShape(cornerRadius))
            .background(if (isSelected) primaryColor else Color.White)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Animated check mark from left to right
        Canvas(modifier = Modifier.size(16.dp)) {
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
                            width = 2.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
        }
    }
}
