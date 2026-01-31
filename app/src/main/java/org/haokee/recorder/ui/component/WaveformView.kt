package org.haokee.recorder.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.haokee.recorder.audio.waveform.WaveformExtractor
import java.io.File

@Composable
fun WaveformView(
    audioPath: String,
    cachedWaveform: String? = null, // 缓存的波形数据（CSV 格式）
    progress: Float = 0f, // 0.0 to 1.0
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Convert relative path to absolute path
    val fullPath = remember(audioPath) {
        if (audioPath.startsWith("/")) {
            audioPath // Already absolute
        } else {
            // Relative path, construct full path
            File(context.filesDir, "recordings/$audioPath").absolutePath
        }
    }

    // State for audio duration and waveform data
    var audioDuration by remember(fullPath) { mutableStateOf(0L) }
    var waveformData by remember(fullPath, cachedWaveform) { mutableStateOf<List<Float>>(emptyList()) }

    val durationText = formatDuration(audioDuration)

    // Load audio data asynchronously
    LaunchedEffect(fullPath, cachedWaveform) {
        val duration = withContext(Dispatchers.IO) {
            WaveformExtractor.getAudioDuration(fullPath)
        }

        val waveform = if (!cachedWaveform.isNullOrEmpty()) {
            // Use cached waveform
            try {
                cachedWaveform.split(",").map { it.toFloat() }
            } catch (e: Exception) {
                Log.e("WaveformView", "Failed to parse cached waveform", e)
                withContext(Dispatchers.IO) {
                    WaveformExtractor.extractWaveform(fullPath, 120)
                }
            }
        } else {
            // Extract new waveform
            withContext(Dispatchers.IO) {
                WaveformExtractor.extractWaveform(fullPath, 120)
            }
        }

        // Update on main thread
        audioDuration = duration
        waveformData = waveform
        Log.d("WaveformView", "UI received waveform: ${waveform.take(5)} (cached=${!cachedWaveform.isNullOrEmpty()})")
    }

    // Use default waveform while loading
    val displayWaveform = if (waveformData.isEmpty()) {
        List(120) { 0.5f }
    } else {
        waveformData
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Waveform canvas
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
        ) {
            val width = size.width
            val height = size.height
            val centerY = height / 2
            val barWidth = width / displayWaveform.size
            val progressX = width * progress

            displayWaveform.forEachIndexed { index, amplitude ->
                val x = index * barWidth
                val barHeight = amplitude * height * 0.9f // 增加到 90% 高度

                // Draw waveform bar
                val color = if (x <= progressX) {
                    Color(0xFF2196F3) // Blue for played portion
                } else {
                    Color.Gray.copy(alpha = 0.5f) // Gray for unplayed portion
                }

                drawLine(
                    color = color,
                    start = Offset(x + barWidth / 2, centerY - barHeight / 2),
                    end = Offset(x + barWidth / 2, centerY + barHeight / 2),
                    strokeWidth = (barWidth * 0.35f).coerceAtLeast(1f) // 减少到 35% 宽度
                )
            }

            // Draw progress indicator line
            if (progress > 0f) {
                drawLine(
                    color = Color(0xFF2196F3),
                    start = Offset(progressX, 0f),
                    end = Offset(progressX, height),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }

        // Duration text in "xx s/xx s" format (centered)
        val currentSeconds = if (progress > 0f) {
            ((audioDuration * progress) / 1000).toInt()
        } else {
            0
        }
        val totalSeconds = (audioDuration / 1000).toInt()

        Text(
            text = "${currentSeconds}s/${totalSeconds}s",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

/**
 * Format duration in milliseconds to MM:SS format
 */
private fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
