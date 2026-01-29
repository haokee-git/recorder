package org.haokee.recorder.ui.component

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.YearMonth
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WheelTimePickerDialog(
    onDismiss: () -> Unit,
    onTimeSelected: (LocalDateTime) -> Unit
) {
    val currentTime = LocalDateTime.now()
    var selectedYear by remember { mutableIntStateOf(currentTime.year) }
    var selectedMonth by remember { mutableIntStateOf(currentTime.monthValue) }
    var selectedDay by remember { mutableIntStateOf(currentTime.dayOfMonth) }
    var selectedHour by remember { mutableIntStateOf(currentTime.hour) }
    var selectedMinute by remember { mutableIntStateOf(currentTime.minute) }

    // Calculate days in month (considering leap year)
    val daysInMonth = remember(selectedYear, selectedMonth) {
        YearMonth.of(selectedYear, selectedMonth).lengthOfMonth()
    }

    // Adjust day immediately to ensure it's always valid
    val effectiveDay = selectedDay.coerceIn(1, daysInMonth)

    // Update selectedDay state when adjusted
    LaunchedEffect(effectiveDay) {
        if (selectedDay != effectiveDay) {
            selectedDay = effectiveDay
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "设置提醒时间",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                // Date picker: Year / Month / Day
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = String.format("%04d/%02d/%02d", selectedYear, selectedMonth, effectiveDay),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Year picker
                        DrumRollPicker(
                            items = (1900..2100).toList(),
                            selectedItem = selectedYear,
                            onItemSelected = { selectedYear = it },
                            modifier = Modifier.weight(1f)
                        )

                        // Month picker
                        DrumRollPicker(
                            items = (1..12).toList(),
                            selectedItem = selectedMonth,
                            onItemSelected = { selectedMonth = it },
                            modifier = Modifier.weight(1f),
                            cyclic = true
                        )

                        // Day picker
                        DrumRollPicker(
                            items = (1..daysInMonth).toList(),
                            selectedItem = effectiveDay, // Use adjusted day
                            onItemSelected = { selectedDay = it },
                            modifier = Modifier.weight(1f),
                            cyclic = true,
                            key = "$selectedYear-$selectedMonth-$daysInMonth" // Force recreation when days change
                        )
                    }
                }

                Divider()

                // Time picker: Hour : Minute
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = String.format("%02d:%02d", selectedHour, selectedMinute),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Hour picker
                        DrumRollPicker(
                            items = (0..23).toList(),
                            selectedItem = selectedHour,
                            onItemSelected = { selectedHour = it },
                            modifier = Modifier.weight(1f),
                            cyclic = true
                        )

                        // Minute picker
                        DrumRollPicker(
                            items = (0..59).toList(),
                            selectedItem = selectedMinute,
                            onItemSelected = { selectedMinute = it },
                            modifier = Modifier.weight(1f),
                            cyclic = true
                        )
                    }
                }

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledTonalButton(
                        onClick = {
                            val alarmTime = LocalDateTime.of(
                                selectedYear,
                                selectedMonth,
                                selectedDay,
                                selectedHour,
                                selectedMinute
                            )
                            onTimeSelected(alarmTime)
                            onDismiss()
                        }
                    ) {
                        Text("确定")
                    }
                }
            }
        }
    }
}

