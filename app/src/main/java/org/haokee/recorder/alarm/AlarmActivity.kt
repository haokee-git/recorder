package org.haokee.recorder.alarm

import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.haokee.recorder.MainActivity
import org.haokee.recorder.data.local.ThoughtDatabase
import org.haokee.recorder.data.repository.ThoughtRepository
import org.haokee.recorder.ui.theme.RecorderTheme
import java.io.File
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info

class AlarmActivity : ComponentActivity() {
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var repository: ThoughtRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize repository
        val database = ThoughtDatabase.getDatabase(applicationContext)
        repository = ThoughtRepository(database.thoughtDao(), applicationContext)

        // Set window flags to show on lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        val thoughtId = intent.getStringExtra("thought_id") ?: ""
        val thoughtTitle = intent.getStringExtra("thought_title") ?: "感言提醒"
        val thoughtContent = intent.getStringExtra("thought_content") ?: ""

        setContent {
            RecorderTheme {
                AlarmScreen(
                    thoughtId = thoughtId,
                    thoughtTitle = thoughtTitle,
                    thoughtContent = thoughtContent,
                    onDismiss = { finishActivity() },
                    onViewDetail = { viewThoughtDetail(thoughtId) }
                )
            }
        }

        // Start playing audio
        startPlayingAudio(thoughtId)
    }

    @Composable
    private fun AlarmScreen(
        thoughtId: String,
        thoughtTitle: String,
        thoughtContent: String,
        onDismiss: () -> Unit,
        onViewDetail: () -> Unit
    ) {
        var isPlaying by remember { mutableStateOf(true) }
        var currentPosition by remember { mutableStateOf(0) }
        var duration by remember { mutableStateOf(0) }

        // Update playback progress
        LaunchedEffect(isPlaying) {
            while (isPlaying) {
                mediaPlayer?.let {
                    if (it.isPlaying) {
                        currentPosition = it.currentPosition
                        duration = it.duration
                    }
                }
                delay(100)
            }
        }

        // Disable back button
        BackHandler(enabled = true) {
            // Do nothing, force user to use buttons
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
        ) {
            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.9f)
                    .wrapContentHeight(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Alarm icon
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "闹钟提醒",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Title
                    Text(
                        text = thoughtTitle,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (thoughtContent.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))

                        // Content
                        Text(
                            text = thoughtContent,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Playback progress
                    if (duration > 0) {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            LinearProgressIndicator(
                                progress = { currentPosition.toFloat() / duration.toFloat() },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = formatTime(currentPosition),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatTime(duration),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Dismiss button
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "关闭",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("关闭")
                        }

                        // View detail button
                        Button(
                            onClick = onViewDetail,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "查看详情",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("查看详情")
                        }
                    }
                }
            }
        }
    }

    private fun formatTime(millis: Int): String {
        val seconds = millis / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return "%d:%02d".format(minutes, remainingSeconds)
    }

    private fun startPlayingAudio(thoughtId: String) {
        lifecycleScope.launch {
            try {
                val thought = repository.getThoughtById(thoughtId)
                if (thought != null) {
                    val audioFile = repository.getAudioFile(thought.audioPath)
                    if (audioFile.exists()) {
                        playAudio(audioFile)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun playAudio(audioFile: File) {
        try {
            stopAudio()

            mediaPlayer = MediaPlayer().apply {
                setDataSource(audioFile.absolutePath)
                prepare()
                setOnCompletionListener {
                    // Loop playback
                    start()
                }
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopAudio() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        mediaPlayer = null
    }

    private fun finishActivity() {
        stopAudio()
        finish()
    }

    private fun viewThoughtDetail(thoughtId: String) {
        stopAudio()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("thought_id", thoughtId)
        }
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAudio()
    }
}
