package org.haokee.recorder.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.haokee.recorder.audio.player.AudioPlayer
import org.haokee.recorder.audio.recognizer.SpeechToTextHelper
import org.haokee.recorder.audio.recorder.AudioRecorder
import org.haokee.recorder.data.model.Thought
import org.haokee.recorder.data.model.ThoughtColor
import org.haokee.recorder.data.repository.SettingsRepository
import org.haokee.recorder.data.repository.ThoughtRepository
import java.time.LocalDateTime
import java.util.UUID

data class ThoughtListUiState(
    val transcribedThoughts: List<Thought> = emptyList(),
    val originalThoughts: List<Thought> = emptyList(),
    val expiredAlarmThoughts: List<Thought> = emptyList(),
    val selectedThoughts: Set<String> = emptySet(),
    val isMultiSelectMode: Boolean = false,
    val selectedColors: List<ThoughtColor?> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val scrollToThoughtId: String? = null
)

class ThoughtListViewModel(
    private val context: Context,
    private val repository: ThoughtRepository,
    private val settingsRepository: SettingsRepository,
    val audioRecorder: AudioRecorder,
    val audioPlayer: AudioPlayer
) : ViewModel() {

    private val _uiState = MutableStateFlow(ThoughtListUiState())
    val uiState: StateFlow<ThoughtListUiState> = _uiState.asStateFlow()

    private val speechToTextHelper = SpeechToTextHelper.getInstance(context, settingsRepository)

    init {
        android.util.Log.d("ThoughtListViewModel", "Initializing ViewModel...")
        try {
            loadThoughts()
            startPlaybackProgressUpdater()
            initializeWhisper()
            startMinutelyRefresh()
            android.util.Log.d("ThoughtListViewModel", "ViewModel initialized successfully")
        } catch (e: Exception) {
            android.util.Log.e("ThoughtListViewModel", "Error during initialization", e)
            _uiState.update { it.copy(error = "初始化失败: ${e.message}") }
        }
    }

    private fun startMinutelyRefresh() {
        viewModelScope.launch {
            try {
                while (true) {
                    // 计算到下一个整分钟还有多少毫秒
                    val now = LocalDateTime.now()
                    val secondsUntilNextMinute = 60 - now.second
                    val nanosUntilNextMinute = 1_000_000_000 - now.nano
                    val millisUntilNextMinute = secondsUntilNextMinute * 1000L + nanosUntilNextMinute / 1_000_000

                    // 等待到下一个整分钟的第0秒
                    delay(millisUntilNextMinute)

                    // 刷新列表（闹钟过期的感言会自动移到"已过期"区域）
                    loadThoughts()
                }
            } catch (e: Exception) {
                // Catch any exceptions to prevent crash
                android.util.Log.e("ThoughtListViewModel", "Error in minutely refresh", e)
            }
        }
    }

    private fun initializeWhisper() {
        viewModelScope.launch {
            speechToTextHelper.initialize().onFailure { exception ->
                // Whisper initialization failed, but app can still work
                // User will see error message when trying to convert
                android.util.Log.w("ThoughtListViewModel", "Whisper initialization failed: ${exception.message}")
            }
        }
    }

    private fun startPlaybackProgressUpdater() {
        viewModelScope.launch {
            try {
                audioPlayer.playbackState.collect { playbackState ->
                    if (playbackState.isPlaying) {
                        // Update progress every 100ms while playing
                        while (audioPlayer.playbackState.value.isPlaying) {
                            delay(100)
                            audioPlayer.updateProgress()
                        }
                    }
                }
            } catch (e: Exception) {
                // Catch any exceptions to prevent crash
                android.util.Log.e("ThoughtListViewModel", "Error in playback progress updater", e)
            }
        }
    }

    private fun loadThoughts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            combine(
                repository.getTranscribedThoughts(),
                repository.getOriginalThoughts(),
                repository.getExpiredAlarmThoughts()
            ) { transcribed, original, expired ->
                Triple(transcribed, original, expired)
            }.catch { exception ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = exception.message ?: "Unknown error occurred"
                    )
                }
            }.collect { (transcribed, original, expired) ->
                _uiState.update {
                    it.copy(
                        transcribedThoughts = filterByColors(transcribed),
                        originalThoughts = filterByColors(original),
                        expiredAlarmThoughts = filterByColors(expired),
                        isLoading = false,
                        error = null
                    )
                }
            }
        }
    }

    private fun filterByColors(thoughts: List<Thought>): List<Thought> {
        val selectedColors = _uiState.value.selectedColors
        if (selectedColors.isEmpty()) return thoughts
        return thoughts.filter { thought ->
            thought.color in selectedColors
        }
    }

    fun startRecording() {
        viewModelScope.launch {
            // Stop playback if playing
            if (audioPlayer.playbackState.value.isPlaying) {
                audioPlayer.stop()
            }

            val fileName = "thought_${System.currentTimeMillis()}.m4a"
            val outputFile = repository.createAudioFile(fileName)

            audioRecorder.startRecording(outputFile).onFailure { exception ->
                _uiState.update {
                    it.copy(error = "Recording failed: ${exception.message}")
                }
            }
        }
    }

    fun stopRecording() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            audioRecorder.stopRecording().onSuccess { file ->
                file?.let {
                    // Extract waveform in background
                    val waveformData = withContext(Dispatchers.IO) {
                        try {
                            val waveform = org.haokee.recorder.audio.waveform.WaveformExtractor
                                .extractWaveform(it.absolutePath, 120)
                            // Serialize to JSON
                            waveform.joinToString(",")
                        } catch (e: Exception) {
                            android.util.Log.e("ThoughtListViewModel", "Failed to extract waveform", e)
                            null
                        }
                    }

                    val thought = Thought(
                        id = UUID.randomUUID().toString(),
                        audioPath = it.name,
                        createdAt = LocalDateTime.now(),
                        isTranscribed = false,
                        waveformData = waveformData
                    )
                    repository.insertThought(thought)

                    // Wait for data to propagate through Flow
                    delay(150)

                    // Auto-select the new thought (single selection) and scroll to it
                    _uiState.update { state ->
                        state.copy(
                            selectedThoughts = setOf(thought.id),
                            isMultiSelectMode = true,
                            scrollToThoughtId = thought.id,
                            isLoading = false
                        )
                    }
                }
            }.onFailure { exception ->
                _uiState.update {
                    it.copy(
                        error = "Failed to save recording: ${exception.message}",
                        isLoading = false
                    )
                }
            }
        }
    }

    fun playThought(thought: Thought) {
        viewModelScope.launch {
            val audioFile = repository.getAudioFile(thought.audioPath)
            if (!audioFile.exists()) {
                _uiState.update { it.copy(error = "Audio file not found") }
                return@launch
            }

            audioPlayer.play(thought.id, audioFile).onFailure { exception ->
                _uiState.update {
                    it.copy(error = "Playback failed: ${exception.message}")
                }
            }
        }
    }

    fun pausePlayback() {
        audioPlayer.pause()
    }

    fun resumePlayback() {
        audioPlayer.resume()
    }

    fun stopPlayback() {
        audioPlayer.stop()
    }

    fun toggleThoughtSelection(thoughtId: String) {
        _uiState.update { state ->
            val newSelection = if (thoughtId in state.selectedThoughts) {
                state.selectedThoughts - thoughtId
            } else {
                state.selectedThoughts + thoughtId
            }

            state.copy(
                selectedThoughts = newSelection,
                isMultiSelectMode = newSelection.isNotEmpty()
            )
        }
    }

    fun clearSelection() {
        _uiState.update {
            it.copy(
                selectedThoughts = emptySet(),
                isMultiSelectMode = false
            )
        }
    }

    fun deleteSelectedThoughts() {
        viewModelScope.launch {
            val selectedIds = _uiState.value.selectedThoughts
            if (selectedIds.isEmpty()) return@launch

            val allThoughts = _uiState.value.transcribedThoughts +
                    _uiState.value.originalThoughts +
                    _uiState.value.expiredAlarmThoughts

            val thoughtsToDelete = allThoughts.filter { it.id in selectedIds }

            repository.deleteThoughts(thoughtsToDelete)
            clearSelection()
        }
    }

    fun toggleColorFilter(color: ThoughtColor) {
        _uiState.update { state ->
            val newColors = if (color in state.selectedColors) {
                state.selectedColors - color
            } else {
                state.selectedColors + color
            }
            state.copy(selectedColors = newColors)
        }
        loadThoughts()
    }

    fun clearColorFilter() {
        _uiState.update { it.copy(selectedColors = emptyList()) }
        loadThoughts()
    }

    fun setColorFilter(colors: List<ThoughtColor?>) {
        _uiState.update { it.copy(selectedColors = colors) }
        loadThoughts()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearScrollRequest() {
        _uiState.update { it.copy(scrollToThoughtId = null) }
    }

    /**
     * 选择并滚动到指定的感言（用于通知点击）
     * 清除其他选择，只选择这一条感言
     */
    fun selectAndScrollToThought(thoughtId: String) {
        _uiState.update { state ->
            state.copy(
                selectedThoughts = setOf(thoughtId),
                isMultiSelectMode = true,
                scrollToThoughtId = thoughtId
            )
        }
    }

    // Phase 2: Speech-to-text functions
    fun convertSelectedThoughts() {
        viewModelScope.launch {
            val selectedIds = _uiState.value.selectedThoughts
            if (selectedIds.isEmpty()) return@launch

            val allThoughts = _uiState.value.transcribedThoughts +
                    _uiState.value.originalThoughts +
                    _uiState.value.expiredAlarmThoughts

            val thoughtsToConvert = allThoughts.filter { it.id in selectedIds && !it.isTranscribed }

            // Show loading state
            _uiState.update { it.copy(isLoading = true) }

            // Remember the first thought ID for auto-selection
            val firstThoughtId = thoughtsToConvert.firstOrNull()?.id

            thoughtsToConvert.forEach { thought ->
                try {
                    val convertedThought = speechToTextHelper.convertThought(thought)
                    repository.updateThought(convertedThought)
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(error = "转换失败: ${e.message}")
                    }
                }
            }

            _uiState.update { it.copy(isLoading = false) }

            // Wait for data to propagate through Flow
            delay(200)

            // Auto-select the first converted thought (single selection) and scroll to it
            if (firstThoughtId != null) {
                _uiState.update { state ->
                    state.copy(
                        selectedThoughts = setOf(firstThoughtId),
                        isMultiSelectMode = true,
                        scrollToThoughtId = firstThoughtId
                    )
                }
            } else {
                clearSelection()
            }
        }
    }

    fun editThought(thoughtId: String, newTitle: String, newContent: String) {
        viewModelScope.launch {
            val allThoughts = _uiState.value.transcribedThoughts +
                    _uiState.value.originalThoughts +
                    _uiState.value.expiredAlarmThoughts

            val thought = allThoughts.find { it.id == thoughtId } ?: return@launch

            val updatedThought = thought.copy(
                title = newTitle.trim(),
                content = newContent.trim(),
                isTranscribed = true,
                transcribedAt = thought.transcribedAt ?: java.time.LocalDateTime.now()
            )

            repository.updateThought(updatedThought)
        }
    }

    // Phase 3: Color marking
    fun setColorForSelectedThoughts(color: ThoughtColor?) {
        viewModelScope.launch {
            val selectedIds = _uiState.value.selectedThoughts
            if (selectedIds.isEmpty()) return@launch

            val allThoughts = _uiState.value.transcribedThoughts +
                    _uiState.value.originalThoughts +
                    _uiState.value.expiredAlarmThoughts

            val thoughtsToUpdate = allThoughts.filter { it.id in selectedIds }

            thoughtsToUpdate.forEach { thought ->
                val updatedThought = thought.copy(color = color)
                repository.updateThought(updatedThought)
            }

            clearSelection()
        }
    }

    // Phase 3: Alarm
    fun setAlarmForSelectedThoughts(alarmTime: LocalDateTime) {
        viewModelScope.launch {
            val selectedIds = _uiState.value.selectedThoughts
            if (selectedIds.isEmpty()) return@launch

            val allThoughts = _uiState.value.transcribedThoughts +
                    _uiState.value.originalThoughts +
                    _uiState.value.expiredAlarmThoughts

            val thoughtsToUpdate = allThoughts.filter { it.id in selectedIds }

            thoughtsToUpdate.forEach { thought ->
                val updatedThought = thought.copy(alarmTime = alarmTime)
                repository.updateThought(updatedThought)
            }

            clearSelection()
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioRecorder.release()
        audioPlayer.release()
        speechToTextHelper.release()
    }
}
