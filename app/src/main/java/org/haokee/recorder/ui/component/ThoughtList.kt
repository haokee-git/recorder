package org.haokee.recorder.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.haokee.recorder.data.model.Thought

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ThoughtList(
    transcribedThoughts: List<Thought>,
    originalThoughts: List<Thought>,
    expiredAlarmThoughts: List<Thought>,
    selectedThoughts: Set<String>,
    currentPlayingThoughtId: String?,
    isPlaying: Boolean,
    playbackProgress: Float = 0f,
    isRecording: Boolean = false,
    scrollToThoughtId: String? = null,
    onThoughtClick: (Thought) -> Unit,
    onCheckboxClick: (Thought) -> Unit,
    onPlayClick: (Thought) -> Unit,
    onSelectAllInSection: (List<Thought>, Boolean) -> Unit = { _, _ -> },
    onScrollComplete: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Collapse states for each section
    var transcribedCollapsed by remember { mutableStateOf(false) }
    var originalCollapsed by remember { mutableStateOf(false) }
    var expiredCollapsed by remember { mutableStateOf(false) }

    // Handle auto-scroll to newly created/converted thought
    LaunchedEffect(scrollToThoughtId) {
        scrollToThoughtId?.let { targetId ->
            // Calculate the index position of the target thought
            var index = 0
            var found = false

            // Check in transcribed thoughts
            if (transcribedThoughts.isNotEmpty()) {
                index++ // Section header
                val transcribedIndex = transcribedThoughts.indexOfFirst { it.id == targetId }
                if (transcribedIndex >= 0) {
                    index += transcribedIndex
                    found = true
                } else {
                    index += transcribedThoughts.size
                }
            }

            // Check in original thoughts (only if not found yet)
            if (!found && originalThoughts.isNotEmpty()) {
                index++ // Section header
                val originalIndex = originalThoughts.indexOfFirst { it.id == targetId }
                if (originalIndex >= 0) {
                    index += originalIndex
                    found = true
                } else {
                    index += originalThoughts.size
                }
            }

            // Check in expired alarm thoughts (only if not found yet)
            if (!found && expiredAlarmThoughts.isNotEmpty()) {
                index++ // Section header
                val expiredIndex = expiredAlarmThoughts.indexOfFirst { it.id == targetId }
                if (expiredIndex >= 0) {
                    index += expiredIndex
                    found = true
                }
            }

            // Scroll to the found item with smart animation
            if (found) {
                val firstVisibleIndex = listState.firstVisibleItemIndex
                val distance = kotlin.math.abs(index - firstVisibleIndex)

                // Smart scroll strategy:
                // - If close (< 8 items): direct smooth animation
                // - If far (>= 8 items): jump near target, then animate
                if (distance < 8) {
                    // Close distance - smooth animation
                    listState.animateScrollToItem(index)
                } else {
                    // Far distance - jump close then animate for effect
                    val jumpTarget = maxOf(0, index - 3)
                    listState.scrollToItem(jumpTarget)
                    // Small delay for visual stability
                    kotlinx.coroutines.delay(50)
                    listState.animateScrollToItem(index)
                }
            }

            // Always clear the scroll request
            onScrollComplete()
        }
    }
    if (transcribedThoughts.isEmpty() && originalThoughts.isEmpty() && expiredAlarmThoughts.isEmpty()) {
        EmptyState(modifier = modifier)
    } else {
        LazyColumn(
            state = listState,
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Transcribed thoughts section
            if (transcribedThoughts.isNotEmpty()) {
                item(key = "transcribed_header") {
                    val isAllSelected = transcribedThoughts.all { it.id in selectedThoughts }
                    SectionHeader(
                        text = "已转换感言",
                        isCollapsed = transcribedCollapsed,
                        isAllSelected = isAllSelected,
                        onToggleCollapse = { transcribedCollapsed = !transcribedCollapsed },
                        onSelectAll = {
                            onSelectAllInSection(transcribedThoughts, !isAllSelected)
                        },
                        modifier = Modifier.animateItemPlacement(
                            animationSpec = tween(durationMillis = 200)
                        )
                    )
                }
                if (!transcribedCollapsed) {
                    items(
                        items = transcribedThoughts,
                        key = { it.id }
                    ) { thought ->
                        TranscribedThoughtItem(
                            thought = thought,
                            isSelected = thought.id in selectedThoughts,
                            isPlaying = thought.id == currentPlayingThoughtId && isPlaying,
                            playbackProgress = if (thought.id == currentPlayingThoughtId) playbackProgress else 0f,
                            isRecording = isRecording,
                            onClick = { onThoughtClick(thought) },
                            onCheckboxClick = { onCheckboxClick(thought) },
                            onPlayClick = { onPlayClick(thought) },
                            modifier = Modifier.animateItemPlacement(
                                animationSpec = tween(durationMillis = 200)
                            )
                        )
                    }
                }
            }

            // Original thoughts section
            if (originalThoughts.isNotEmpty()) {
                item(key = "original_header") {
                    val isAllSelected = originalThoughts.all { it.id in selectedThoughts }
                    SectionHeader(
                        text = "原始感言",
                        isCollapsed = originalCollapsed,
                        isAllSelected = isAllSelected,
                        onToggleCollapse = { originalCollapsed = !originalCollapsed },
                        onSelectAll = {
                            onSelectAllInSection(originalThoughts, !isAllSelected)
                        },
                        modifier = Modifier.animateItemPlacement(
                            animationSpec = tween(durationMillis = 200)
                        )
                    )
                }
                if (!originalCollapsed) {
                    items(
                        items = originalThoughts,
                        key = { it.id }
                    ) { thought ->
                        OriginalThoughtItem(
                            thought = thought,
                            isSelected = thought.id in selectedThoughts,
                            isPlaying = thought.id == currentPlayingThoughtId && isPlaying,
                            playbackProgress = if (thought.id == currentPlayingThoughtId) playbackProgress else 0f,
                            isRecording = isRecording,
                            onClick = { onThoughtClick(thought) },
                            onCheckboxClick = { onCheckboxClick(thought) },
                            onPlayClick = { onPlayClick(thought) },
                            modifier = Modifier.animateItemPlacement(
                                animationSpec = tween(durationMillis = 200)
                            )
                        )
                    }
                }
            }

            // Expired alarm thoughts section
            if (expiredAlarmThoughts.isNotEmpty()) {
                item(key = "expired_header") {
                    val isAllSelected = expiredAlarmThoughts.all { it.id in selectedThoughts }
                    SectionHeader(
                        text = "闹钟已过的感言",
                        isCollapsed = expiredCollapsed,
                        isAllSelected = isAllSelected,
                        onToggleCollapse = {
                            expiredCollapsed = !expiredCollapsed
                        },
                        onSelectAll = {
                            onSelectAllInSection(expiredAlarmThoughts, !isAllSelected)
                        },
                        modifier = Modifier.animateItemPlacement(
                            animationSpec = tween(durationMillis = 200)
                        )
                    )
                }
                if (!expiredCollapsed) {
                    items(
                        items = expiredAlarmThoughts,
                        key = { it.id }
                    ) { thought ->
                        ExpiredThoughtItem(
                            thought = thought,
                            isSelected = thought.id in selectedThoughts,
                            isPlaying = thought.id == currentPlayingThoughtId && isPlaying,
                            playbackProgress = if (thought.id == currentPlayingThoughtId) playbackProgress else 0f,
                            isRecording = isRecording,
                            onClick = { onThoughtClick(thought) },
                            onCheckboxClick = { onCheckboxClick(thought) },
                            onPlayClick = { onPlayClick(thought) },
                            modifier = Modifier.animateItemPlacement(
                                animationSpec = tween(durationMillis = 200)
                            )
                        )
                    }
                }
            }

            // Spacer for record button
            item {
                Spacer(modifier = Modifier.height(96.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(
    text: String,
    isCollapsed: Boolean,
    isAllSelected: Boolean,
    onToggleCollapse: () -> Unit,
    onSelectAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable(onClick = onToggleCollapse)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.clickable(onClick = onSelectAll)
            ) {
                SectionCheckbox(isSelected = isAllSelected)
                Text(
                    "全选",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(
                onClick = onToggleCollapse,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (isCollapsed) Icons.Default.KeyboardArrowRight else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isCollapsed) "展开" else "折叠",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun SectionCheckbox(
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    // Animate corner radius: circle (10.dp) to rounded rect (5.dp)
    val cornerRadius by animateDpAsState(
        targetValue = if (isSelected) 5.dp else 10.dp,
        animationSpec = tween(durationMillis = 200),
        label = "cornerRadius"
    )

    // Save primary color for use in Canvas and border
    val primaryColor = MaterialTheme.colorScheme.primary

    // Border width is always 1.dp
    val borderWidth = 1.dp

    // Border color changes based on selection
    val borderColor = if (isSelected) primaryColor else MaterialTheme.colorScheme.outline

    // Animate check progress (0 to 1)
    val checkProgress by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "checkProgress"
    )

    Box(
        modifier = modifier
            .size(20.dp)
            .border(
                width = borderWidth,
                color = borderColor,
                shape = RoundedCornerShape(cornerRadius)
            )
            .clip(RoundedCornerShape(cornerRadius))
            .background(if (isSelected) primaryColor else Color.White),
        contentAlignment = Alignment.Center
    ) {
        // Animated check mark from left to right
        Canvas(modifier = Modifier.size(13.dp)) {
            if (checkProgress > 0f) {
                val checkPath = Path().apply {
                    // Check mark path
                    moveTo(size.width * 0.2f, size.height * 0.5f)
                    lineTo(size.width * 0.4f, size.height * 0.7f)
                    lineTo(size.width * 0.8f, size.height * 0.3f)
                }

                // Clip the path based on progress
                clipRect(
                    left = 0f,
                    top = 0f,
                    right = size.width * checkProgress,
                    bottom = size.height
                ) {
                    drawPath(
                        path = checkPath,
                        color = Color.White,
                        style = Stroke(
                            width = 2.dp.toPx(),
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
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "还没有感言",
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "点击右下角按钮开始录音",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
