package org.haokee.recorder.ui.screen

import android.Manifest
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
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

    // Color filter dropdown state
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
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Toolbar
            val allThoughts = uiState.transcribedThoughts +
                    uiState.originalThoughts +
                    uiState.expiredAlarmThoughts
            val isAllTranscribed = uiState.selectedThoughts.isNotEmpty() &&
                    uiState.selectedThoughts.all { id ->
                        allThoughts.find { it.id == id }?.isTranscribed == true
                    }

            // Toolbar and Selection Info (merged in one Surface for consistent shadow)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 4.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    ThoughtToolbar(
                        hasSelection = uiState.selectedThoughts.isNotEmpty(),
                        isSingleSelection = uiState.selectedThoughts.size == 1,
                        isAllTranscribed = isAllTranscribed,
                        onBatchConvertClick = {
                            viewModel.convertSelectedThoughts()
                        },
                        onEditClick = {
                            // Get the single selected thought
                            val selectedId = uiState.selectedThoughts.firstOrNull()
                            if (selectedId != null) {
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
                    // Selection info bar (always visible)
                    SelectionInfoBar(
                        selectedCount = uiState.selectedThoughts.size,
                        onClearSelection = { viewModel.clearSelection() }
                    )
                }
            }

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
                onSelectAllInSection = { thoughts, selectAll ->
                    if (selectAll) {
                        thoughts.forEach { viewModel.toggleThoughtSelection(it.id) }
                    } else {
                        thoughts.forEach { if (it.id in uiState.selectedThoughts) viewModel.toggleThoughtSelection(it.id) }
                    }
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

        // Floating record button at bottom center
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp),
            contentAlignment = Alignment.Center
        ) {
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

    // Color filter dropdown (no dialog, just show/hide)
    if (showColorFilter) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    onClick = { showColorFilter = false },
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                )
        ) {
            ColorFilterDropdown(
                selectedColors = uiState.selectedColors,
                onColorToggle = { color ->
                    val currentColors = uiState.selectedColors.toMutableSet()
                    if (color in currentColors) {
                        currentColors.remove(color)
                    } else {
                        currentColors.add(color)
                    }
                    viewModel.setColorFilter(currentColors.toList())
                },
                onSelectAll = {
                    viewModel.setColorFilter(org.haokee.recorder.data.model.ThoughtColor.entries)
                },
                onClearAll = {
                    viewModel.setColorFilter(emptyList())
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 120.dp, end = 16.dp)
            )
        }
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

@Composable
private fun SelectionInfoBar(
    selectedCount: Int,
    onClearSelection: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 0.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "已选择 $selectedCount 条感言",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        TextButton(
            onClick = onClearSelection,
            enabled = selectedCount > 0,
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.primary,
                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            ),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("清除选中", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ColorFilterDropdown(
    selectedColors: List<org.haokee.recorder.data.model.ThoughtColor>,
    onColorToggle: (org.haokee.recorder.data.model.ThoughtColor) -> Unit,
    onSelectAll: () -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.width(220.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Color grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                org.haokee.recorder.data.model.ThoughtColor.entries.take(4).forEach { color ->
                    FilterColorCircle(
                        color = color,
                        isSelected = color in selectedColors,
                        onClick = { onColorToggle(color) }
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                org.haokee.recorder.data.model.ThoughtColor.entries.drop(4).forEach { color ->
                    FilterColorCircle(
                        color = color,
                        isSelected = color in selectedColors,
                        onClick = { onColorToggle(color) }
                    )
                }
            }

            // All/Clear buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(
                    onClick = onSelectAll,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("全选", style = MaterialTheme.typography.bodySmall)
                }
                TextButton(
                    onClick = onClearAll,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("清除", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun FilterColorCircle(
    color: org.haokee.recorder.data.model.ThoughtColor,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val cornerRadius by animateDpAsState(
        targetValue = if (isSelected) 8.dp else 28.dp,
        animationSpec = tween(durationMillis = 200),
        label = "cornerRadius"
    )

    val innerCornerRadius = (cornerRadius - 1.dp).coerceAtLeast(0.dp)

    val checkProgress by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "checkProgress"
    )

    val borderColor = remember(color) {
        androidx.compose.ui.graphics.Color(
            red = color.color.red * 0.7f,
            green = color.color.green * 0.7f,
            blue = color.color.blue * 0.7f
        )
    }

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(cornerRadius))
            .background(borderColor)
            .padding(1.dp)
            .clip(RoundedCornerShape(innerCornerRadius))
            .background(color.color)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(24.dp)) {
            if (checkProgress > 0f) {
                val checkPath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(size.width * 0.2f, size.height * 0.5f)
                    lineTo(size.width * 0.4f, size.height * 0.7f)
                    lineTo(size.width * 0.8f, size.height * 0.3f)
                }

                clipRect(
                    left = 0f,
                    top = 0f,
                    right = size.width * checkProgress,
                    bottom = size.height
                ) {
                    drawPath(
                        path = checkPath,
                        color = androidx.compose.ui.graphics.Color.White,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 4.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
        }
    }
}
