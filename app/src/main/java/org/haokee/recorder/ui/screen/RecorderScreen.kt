package org.haokee.recorder.ui.screen

import android.Manifest
import android.view.HapticFeedbackConstants
import kotlinx.coroutines.launch
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import org.haokee.recorder.ui.component.*
import org.haokee.recorder.ui.viewmodel.ChatViewModel
import org.haokee.recorder.ui.viewmodel.ThoughtListViewModel

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RecorderScreen(
    viewModel: ThoughtListViewModel,
    chatViewModel: ChatViewModel,
    onSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    val recordingState by viewModel.audioRecorder.recordingState.collectAsState()
    val playbackState by viewModel.audioPlayer.playbackState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // Edit dialog state
    var editingThought by remember { mutableStateOf<org.haokee.recorder.data.model.Thought?>(null) }

    // Color picker dialog state
    var showColorPicker by remember { mutableStateOf(false) }

    // Color filter dropdown state
    var showColorFilter by remember { mutableStateOf(false) }

    // Alarm time picker dialog state
    var showAlarmPicker by remember { mutableStateOf(false) }
    var pendingAlarmTime by remember { mutableStateOf<java.time.LocalDateTime?>(null) }

    // Delete pending state
    var isDeletePending by remember { mutableStateOf(false) }

    // Reset delete pending state when selection changes
    LaunchedEffect(uiState.selectedThoughts) {
        isDeletePending = false
    }

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

    // 监听应用恢复（从设置页面返回），检查权限并设置待处理的闹钟
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, coroutineScope) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                // 应用恢复，检查是否有待设置的闹钟
                pendingAlarmTime?.let { alarmTime ->
                    if (org.haokee.recorder.alarm.AlarmHelper.hasAlarmPermission(context)) {
                        // 有权限了，设置闹钟
                        viewModel.setAlarmForSelectedThoughts(alarmTime)

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

                        // 清除待处理的闹钟时间
                        pendingAlarmTime = null

                        // 显示成功提示
                        kotlinx.coroutines.MainScope().launch {
                            snackbarHostState.showSnackbar("闹钟设置成功")
                        }
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ChatDrawer(
                viewModel = chatViewModel,
                onClose = {
                    coroutineScope.launch { drawerState.close() }
                }
            )
        },
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    RecorderTopBar(
                        onChatClick = {
                            coroutineScope.launch {
                                drawerState.open()
                            }
                        },
                        onSettingsClick = onSettingsClick
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
                        isDeletePending = isDeletePending,
                        onBatchConvertClick = {
                            isDeletePending = false
                            viewModel.stopPlayback()
                            viewModel.convertSelectedThoughts()
                        },
                        onEditClick = {
                            isDeletePending = false
                            viewModel.stopPlayback()
                            // Get the single selected thought
                            val selectedId = uiState.selectedThoughts.firstOrNull()
                            if (selectedId != null) {
                                editingThought = allThoughts.find { it.id == selectedId }
                            }
                        },
                        onSetAlarmClick = {
                            isDeletePending = false
                            showAlarmPicker = true
                        },
                        onSetColorClick = {
                            isDeletePending = false
                            viewModel.stopPlayback()
                            showColorPicker = true
                        },
                        onDeleteClick = {
                            if (isDeletePending) {
                                // 第二次点击：真正删除
                                viewModel.stopPlayback()
                                viewModel.deleteSelectedThoughts()
                                isDeletePending = false
                            } else {
                                // 第一次点击：进入待确认状态
                                isDeletePending = true
                            }
                        },
                        onFilterClick = {
                            isDeletePending = false
                            showColorFilter = true
                        }
                    )
                    // Selection info bar (always visible)
                    SelectionInfoBar(
                        selectedCount = uiState.selectedThoughts.size,
                        onClearSelection = { viewModel.clearSelection() }
                    )
                    // Spacer for visual breathing room
                    Spacer(modifier = Modifier.height(8.dp))
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
                    // Click on card - do nothing (only play button triggers playback)
                },
                onCheckboxClick = { thought ->
                    // Click on checkbox - toggle selection
                    viewModel.toggleThoughtSelection(thought.id)
                },
                onSelectAllInSection = { thoughts, selectAll ->
                    if (selectAll) {
                        // 全选：只选择未选中的项
                        thoughts.forEach {
                            if (it.id !in uiState.selectedThoughts) {
                                viewModel.toggleThoughtSelection(it.id)
                            }
                        }
                    } else {
                        // 取消全选：取消该区域的所有选中项
                        thoughts.forEach {
                            if (it.id in uiState.selectedThoughts) {
                                viewModel.toggleThoughtSelection(it.id)
                            }
                        }
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

        // Dismiss background when filter is shown (behind the dropdown)
        if (showColorFilter) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        onClick = { showColorFilter = false },
                        indication = null,
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    )
            )
        }

        // Color filter dropdown (on top of everything)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .align(Alignment.TopEnd)
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = showColorFilter,
                enter = expandVertically(
                    animationSpec = tween(durationMillis = 200),
                    expandFrom = Alignment.Top
                ),
                exit = shrinkVertically(
                    animationSpec = tween(durationMillis = 200),
                    shrinkTowards = Alignment.Top
                ),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 88.dp, end = 16.dp)
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
                    onClearAll = {
                        viewModel.setColorFilter(emptyList())
                    }
                )
            }
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

        // Truly floating progress bar overlay (outside Scaffold, doesn't affect layout)
        if (uiState.isLoading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
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

    // Alarm time picker dialog
    if (showAlarmPicker) {
        // Collect existing alarm times from non-selected thoughts
        val existingAlarmTimes = remember(uiState.transcribedThoughts, uiState.originalThoughts, uiState.expiredAlarmThoughts, uiState.selectedThoughts) {
            val allThoughts = uiState.transcribedThoughts + uiState.originalThoughts + uiState.expiredAlarmThoughts
            allThoughts
                .filter { it.id !in uiState.selectedThoughts && it.alarmTime != null }
                .mapNotNull { it.alarmTime }
        }

        WheelTimePickerDialog(
            onDismiss = {
                showAlarmPicker = false
            },
            existingAlarmTimes = existingAlarmTimes,
            onTimeSelected = { alarmTime ->
                // 先检查精确闹钟权限
                if (!org.haokee.recorder.alarm.AlarmHelper.hasAlarmPermission(context)) {
                    // 没有权限，保存待设置的闹钟时间，然后跳转到设置页面
                    pendingAlarmTime = alarmTime
                    android.widget.Toast.makeText(
                        context,
                        "需要授予精确闹钟权限，即将跳转到设置页面",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                    org.haokee.recorder.alarm.AlarmHelper.requestAlarmPermission(context)
                } else {
                    // 有权限，直接设置闹钟
                    viewModel.setAlarmForSelectedThoughts(alarmTime)

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
                TextButton(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        recordAudioPermissionState.launchPermissionRequest()
                    }
                ) {
                    Text("授予权限")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        /* Do nothing */
                    }
                ) {
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
                        TextButton(
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                permissionState.launchPermissionRequest()
                            }
                        ) {
                            Text("授予权限")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                /* Do nothing */
                            }
                        ) {
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

    // Handle back gesture when chat drawer is open (higher priority than selection clearing)
    BackHandler(enabled = drawerState.isOpen) {
        coroutineScope.launch { drawerState.close() }
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
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            modifier = Modifier.height(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("取消选中", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ColorFilterDropdown(
    selectedColors: List<org.haokee.recorder.data.model.ThoughtColor?>,
    onColorToggle: (org.haokee.recorder.data.model.ThoughtColor?) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(200.dp)
            .wrapContentHeight(),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Color grid - Row 1
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
            // Color grid - Row 2
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
            // Row 3: No Color + Clear button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NoColorFilterCircle(
                    isSelected = null in selectedColors,
                    onClick = { onColorToggle(null) }
                )
                TextButton(
                    onClick = onClearAll,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
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
    val view = LocalView.current
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
            .clickable {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onClick()
            },
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

@Composable
private fun NoColorFilterCircle(
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val view = LocalView.current
    val cornerRadius by animateDpAsState(
        targetValue = if (isSelected) 8.dp else 28.dp,
        animationSpec = tween(durationMillis = 200),
        label = "cornerRadius"
    )

    val innerCornerRadius = (cornerRadius - 1.dp).coerceAtLeast(0.dp)

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(cornerRadius))
            .background(MaterialTheme.colorScheme.error)
            .padding(1.dp)
            .clip(RoundedCornerShape(innerCornerRadius))
            .background(MaterialTheme.colorScheme.surface)
            .clickable {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        // Draw diagonal slash that extends from corner to corner
        val errorColor = MaterialTheme.colorScheme.error
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawLine(
                color = errorColor,
                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                end = androidx.compose.ui.geometry.Offset(size.width, size.height),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Square
            )
        }
    }
}
