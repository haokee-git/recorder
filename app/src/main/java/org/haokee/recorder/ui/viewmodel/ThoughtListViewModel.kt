package org.haokee.recorder.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.haokee.recorder.audio.player.AudioPlayer
import org.haokee.recorder.audio.recorder.AudioRecorder
import org.haokee.recorder.data.model.Thought
import org.haokee.recorder.data.model.ThoughtColor
import org.haokee.recorder.data.repository.ThoughtRepository
import java.time.LocalDateTime
import java.util.UUID

data class ThoughtListUiState(
    val transcribedThoughts: List<Thought> = emptyList(),
    val originalThoughts: List<Thought> = emptyList(),
    val expiredAlarmThoughts: List<Thought> = emptyList(),
    val selectedThoughts: Set<String> = emptySet(),
    val isMultiSelectMode: Boolean = false,
    val selectedColors: List<ThoughtColor> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class ThoughtListViewModel(
    private val repository: ThoughtRepository,
    val audioRecorder: AudioRecorder,
    val audioPlayer: AudioPlayer
) : ViewModel() {

    private val _uiState = MutableStateFlow(ThoughtListUiState())
    val uiState: StateFlow<ThoughtListUiState> = _uiState.asStateFlow()

    init {
        loadThoughts()
        startPlaybackProgressUpdater()
    }

    private fun startPlaybackProgressUpdater() {
        viewModelScope.launch {
            audioPlayer.playbackState.collect { playbackState ->
                if (playbackState.isPlaying) {
                    // Update progress every 100ms while playing
                    while (playbackState.isPlaying) {
                        delay(100)
                        audioPlayer.updateProgress()
                    }
                }
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
        return thoughts.filter { it.color in selectedColors }
    }

    fun startRecording() {
        viewModelScope.launch {
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
            audioRecorder.stopRecording().onSuccess { file ->
                file?.let {
                    val thought = Thought(
                        id = UUID.randomUUID().toString(),
                        audioPath = it.name,
                        createdAt = LocalDateTime.now(),
                        isTranscribed = false
                    )
                    repository.insertThought(thought)
                }
            }.onFailure { exception ->
                _uiState.update {
                    it.copy(error = "Failed to save recording: ${exception.message}")
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

    fun setColorFilter(colors: List<ThoughtColor>) {
        _uiState.update { it.copy(selectedColors = colors) }
        loadThoughts()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
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

            thoughtsToConvert.forEach { thought ->
                val convertedThought = org.haokee.recorder.audio.recognizer.SpeechToTextHelper.convertThought(thought)
                repository.updateThought(convertedThought)
            }

            clearSelection()
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
    }
}
