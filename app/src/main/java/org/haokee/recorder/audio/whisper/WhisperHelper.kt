package org.haokee.recorder.audio.whisper

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import com.k2fsa.sherpa.onnx.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Whisper Helper - Offline speech recognition using Whisper tiny model
 *
 * This helper class encapsulates the sherpa-onnx library to provide
 * local, offline speech-to-text capabilities using OpenAI's Whisper model.
 *
 * Requirements:
 * 1. AAR file: sherpa-onnx-static-link-onnxruntime-1.12.23.aar in app/libs/
 * 2. Model files in app/src/main/assets/models/whisper-tiny/:
 *    - tiny-encoder.int8.onnx
 *    - tiny-decoder.int8.onnx
 *    - tiny-tokens.txt
 *
 * Download links:
 * - AAR: https://github.com/k2-fsa/sherpa-onnx/releases/tag/v1.12.23
 * - Models: https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-whisper-tiny.tar.bz2
 *
 * Note: Using multilingual model to support Chinese and English mixed speech recognition
 */
class WhisperHelper(private val context: Context) {

    companion object {
        private const val TAG = "WhisperHelper"
        private const val MODEL_DIR = "models/whisper-tiny"
        // Multilingual model files (supports Chinese and English)
        private const val ENCODER_FILE = "tiny-encoder.int8.onnx"
        private const val DECODER_FILE = "tiny-decoder.int8.onnx"
        private const val TOKENS_FILE = "tiny-tokens.txt"

        @Volatile
        private var instance: WhisperHelper? = null

        fun getInstance(context: Context): WhisperHelper {
            return instance ?: synchronized(this) {
                instance ?: WhisperHelper(context.applicationContext).also { instance = it }
            }
        }
    }

    private var recognizer: OfflineRecognizer? = null
    private val isInitialized: Boolean
        get() = recognizer != null

    /**
     * Initialize the Whisper recognizer
     * This should be called once during app startup or before first use
     */
    suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (isInitialized) {
                return@withContext Result.success(Unit)
            }

            Log.d(TAG, "Initializing Whisper recognizer...")

            // Check if model files exist in assets
            val assetManager = context.assets
            if (!checkModelFiles(assetManager)) {
                return@withContext Result.failure(
                    Exception("Model files not found in assets/$MODEL_DIR. Please download and place the model files.")
                )
            }

            // Configure Whisper model
            val whisperConfig = OfflineWhisperModelConfig(
                encoder = "$MODEL_DIR/$ENCODER_FILE",
                decoder = "$MODEL_DIR/$DECODER_FILE",
                language = "zh",  // Chinese language (also recognizes English in mixed speech)
                task = "transcribe"
            )

            val modelConfig = OfflineModelConfig(
                whisper = whisperConfig,
                tokens = "$MODEL_DIR/$TOKENS_FILE",
                numThreads = 2,
                debug = false,
                modelType = "whisper"
            )

            val config = OfflineRecognizerConfig(
                modelConfig = modelConfig,
                decodingMethod = "greedy_search"
            )

            // Create recognizer from assets
            recognizer = OfflineRecognizer(
                assetManager = assetManager,
                config = config
            )

            Log.d(TAG, "Whisper recognizer initialized successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Whisper recognizer", e)
            Result.failure(e)
        }
    }

    /**
     * Transcribe an audio file to text
     *
     * @param audioPath Path to the audio file (M4A/WAV format)
     * @return Transcribed text, or error message
     */
    suspend fun transcribe(audioPath: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!isInitialized) {
                return@withContext Result.failure(Exception("Whisper recognizer not initialized"))
            }

            Log.d(TAG, "Transcribing audio file: $audioPath")

            // Check if file exists
            val audioFile = File(audioPath)
            if (!audioFile.exists()) {
                return@withContext Result.failure(Exception("Audio file not found: $audioPath"))
            }

            // Read audio samples
            // Note: sherpa-onnx expects 16kHz mono audio
            val samples = readAudioSamples(audioPath)

            // Create stream and decode
            val stream = recognizer!!.createStream()
            stream.acceptWaveform(sampleRate = 16000, samples = samples)

            recognizer!!.decode(stream)
            val result = recognizer!!.getResult(stream)

            Log.d(TAG, "Transcription result: ${result.text}")
            Result.success(result.text)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to transcribe audio", e)
            Result.failure(e)
        }
    }

    /**
     * Release resources
     */
    fun release() {
        recognizer?.release()
        recognizer = null
        Log.d(TAG, "Whisper recognizer released")
    }

    /**
     * Check if all required model files exist in assets
     */
    private fun checkModelFiles(assetManager: AssetManager): Boolean {
        return try {
            val files = assetManager.list(MODEL_DIR) ?: return false
            files.contains(ENCODER_FILE) &&
                    files.contains(DECODER_FILE) &&
                    files.contains(TOKENS_FILE)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking model files", e)
            false
        }
    }

    /**
     * Read audio samples from file
     * Decodes M4A/MP3/WAV to 16kHz mono PCM float array
     */
    private fun readAudioSamples(audioPath: String): FloatArray {
        return AudioDecoder.decodeToFloatArray(audioPath)
    }
}
