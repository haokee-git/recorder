package org.haokee.recorder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import org.haokee.recorder.audio.player.AudioPlayer
import org.haokee.recorder.audio.recorder.AudioRecorder
import org.haokee.recorder.data.local.ThoughtDatabase
import org.haokee.recorder.data.repository.ChatRepository
import org.haokee.recorder.data.repository.SettingsRepository
import org.haokee.recorder.data.repository.ThoughtRepository
import org.haokee.recorder.ui.screen.RecorderScreen
import org.haokee.recorder.ui.screen.SettingsScreen
import org.haokee.recorder.ui.theme.RecorderTheme
import org.haokee.recorder.ui.viewmodel.ChatViewModel
import org.haokee.recorder.ui.viewmodel.SettingsViewModel
import org.haokee.recorder.ui.viewmodel.ThoughtListViewModel
import org.haokee.recorder.ui.viewmodel.ThoughtViewModelFactory

class MainActivity : ComponentActivity() {
    private lateinit var thoughtViewModel: ThoughtListViewModel
    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var chatViewModel: ChatViewModel
    private var currentScreen by mutableStateOf(Screen.RECORDER)

    enum class Screen {
        RECORDER,
        SETTINGS
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        android.util.Log.d("MainActivity", "onCreate started")

        try {
            // Initialize dependencies
            android.util.Log.d("MainActivity", "Initializing database...")
            val database = ThoughtDatabase.getDatabase(applicationContext)
            val thoughtRepository = ThoughtRepository(database.thoughtDao(), applicationContext)
            val settingsRepository = SettingsRepository(applicationContext)
            val audioRecorder = AudioRecorder()
            val audioPlayer = AudioPlayer()

            // Create ViewModels
            android.util.Log.d("MainActivity", "Creating ViewModels...")
            val thoughtFactory = ThoughtViewModelFactory(applicationContext, thoughtRepository, settingsRepository, audioRecorder, audioPlayer)
            thoughtViewModel = ViewModelProvider(this, thoughtFactory)[ThoughtListViewModel::class.java]

            settingsViewModel = SettingsViewModel(settingsRepository, thoughtRepository)
            val chatRepository = ChatRepository(applicationContext)
            chatViewModel = ChatViewModel(settingsRepository, thoughtRepository, chatRepository)

            android.util.Log.d("MainActivity", "ViewModels created successfully")

            // Handle notification click (from alarm)
            handleNotificationIntent(intent)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error in onCreate", e)
            throw e
        }

        setContent {
            RecorderTheme(darkTheme = settingsViewModel.uiState.value.isDarkTheme) {
                when (currentScreen) {
                    Screen.RECORDER -> RecorderScreen(
                        viewModel = thoughtViewModel,
                        chatViewModel = chatViewModel,
                        onSettingsClick = { currentScreen = Screen.SETTINGS },
                        modifier = Modifier.fillMaxSize()
                    )
                    Screen.SETTINGS -> SettingsScreen(
                        viewModel = settingsViewModel,
                        onNavigateBack = { currentScreen = Screen.RECORDER },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: android.content.Intent?) {
        intent?.getStringExtra("thought_id")?.let { thoughtId ->
            thoughtViewModel.selectAndScrollToThought(thoughtId)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        thoughtViewModel.audioRecorder.release()
        thoughtViewModel.audioPlayer.release()
    }
}