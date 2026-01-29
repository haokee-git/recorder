package org.haokee.recorder.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun WaveformView(
    audioPath: String,
    progress: Float = 0f, // 0.0 to 1.0
    modifier: Modifier = Modifier
) {
    // Get audio duration
    val audioDuration = getAudioDuration(audioPath)
    val durationText = formatDuration(audioDuration)

    // Generate pseudo-waveform data based on file hash for consistency
    val waveformData = remember(audioPath) {
        generatePseudoWaveform(audioPath, 60)
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
            val barWidth = width / waveformData.size
            val progressX = width * progress

            waveformData.forEachIndexed { index, amplitude ->
                val x = index * barWidth
                val barHeight = amplitude * height * 0.4f

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
                    strokeWidth = (barWidth * 0.7f).coerceAtLeast(2f)
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

        // Duration text
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (progress > 0f) {
                    formatDuration((audioDuration * progress).toLong())
                } else {
                    "00:00"
                },
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = durationText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Generate pseudo-waveform data based on audio file hash
 * This creates a consistent waveform appearance for the same file
 */
private fun generatePseudoWaveform(audioPath: String, barCount: Int): List<Float> {
    val file = File(audioPath)
    if (!file.exists()) {
        // Return default waveform if file doesn't exist
        return List(barCount) { 0.5f }
    }

    // Use file name hash as seed for consistent pseudo-random generation
    val seed = audioPath.hashCode()
    val random = Random(seed)

    return List(barCount) {
        // Generate amplitude between 0.2 and 1.0 with some smoothing
        val baseAmplitude = random.nextFloat() * 0.8f + 0.2f
        // Add some wave pattern for more natural look
        val wavePattern = abs(sin((it.toFloat() / barCount) * 6.28f)) * 0.3f
        (baseAmplitude + wavePattern).coerceIn(0.2f, 1.0f)
    }
}

/**
 * Get audio duration in milliseconds
 * For now, estimate based on file size (this is a simplification)
 * TODO: Use MediaMetadataRetriever for accurate duration
 */
private fun getAudioDuration(audioPath: String): Long {
    val file = File(audioPath)
    if (!file.exists()) return 0L

    // Estimate: AAC 128kbps ≈ 16KB/s
    // This is a rough estimate; actual implementation should use MediaMetadataRetriever
    val fileSizeKB = file.length() / 1024
    val estimatedSeconds = fileSizeKB / 16

    return estimatedSeconds * 1000
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

@Composable
private fun <T> remember(key1: Any?, calculation: () -> T): T {
    return androidx.compose.runtime.remember(key1) { calculation() }
}
