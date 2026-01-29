package org.haokee.recorder.audio.recorder

import android.media.MediaRecorder
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

data class RecordingState(
    val isRecording: Boolean = false,
    val duration: Long = 0L,
    val currentFile: File? = null
)

class AudioRecorder {
    private var mediaRecorder: MediaRecorder? = null
    private var startTime: Long = 0L

    private val _recordingState = MutableStateFlow(RecordingState())
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    fun startRecording(outputFile: File): Result<Unit> {
        return try {
            @Suppress("DEPRECATION")
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }

            startTime = System.currentTimeMillis()
            _recordingState.value = RecordingState(
                isRecording = true,
                duration = 0L,
                currentFile = outputFile
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun stopRecording(): Result<File?> {
        return try {
            val file = _recordingState.value.currentFile
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null

            _recordingState.value = RecordingState(
                isRecording = false,
                duration = 0L,
                currentFile = null
            )

            Result.success(file)
        } catch (e: Exception) {
            mediaRecorder?.release()
            mediaRecorder = null
            _recordingState.value = RecordingState()
            Result.failure(e)
        }
    }

    fun updateDuration() {
        if (_recordingState.value.isRecording) {
            val duration = System.currentTimeMillis() - startTime
            _recordingState.value = _recordingState.value.copy(duration = duration)
        }
    }

    fun release() {
        mediaRecorder?.release()
        mediaRecorder = null
        _recordingState.value = RecordingState()
    }
}
