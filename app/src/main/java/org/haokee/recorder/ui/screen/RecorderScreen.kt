package org.haokee.recorder.ui.screen

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import org.haokee.recorder.ui.component.*
import org.haokee.recorder.ui.viewmodel.ThoughtListViewModel

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RecorderScreen(
    viewModel: ThoughtListViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val recordingState by viewModel.audioRecorder.recordingState.collectAsState()
    val playbackState by viewModel.audioPlayer.playbackState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Edit dialog state
    var editingThought by remember { mutableStateOf<org.haokee.recorder.data.model.Thought?>(null) }

    // Color picker dialog state
    var showColorPicker by remember { mutableStateOf(false) }

    // Color filter dialog state
    var showColorFilter by remember { mutableStateOf(false) }

    // Alarm time picker dialog state
    var showAlarmPicker by remember { mutableStateOf(false) }

    // Permission handling
    val recordAudioPermissionState = rememberPermissionState(
        permission = Manifest.permission.RECORD_AUDIO
    )

    // Notification permission for Android 13+
    val notificationPermissionState = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(
            permission = Manifest.permission.POST_NOTIFICATIONS
        )
    } else {
        null
    }

    // Request notification permission on first composition if needed
    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionState?.let {
                if (!it.status.isGranted && !it.status.shouldShowRationale) {
                    it.launchPermissionRequest()
                }
            }
        }
    }

    // Show error messages
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            RecorderTopBar(
                onChatClick = {
                    // TODO: Phase 3 - Open chat dialog
                },
                onSettingsClick = {
                    // TODO: Phase 4 - Open settings screen
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            RecordButton(
                isRecording = recordingState.isRecording,
                onClick = {
                    if (recordingState.isRecording) {
                        viewModel.stopRecording()
                    } else {
                        if (recordAudioPermissionState.status.isGranted) {
                            viewModel.startRecording()
                        } else {
                            recordAudioPermissionState.launchPermissionRequest()
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Toolbar
            ThoughtToolbar(
                hasSelection = uiState.selectedThoughts.isNotEmpty(),
                isSingleSelection = uiState.selectedThoughts.size == 1,
                onBatchConvertClick = {
                    viewModel.convertSelectedThoughts()
                },
                onEditClick = {
                    // Get the single selected thought
                    val selectedId = uiState.selectedThoughts.firstOrNull()
                    if (selectedId != null) {
                        val allThoughts = uiState.transcribedThoughts +
                                uiState.originalThoughts +
                                uiState.expiredAlarmThoughts
                        editingThought = allThoughts.find { it.id == selectedId }
                    }
                },
                onSetAlarmClick = {
                    showAlarmPicker = true
                },
                onSetColorClick = {
                    showColorPicker = true
                },
                onDeleteClick = {
                    viewModel.deleteSelectedThoughts()
                },
                onFilterClick = {
                    showColorFilter = true
                }
            )

            // Thought list
            ThoughtList(
                transcribedThoughts = uiState.transcribedThoughts,
                originalThoughts = uiState.originalThoughts,
                expiredAlarmThoughts = uiState.expiredAlarmThoughts,
                selectedThoughts = uiState.selectedThoughts,
                currentPlayingThoughtId = playbackState.currentThoughtId,
                isPlaying = playbackState.isPlaying,
                playbackProgress = if (playbackState.duration > 0) {
                    playbackState.currentPosition.toFloat() / playbackState.duration.toFloat()
                } else 0f,
                isRecording = recordingState.isRecording,
                scrollToThoughtId = uiState.scrollToThoughtId,
                onThoughtClick = { thought ->
                    // Click on card - play audio
                    if (playbackState.currentThoughtId == thought.id && playbackState.isPlaying) {
                        viewModel.pausePlayback()
                    } else if (playbackState.currentThoughtId == thought.id && !playbackState.isPlaying) {
                        viewModel.resumePlayback()
                    } else {
                        viewModel.playThought(thought)
                    }
                },
                onCheckboxClick = { thought ->
                    // Click on checkbox - toggle selection
                    viewModel.toggleThoughtSelection(thought.id)
                },
                onPlayClick = { thought ->
                    if (playbackState.currentThoughtId == thought.id && playbackState.isPlaying) {
                        viewModel.pausePlayback()
                    } else if (playbackState.currentThoughtId == thought.id && !playbackState.isPlaying) {
                        viewModel.resumePlayback()
                    } else {
                        viewModel.playThought(thought)
                    }
                },
                onScrollComplete = {
                    viewModel.clearScrollRequest()
                },
                modifier = Modifier.weight(1f)
            )
        }
    }

    // Edit thought dialog
    editingThought?.let { thought ->
        EditThoughtDialog(
            thought = thought,
            onDismiss = {
                editingThought = null
            },
            onSave = { title, content ->
                viewModel.editThought(thought.id, title, content)
                viewModel.clearSelection()
            }
        )
    }

    // Color picker dialog
    if (showColorPicker) {
        // Get current color from first selected thought
        val currentColor = remember {
            val selectedId = uiState.selectedThoughts.firstOrNull()
            if (selectedId != null) {
                val allThoughts = uiState.transcribedThoughts +
                        uiState.originalThoughts +
                        uiState.expiredAlarmThoughts
                allThoughts.find { it.id == selectedId }?.color
            } else null
        }

        ColorPickerDialog(
            currentColor = currentColor,
            onDismiss = {
                showColorPicker = false
            },
            onColorSelected = { color ->
                viewModel.setColorForSelectedThoughts(color)
            }
        )
    }

    // Color filter dialog
    if (showColorFilter) {
        ColorFilterDialog(
            selectedColors = uiState.selectedColors,
            onDismiss = {
                showColorFilter = false
            },
            onColorsSelected = { colors ->
                viewModel.setColorFilter(colors)
            }
        )
    }

    // Alarm time picker dialog
    if (showAlarmPicker) {
        WheelTimePickerDialog(
            onDismiss = {
                showAlarmPicker = false
            },
            onTimeSelected = { alarmTime ->
                viewModel.setAlarmForSelectedThoughts(alarmTime)

                // Schedule alarms for selected thoughts
                val selectedIds = uiState.selectedThoughts
                val allThoughts = uiState.transcribedThoughts +
                        uiState.originalThoughts +
                        uiState.expiredAlarmThoughts

                selectedIds.forEach { id ->
                    val thought = allThoughts.find { it.id == id }
                    thought?.let {
                        val title = if (it.isTranscribed) it.title ?: "感言提醒" else "感言提醒"
                        org.haokee.recorder.alarm.AlarmHelper.scheduleAlarm(
                            context,
                            it.id,
                            title,
                            alarmTime
                        )
                    }
                }
            }
        )
    }

    // Permission rationale dialog for recording
    if (!recordAudioPermissionState.status.isGranted && recordAudioPermissionState.status.shouldShowRationale) {
        AlertDialog(
            onDismissRequest = { /* Do nothing */ },
            title = { Text("需要录音权限") },
            text = { Text("此应用需要录音权限来录制您的感言。") },
            confirmButton = {
                TextButton(onClick = { recordAudioPermissionState.launchPermissionRequest() }) {
                    Text("授予权限")
                }
            },
            dismissButton = {
                TextButton(onClick = { /* Do nothing */ }) {
                    Text("取消")
                }
            }
        )
    }

    // Permission rationale dialog for notifications (Android 13+)
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        notificationPermissionState?.let { permissionState ->
            if (!permissionState.status.isGranted && permissionState.status.shouldShowRationale) {
                AlertDialog(
                    onDismissRequest = { /* Do nothing */ },
                    title = { Text("需要通知权限") },
                    text = { Text("此应用需要通知权限来在闹钟时间到达时提醒您。") },
                    confirmButton = {
                        TextButton(onClick = { permissionState.launchPermissionRequest() }) {
                            Text("授予权限")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { /* Do nothing */ }) {
                            Text("取消")
                        }
                    }
                )
            }
        }
    }

    // Clear selection on back press
    BackHandler(enabled = uiState.isMultiSelectMode) {
        viewModel.clearSelection()
    }
}

@Composable
private fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
    androidx.activity.compose.BackHandler(enabled = enabled, onBack = onBack)
}
