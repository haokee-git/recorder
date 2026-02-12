package org.haokee.recorder.ui.component

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
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
    val view = LocalView.current
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

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // First row: Checkbox, Title + Waveform, Play button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Checkbox
                    AnimatedCheckbox(
                        isSelected = isSelected,
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onCheckboxClick()
                        }
                    )

                    // Title and Waveform
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
                        WaveformView(
                            audioPath = thought.audioPath,
                            cachedWaveform = thought.waveformData,
                            progress = playbackProgress,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Play button
                    IconButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onPlayClick()
                        },
                        enabled = !isRecording
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "暂停" else "播放"
                        )
                    }
                }

                // Content text (和标题左端对齐)
                Text(
                    text = thought.content ?: "",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 36.dp)
                )

                // Time section (和标题左端对齐)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 36.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Alarm time (如果有，居左)
                    thought.alarmTime?.let { alarmTime ->
                        val isExpired = alarmTime.isBefore(java.time.LocalDateTime.now())
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Alarm,
                                contentDescription = "提醒时间",
                                modifier = Modifier.size(14.dp),
                                tint = if (isExpired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = alarmTime.toDisplayString() + if (isExpired) " (已过期)" else "",
                                fontSize = 12.sp,
                                color = if (isExpired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Created time (居右)
                    Text(
                        text = thought.createdAt.toDisplayString(),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.End)
                    )
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
    val view = LocalView.current
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

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // First row: Checkbox, Title + Waveform, Play button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Checkbox
                    AnimatedCheckbox(
                        isSelected = isSelected,
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onCheckboxClick()
                        }
                    )

                    // Title and Waveform
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
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
                                    fontSize = 9.sp,
                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.dp)
                                )
                            }
                        }
                        WaveformView(
                            audioPath = thought.audioPath,
                            cachedWaveform = thought.waveformData,
                            progress = playbackProgress,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Play button
                    IconButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onPlayClick()
                        },
                        enabled = !isRecording
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "暂停" else "播放"
                        )
                    }
                }

                // Time section (和标题左端对齐)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 36.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Alarm time (如果有，居左)
                    thought.alarmTime?.let { alarmTime ->
                        val isExpired = alarmTime.isBefore(java.time.LocalDateTime.now())
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Alarm,
                                contentDescription = "提醒时间",
                                modifier = Modifier.size(14.dp),
                                tint = if (isExpired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = alarmTime.toDisplayString() + if (isExpired) " (已过期)" else "",
                                fontSize = 12.sp,
                                color = if (isExpired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Created time (居右)
                    Text(
                        text = thought.createdAt.toDisplayString(),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.End)
                    )
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
    val view = LocalView.current
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

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // First row: Checkbox, Title + Waveform, Play button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Checkbox
                    AnimatedCheckbox(
                        isSelected = isSelected,
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onCheckboxClick()
                        }
                    )

                    // Title and Waveform
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
                        WaveformView(
                            audioPath = thought.audioPath,
                            cachedWaveform = thought.waveformData,
                            progress = playbackProgress,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Play button
                    IconButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onPlayClick()
                        }
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "暂停" else "播放",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Content text (if transcribed) (和标题左端对齐)
                if (thought.isTranscribed && thought.content != null) {
                    Text(
                        text = thought.content,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(start = 36.dp)
                    )
                }

                // Time section (和标题左端对齐)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 36.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Alarm time (已过期，显示红色，居左)
                    thought.alarmTime?.let { alarmTime ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Alarm,
                                contentDescription = "提醒时间",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                            )
                            Text(
                                text = alarmTime.toDisplayString() + " (已过期)",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                            )
                        }
                    }

                    // Created time (居右)
                    Text(
                        text = thought.createdAt.toDisplayString(),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.End)
                    )
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

    // Save primary color for use in Canvas and border
    val primaryColor = MaterialTheme.colorScheme.primary

    // Border width is always 1.dp
    val borderWidth = 1.dp

    // Border color changes based on selection
    val borderColor = if (isSelected) primaryColor else MaterialTheme.colorScheme.outline

    // Animate check progress (0 to 1)
    val checkProgress by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "checkProgress"
    )

    Box(
        modifier = modifier
            .size(24.dp)
            .border(
                width = borderWidth,
                color = borderColor,
                shape = RoundedCornerShape(cornerRadius)
            )
            .clip(RoundedCornerShape(cornerRadius))
            .background(if (isSelected) primaryColor else Color.Transparent)
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
