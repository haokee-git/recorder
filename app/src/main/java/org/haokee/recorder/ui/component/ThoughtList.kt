package org.haokee.recorder.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.haokee.recorder.data.model.Thought

@Composable
fun ThoughtList(
    transcribedThoughts: List<Thought>,
    originalThoughts: List<Thought>,
    expiredAlarmThoughts: List<Thought>,
    selectedThoughts: Set<String>,
    currentPlayingThoughtId: String?,
    isPlaying: Boolean,
    playbackProgress: Float = 0f,
    isRecording: Boolean = false,
    onThoughtClick: (Thought) -> Unit,
    onCheckboxClick: (Thought) -> Unit,
    onPlayClick: (Thought) -> Unit,
    modifier: Modifier = Modifier
) {
    if (transcribedThoughts.isEmpty() && originalThoughts.isEmpty() && expiredAlarmThoughts.isEmpty()) {
        EmptyState(modifier = modifier)
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Transcribed thoughts section
            if (transcribedThoughts.isNotEmpty()) {
                item {
                    SectionHeader("已转换感言")
                }
                items(
                    items = transcribedThoughts,
                    key = { it.id }
                ) { thought ->
                    TranscribedThoughtItem(
                        thought = thought,
                        isSelected = thought.id in selectedThoughts,
                        isPlaying = thought.id == currentPlayingThoughtId && isPlaying,
                        playbackProgress = if (thought.id == currentPlayingThoughtId) playbackProgress else 0f,
                        isRecording = isRecording,
                        onClick = { onThoughtClick(thought) },
                        onCheckboxClick = { onCheckboxClick(thought) },
                        onPlayClick = { onPlayClick(thought) }
                    )
                }
            }

            // Original thoughts section
            if (originalThoughts.isNotEmpty()) {
                item {
                    SectionHeader("原始感言")
                }
                items(
                    items = originalThoughts,
                    key = { it.id }
                ) { thought ->
                    OriginalThoughtItem(
                        thought = thought,
                        isSelected = thought.id in selectedThoughts,
                        isPlaying = thought.id == currentPlayingThoughtId && isPlaying,
                        playbackProgress = if (thought.id == currentPlayingThoughtId) playbackProgress else 0f,
                        isRecording = isRecording,
                        onClick = { onThoughtClick(thought) },
                        onCheckboxClick = { onCheckboxClick(thought) },
                        onPlayClick = { onPlayClick(thought) }
                    )
                }
            }

            // Expired alarm thoughts section
            if (expiredAlarmThoughts.isNotEmpty()) {
                item {
                    SectionHeader("闹钟已过的感言")
                }
                items(
                    items = expiredAlarmThoughts,
                    key = { it.id }
                ) { thought ->
                    ExpiredThoughtItem(
                        thought = thought,
                        isSelected = thought.id in selectedThoughts,
                        isPlaying = thought.id == currentPlayingThoughtId && isPlaying,
                        playbackProgress = if (thought.id == currentPlayingThoughtId) playbackProgress else 0f,
                        isRecording = isRecording,
                        onClick = { onThoughtClick(thought) },
                        onCheckboxClick = { onCheckboxClick(thought) },
                        onPlayClick = { onPlayClick(thought) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "还没有感言",
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "点击右下角按钮开始录音",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
