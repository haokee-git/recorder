package org.haokee.recorder.audio.player

import android.media.MediaPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

data class PlaybackState(
    val isPlaying: Boolean = false,
    val currentThoughtId: String? = null,
    val duration: Int = 0,
    val currentPosition: Int = 0
)

class AudioPlayer {
    private var mediaPlayer: MediaPlayer? = null

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    fun play(thoughtId: String, audioFile: File): Result<Unit> {
        return try {
            // Stop current playback if playing
            stop()

            mediaPlayer = MediaPlayer().apply {
                setDataSource(audioFile.absolutePath)
                prepare()
                setOnCompletionListener {
                    // Reset state when playback completes
                    _playbackState.value = PlaybackState(
                        isPlaying = false,
                        currentThoughtId = null,
                        duration = 0,
                        currentPosition = 0
                    )
                    // Release the media player
                    release()
                    mediaPlayer = null
                }
                start()
            }

            _playbackState.value = PlaybackState(
                isPlaying = true,
                currentThoughtId = thoughtId,
                duration = mediaPlayer?.duration ?: 0,
                currentPosition = 0
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun pause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                _playbackState.value = _playbackState.value.copy(
                    isPlaying = false,
                    currentPosition = it.currentPosition
                )
            }
        }
    }

    fun resume() {
        mediaPlayer?.let {
            if (!it.isPlaying) {
                it.start()
                _playbackState.value = _playbackState.value.copy(isPlaying = true)
            }
        }
    }

    fun stop() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        mediaPlayer = null
        _playbackState.value = PlaybackState()
    }

    fun updateProgress() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                _playbackState.value = _playbackState.value.copy(
                    currentPosition = it.currentPosition
                )
            }
        }
    }

    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
        _playbackState.value = PlaybackState()
    }
}
