package org.haokee.recorder.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.haokee.recorder.audio.player.AudioPlayer
import org.haokee.recorder.audio.recorder.AudioRecorder
import org.haokee.recorder.data.repository.SettingsRepository
import org.haokee.recorder.data.repository.ThoughtRepository

class ThoughtViewModelFactory(
    private val context: Context,
    private val repository: ThoughtRepository,
    private val settingsRepository: SettingsRepository,
    private val audioRecorder: AudioRecorder,
    private val audioPlayer: AudioPlayer
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ThoughtListViewModel::class.java)) {
            return ThoughtListViewModel(context, repository, settingsRepository, audioRecorder, audioPlayer) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
