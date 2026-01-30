package org.haokee.recorder.audio.whisper

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min

/**
 * Audio Decoder - Converts M4A/MP4 audio files to PCM samples
 *
 * Uses Android MediaCodec to decode compressed audio to raw PCM data
 * suitable for speech recognition.
 */
object AudioDecoder {

    private const val TAG = "AudioDecoder"
    private const val TARGET_SAMPLE_RATE = 16000  // Whisper expects 16kHz
    private const val TARGET_CHANNELS = 1  // Mono

    /**
     * Decode audio file to float array of PCM samples
     *
     * @param filePath Path to audio file (M4A, MP3, WAV, etc.)
     * @return Float array of normalized PCM samples (-1.0 to 1.0), mono, 16kHz
     */
    fun decodeToFloatArray(filePath: String): FloatArray {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        val samples = mutableListOf<Float>()

        try {
            extractor.setDataSource(filePath)

            // Find audio track
            val trackIndex = findAudioTrack(extractor)
            if (trackIndex < 0) {
                Log.e(TAG, "No audio track found in file: $filePath")
                return FloatArray(0)
            }

            extractor.selectTrack(trackIndex)
            val format = extractor.getTrackFormat(trackIndex)

            // Get audio parameters
            val mimeType = format.getString(MediaFormat.KEY_MIME) ?: ""
            val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

            Log.d(TAG, "Audio format: $mimeType, $sampleRate Hz, $channelCount channels")

            // Create decoder
            codec = MediaCodec.createDecoderByType(mimeType)
            codec.configure(format, null, null, 0)
            codec.start()

            val bufferInfo = MediaCodec.BufferInfo()
            var isEOS = false

            while (!isEOS) {
                // Feed input
                val inputBufferId = codec.dequeueInputBuffer(10000)
                if (inputBufferId >= 0) {
                    val inputBuffer = codec.getInputBuffer(inputBufferId)
                    if (inputBuffer != null) {
                        val sampleSize = extractor.readSampleData(inputBuffer, 0)
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(inputBufferId, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        } else {
                            val presentationTimeUs = extractor.sampleTime
                            codec.queueInputBuffer(inputBufferId, 0, sampleSize, presentationTimeUs, 0)
                            extractor.advance()
                        }
                    }
                }

                // Get output
                val outputBufferId = codec.dequeueOutputBuffer(bufferInfo, 10000)
                if (outputBufferId >= 0) {
                    val outputBuffer = codec.getOutputBuffer(outputBufferId)
                    if (outputBuffer != null && bufferInfo.size > 0) {
                        // Convert PCM to float samples
                        val pcmSamples = extractPCMSamples(outputBuffer, bufferInfo.size, channelCount)
                        samples.addAll(pcmSamples)
                    }

                    codec.releaseOutputBuffer(outputBufferId, false)

                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        isEOS = true
                    }
                }
            }

            // Convert to array
            val floatArray = samples.toFloatArray()

            // Resample if necessary
            val resampledArray = if (sampleRate != TARGET_SAMPLE_RATE) {
                resample(floatArray, sampleRate, TARGET_SAMPLE_RATE)
            } else {
                floatArray
            }

            Log.d(TAG, "Decoded ${resampledArray.size} samples at $TARGET_SAMPLE_RATE Hz")
            return resampledArray

        } catch (e: Exception) {
            Log.e(TAG, "Error decoding audio file", e)
            return FloatArray(0)
        } finally {
            codec?.stop()
            codec?.release()
            extractor.release()
        }
    }

    /**
     * Find audio track index in MediaExtractor
     */
    private fun findAudioTrack(extractor: MediaExtractor): Int {
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
            if (mime.startsWith("audio/")) {
                return i
            }
        }
        return -1
    }

    /**
     * Extract PCM samples from output buffer and convert to float
     * Automatically converts stereo to mono by averaging channels
     */
    private fun extractPCMSamples(buffer: ByteBuffer, size: Int, channelCount: Int): List<Float> {
        val samples = mutableListOf<Float>()
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        buffer.position(0)

        val sampleCount = size / 2 / channelCount  // 16-bit samples

        for (i in 0 until sampleCount) {
            if (channelCount == 1) {
                // Mono: read single sample
                val sample = buffer.short.toFloat() / 32768f  // Normalize to -1.0 to 1.0
                samples.add(sample)
            } else {
                // Stereo: average left and right channels
                var sum = 0f
                for (ch in 0 until channelCount) {
                    sum += buffer.short.toFloat()
                }
                val avgSample = (sum / channelCount) / 32768f  // Normalize
                samples.add(avgSample)
            }
        }

        return samples
    }

    /**
     * Simple linear interpolation resampling
     * This is a basic implementation - for production use, consider using a better resampling algorithm
     */
    private fun resample(input: FloatArray, inputRate: Int, outputRate: Int): FloatArray {
        if (inputRate == outputRate) return input

        val ratio = inputRate.toFloat() / outputRate
        val outputLength = (input.size / ratio).toInt()
        val output = FloatArray(outputLength)

        for (i in output.indices) {
            val srcIndex = i * ratio
            val srcIndexInt = srcIndex.toInt()
            val fraction = srcIndex - srcIndexInt

            output[i] = if (srcIndexInt + 1 < input.size) {
                // Linear interpolation
                input[srcIndexInt] * (1 - fraction) + input[srcIndexInt + 1] * fraction
            } else {
                input[min(srcIndexInt, input.size - 1)]
            }
        }

        Log.d(TAG, "Resampled from $inputRate Hz to $outputRate Hz (${input.size} -> ${output.size} samples)")
        return output
    }
}
