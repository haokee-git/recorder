package org.haokee.recorder.audio.waveform

import android.media.MediaMetadataRetriever
import android.util.Log
import org.haokee.recorder.audio.whisper.AudioDecoder
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Waveform Extractor - Extract waveform data from audio files
 */
object WaveformExtractor {

    private const val TAG = "WaveformExtractor"

    /**
     * Get audio duration using MediaMetadataRetriever
     */
    fun getAudioDuration(audioPath: String): Long {
        val file = File(audioPath)
        if (!file.exists()) {
            Log.w(TAG, "Audio file does not exist: $audioPath")
            return 0L
        }

        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(audioPath)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val duration = durationStr?.toLongOrNull() ?: 0L
            Log.d(TAG, "Audio duration: $duration ms for $audioPath")
            duration
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get audio duration for $audioPath", e)
            0L
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to release retriever", e)
            }
        }
    }

    /**
     * Extract waveform from audio file using AudioDecoder
     */
    fun extractWaveform(audioPath: String, barCount: Int = 60): List<Float> {
        val file = File(audioPath)
        if (!file.exists()) {
            Log.e(TAG, "Audio file does not exist: $audioPath")
            return generateFallbackWaveform(barCount)
        }

        Log.d(TAG, "Extracting waveform for: $audioPath")

        return try {
            // Decode audio to PCM using existing AudioDecoder
            val pcmSamples = AudioDecoder.decodeToFloatArray(audioPath)

            if (pcmSamples.isEmpty()) {
                Log.e(TAG, "AudioDecoder returned empty array for $audioPath")
                return generateFallbackWaveform(barCount)
            }

            Log.d(TAG, "Decoded ${pcmSamples.size} PCM samples")

            // Extract waveform from PCM samples
            val waveform = extractWaveformFromPCM(pcmSamples, barCount)

            Log.d(TAG, "Generated waveform: ${waveform.take(10)} ... (${waveform.size} bars)")
            waveform

        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract waveform for $audioPath", e)
            generateFallbackWaveform(barCount)
        }
    }

    /**
     * Extract waveform from PCM samples
     */
    private fun extractWaveformFromPCM(pcmSamples: FloatArray, barCount: Int): List<Float> {
        val samplesPerBar = max(1, pcmSamples.size / barCount)
        val rawAmplitudes = mutableListOf<Float>()

        // First pass: collect raw amplitudes
        for (i in 0 until barCount) {
            val startIndex = i * samplesPerBar
            val endIndex = min(startIndex + samplesPerBar, pcmSamples.size)

            if (startIndex >= pcmSamples.size) {
                rawAmplitudes.add(rawAmplitudes.lastOrNull() ?: 0f)
                continue
            }

            // Find max absolute amplitude in this segment
            var maxAmplitude = 0f
            for (j in startIndex until endIndex) {
                val amplitude = abs(pcmSamples[j])
                if (amplitude > maxAmplitude) {
                    maxAmplitude = amplitude
                }
            }

            rawAmplitudes.add(maxAmplitude)
        }

        // Find global max for normalization
        val globalMax = rawAmplitudes.maxOrNull() ?: 1f

        // Second pass: normalize so max = 1.0, min visible = 0.2
        val normalized = rawAmplitudes.map { amp ->
            if (globalMax > 0f) {
                val ratio = amp / globalMax
                (ratio * 0.8f + 0.2f).coerceIn(0.2f, 1.0f)
            } else {
                0.2f
            }
        }

        Log.d(TAG, "Normalized waveform: max=$globalMax, first 5=${normalized.take(5)}")
        return normalized
    }

    /**
     * Generate fallback waveform (uniform)
     */
    private fun generateFallbackWaveform(barCount: Int): List<Float> {
        return List(barCount) { 0.5f }
    }
}
