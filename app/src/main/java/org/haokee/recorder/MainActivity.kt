package org.haokee.recorder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import org.haokee.recorder.audio.player.AudioPlayer
import org.haokee.recorder.audio.recorder.AudioRecorder
import org.haokee.recorder.data.local.ThoughtDatabase
import org.haokee.recorder.data.repository.ThoughtRepository
import org.haokee.recorder.ui.screen.RecorderScreen
import org.haokee.recorder.ui.theme.RecorderTheme
import org.haokee.recorder.ui.viewmodel.ThoughtListViewModel
import org.haokee.recorder.ui.viewmodel.ThoughtViewModelFactory

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: ThoughtListViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize dependencies
        val database = ThoughtDatabase.getDatabase(applicationContext)
        val repository = ThoughtRepository(database.thoughtDao(), applicationContext)
        val audioRecorder = AudioRecorder()
        val audioPlayer = AudioPlayer()

        // Create ViewModel
        val factory = ThoughtViewModelFactory(repository, audioRecorder, audioPlayer)
        viewModel = ViewModelProvider(this, factory)[ThoughtListViewModel::class.java]

        setContent {
            RecorderTheme {
                RecorderScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.audioRecorder.release()
        viewModel.audioPlayer.release()
    }
}