@Composable
fun DrumRollPicker(
    items: List<Int>,
    selectedItem: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    cyclic: Boolean = false,
    key: String? = null
) {
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val visibleItemsCount = 5
    val itemHeightDp = 40.dp
    val itemHeightPx = with(density) { itemHeightDp.toPx() }

    // Find initial index
    val initialIndex = items.indexOf(selectedItem).coerceAtLeast(0)

    // For cyclic scrolling, repeat items to create infinite scroll
    val displayItems = remember(items, cyclic, key) {
        if (cyclic) {
            items + items + items
        } else {
            items
        }
    }

    // Calculate initial scroll position
    val initialScrollOffset = remember(initialIndex, cyclic, key, items.size) {
        if (cyclic) {
            (initialIndex + items.size) * itemHeightPx
        } else {
            initialIndex * itemHeightPx
        }
    }

    // Scroll offset (in pixels)
    val scrollOffset = remember(key) { Animatable(initialScrollOffset) }
    var isDragging by remember { mutableStateOf(false) }
    var isFling by remember { mutableStateOf(false) }
    var lastNotifiedIndex by remember { mutableIntStateOf(-1) }

    // Calculate current center index based on scroll offset
    val currentCenterIndex by remember {
        derivedStateOf {
            val rawIndex = (scrollOffset.value / itemHeightPx).roundToInt()
            if (cyclic) {
                val moduloIndex = rawIndex % items.size
                // Handle negative modulo
                if (moduloIndex < 0) moduloIndex + items.size else moduloIndex
            } else {
                rawIndex.coerceIn(0, items.lastIndex)
            }
        }
    }

    // Reset scroll position when key changes (e.g., month changes affecting days)
    LaunchedEffect(key, initialScrollOffset) {
        if (!isDragging && !isFling) {
            scrollOffset.snapTo(initialScrollOffset)
            lastNotifiedIndex = -1 // Force update notification
        }
    }

    // Update selected item in real-time (using snapshotFlow to ensure updates during animation)
    LaunchedEffect(Unit) {
        snapshotFlow { scrollOffset.value }
            .collect {
                val rawIndex = (it / itemHeightPx).roundToInt()
                val index = if (cyclic) {
                    val moduloIndex = rawIndex % items.size
                    if (moduloIndex < 0) moduloIndex + items.size else moduloIndex
                } else {
                    rawIndex.coerceIn(0, items.lastIndex)
                }

                if (index in items.indices && index != lastNotifiedIndex) {
                    onItemSelected(items[index])
                    lastNotifiedIndex = index
                }
            }
    }

    // Snap animation when user releases (and not flinging)
    LaunchedEffect(isDragging, isFling) {
        if (!isDragging && !isFling) {
            // Calculate target snap position
            val targetIndex = (scrollOffset.value / itemHeightPx).roundToInt()
            val targetOffset = targetIndex * itemHeightPx

            // Animate to snap position
            scrollOffset.animateTo(
                targetValue = targetOffset,
                animationSpec = tween(durationMillis = 200)
            )

            // Trigger haptic feedback
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)

            // For cyclic scroll, reset to middle repetition when needed
            if (cyclic) {
                val finalIndex = (scrollOffset.value / itemHeightPx).roundToInt()
                if (finalIndex < items.size / 2) {
                    // Jumped too far up, reset to middle
                    scrollOffset.snapTo(scrollOffset.value + items.size * itemHeightPx)
                } else if (finalIndex >= items.size * 2 + items.size / 2) {
                    // Jumped too far down, reset to middle
                    scrollOffset.snapTo(scrollOffset.value - items.size * itemHeightPx)
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(100.dp)
            .pointerInput(key) {
                val velocityTracker = VelocityTracker()

                awaitEachGesture {
                    val down = awaitFirstDown()
                    isDragging = true
                    isFling = false
                    velocityTracker.resetTracking()

                    drag(down.id) { change ->
                        velocityTracker.addPosition(
                            change.uptimeMillis,
                            change.position
                        )

                        val dragAmount = change.positionChange().y
                        val newOffset = scrollOffset.value - dragAmount

                        // For non-cyclic, clamp the offset
                        val clampedOffset = if (cyclic) {
                            newOffset
                        } else {
                            newOffset.coerceIn(0f, (items.size - 1) * itemHeightPx)
                        }

                        scope.launch {
                            scrollOffset.snapTo(clampedOffset)
                        }
                        change.consume()
                    }

                    // Calculate velocity and start fling if needed
                    val velocity = velocityTracker.calculateVelocity()
                    val velocityY = -velocity.y // Invert because scroll direction is opposite

                    if (abs(velocityY) > 100f) { // Minimum velocity threshold
                        // Start fling before setting isDragging = false
                        isFling = true
                        isDragging = false

                        scope.launch {
                            // Fling animation
                            scrollOffset.animateDecay(
                                initialVelocity = velocityY,
                                animationSpec = exponentialDecay(
                                    frictionMultiplier = 3f,
                                    absVelocityThreshold = 50f
                                )
                            )

                            // Clamp to valid range after animation completes (for non-cyclic)
                            if (!cyclic) {
                                val maxOffset = (items.size - 1) * itemHeightPx
                                if (scrollOffset.value < 0f || scrollOffset.value > maxOffset) {
                                    scrollOffset.snapTo(scrollOffset.value.coerceIn(0f, maxOffset))
                                }
                            }

                            isFling = false
                        }
                    } else {
                        // No fling, just end dragging (will trigger snap)
                        isDragging = false
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Display items with clipping
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeightDp * visibleItemsCount)
                .clipToBounds() // Clip to bounds to hide overflow
        ) {
            val centerY = (visibleItemsCount / 2) * itemHeightPx

            // Only render items within visible range ±2
            val centerIndex = (scrollOffset.value / itemHeightPx).toInt()
            val startIndex = (centerIndex - 2).coerceAtLeast(0)
            val endIndex = (centerIndex + 3).coerceAtMost(displayItems.size)

            for (index in startIndex until endIndex) {
                if (index !in displayItems.indices) continue

                val item = displayItems[index]
                val itemY = index * itemHeightPx - scrollOffset.value + centerY

                // Calculate offset from center (in item units)
                val offsetFromCenter = (itemY - centerY) / itemHeightPx

                // Only render if within ±2.5 range (to allow smooth transitions)
                if (abs(offsetFromCenter) <= 2.5f) {
                    PickerItem(
                        value = item,
                        offsetFromCenter = offsetFromCenter,
                        itemHeight = itemHeightDp,
                        yPosition = itemY
                    )
                }
            }
        }
    }
}

@Composable
fun PickerItem(
    value: Int,
    offsetFromCenter: Float,
    itemHeight: androidx.compose.ui.unit.Dp,
    yPosition: Float
) {
    val absOffset = abs(offsetFromCenter)

    // Discrete transparency levels: 100%, 66%, 33%, 0%
    val alpha = when {
        absOffset < 0.5f -> 1.0f      // Center: 100%
        absOffset < 1.5f -> 0.66f     // ±1: 66%
        absOffset < 2.5f -> 0.33f     // ±2: 33%
        else -> 0.0f                  // Beyond ±2: 0% (invisible)
    }

    // Smooth interpolation for scale (1.0 at center, 0.6 at ±2)
    val scale = (1.0f - absOffset * 0.2f).coerceIn(0.6f, 1.0f)

    // Color interpolation from Black to LightGray
    val color = lerp(
        Color.Black,
        Color.LightGray,
        (absOffset * 0.4f).coerceIn(0f, 1f)
    )

    // Font weight based on distance
    val fontWeight = when {
        absOffset < 0.5f -> FontWeight.Bold
        absOffset < 1.5f -> FontWeight.Medium
        else -> FontWeight.Normal
    }

    // Base font size is 28sp, scales down
    val fontSize = (28f * scale).sp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(itemHeight)
            .graphicsLayer {
                translationY = yPosition - (itemHeight.toPx() / 2)
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            },
        contentAlignment = Alignment.Center
    ) {
        if (alpha > 0f) { // Only render if visible
            Text(
                text = if (value >= 100) String.format("%04d", value) else String.format("%02d", value),
                fontSize = fontSize,
                fontWeight = fontWeight,
                color = color,
                textAlign = TextAlign.Center
            )
        }
    }
}
