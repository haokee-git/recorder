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
    val view = LocalView.current
    val currentTime = LocalDateTime.now()
    var selectedYear by remember { mutableIntStateOf(currentTime.year) }
    var selectedMonth by remember { mutableIntStateOf(currentTime.monthValue) }
    var selectedDay by remember { mutableIntStateOf(currentTime.dayOfMonth) }
    var selectedHour by remember { mutableIntStateOf(currentTime.hour) }
    var selectedMinute by remember { mutableIntStateOf(currentTime.minute) }
    var isDayAdjusting by remember { mutableStateOf(false) }

    // Calculate days in month (considering leap year)
    val daysInMonth = remember(selectedYear, selectedMonth) {
        YearMonth.of(selectedYear, selectedMonth).lengthOfMonth()
    }

    // Adjust day immediately to ensure it's always valid
    val effectiveDay = selectedDay.coerceIn(1, daysInMonth)

    // Update selectedDay state when adjusted
    LaunchedEffect(effectiveDay) {
        if (selectedDay != effectiveDay) {
            isDayAdjusting = true
            selectedDay = effectiveDay
            // 短暂延迟后重置标志，确保振动已被抑制
            kotlinx.coroutines.delay(100)
            isDayAdjusting = false
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
                            modifier = Modifier.weight(1f),
                            suppressVibration = true // 年改变时日会振动，所以年本身不振动
                        )

                        // Month picker
                        DrumRollPicker(
                            items = (1..12).toList(),
                            selectedItem = selectedMonth,
                            onItemSelected = { selectedMonth = it },
                            modifier = Modifier.weight(1f),
                            cyclic = true,
                            suppressVibration = true // 月改变时日会振动，所以月本身不振动
                        )

                        // Day picker
                        DrumRollPicker(
                            items = (1..daysInMonth).toList(),
                            selectedItem = effectiveDay,
                            onItemSelected = { selectedDay = it },
                            modifier = Modifier.weight(1f),
                            cyclic = true,
                            key = "$selectedYear-$selectedMonth-$daysInMonth",
                            suppressVibration = isDayAdjusting
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
                    Button(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            val alarmTime = LocalDateTime.of(
                                selectedYear,
                                selectedMonth,
                                selectedDay,
                                selectedHour,
                                selectedMinute
                            )
                            onTimeSelected(alarmTime)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
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
    key: String? = null,
    suppressVibration: Boolean = false
) {
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val visibleItemsCount = 5
    val itemHeightDp = 40.dp
    val itemHeightPx = with(density) { itemHeightDp.toPx() }

    // Find initial index
    val initialIndex = items.indexOf(selectedItem).coerceAtLeast(0)

    // Calculate initial scroll offset (in item units, not pixels)
    val initialScrollIndex = remember(initialIndex, key) { initialIndex.toFloat() }

    // Scroll offset in item units (can be any value for cyclic mode)
    val scrollIndex = remember(key) { Animatable(initialScrollIndex) }
    var isDragging by remember { mutableStateOf(false) }
    var isFling by remember { mutableStateOf(false) }
    var lastNotifiedValue by remember { mutableIntStateOf(-1) }

    // Convert item units to pixels for rendering
    val scrollOffset by remember { derivedStateOf { scrollIndex.value * itemHeightPx } }

    // Reset scroll position when key changes
    LaunchedEffect(key, initialScrollIndex) {
        if (!isDragging && !isFling) {
            scrollIndex.snapTo(initialScrollIndex)
            lastNotifiedValue = -1
        }
    }

    // Update selected item in real-time
    LaunchedEffect(key, items.size) {
        snapshotFlow { scrollIndex.value }
            .collect { indexFloat ->
                val roundedIndex = indexFloat.roundToInt()
                val actualIndex = if (cyclic) {
                    // Modulo operation handling negative numbers
                    val mod = roundedIndex % items.size
                    if (mod < 0) mod + items.size else mod
                } else {
                    roundedIndex.coerceIn(0, items.lastIndex)
                }

                val actualValue = items[actualIndex]
                if (actualValue != lastNotifiedValue) {
                    onItemSelected(actualValue)
                    lastNotifiedValue = actualValue
                    // 值改变时立即振动（除非被抑制）
                    if (!suppressVibration) {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    }
                }
            }
    }

    // Snap animation when user releases
    LaunchedEffect(isDragging, isFling) {
        if (!isDragging && !isFling) {
            val targetIndex = scrollIndex.value.roundToInt()

            scrollIndex.animateTo(
                targetValue = targetIndex.toFloat(),
                animationSpec = tween(durationMillis = 200)
            )
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
                        velocityTracker.addPosition(change.uptimeMillis, change.position)

                        val dragAmount = change.positionChange().y
                        val dragInItems = -dragAmount / itemHeightPx
                        val newIndex = scrollIndex.value + dragInItems

                        // For non-cyclic, clamp the index
                        val clampedIndex = if (cyclic) {
                            newIndex
                        } else {
                            newIndex.coerceIn(0f, items.lastIndex.toFloat())
                        }

                        scope.launch {
                            scrollIndex.snapTo(clampedIndex)
                        }
                        change.consume()
                    }

                    // Calculate velocity and start fling if needed
                    val velocity = velocityTracker.calculateVelocity()
                    val velocityY = -velocity.y / itemHeightPx // Convert to items per second

                    if (abs(velocityY) > 100f / itemHeightPx) {
                        isFling = true
                        isDragging = false

                        scope.launch {
                            scrollIndex.animateDecay(
                                initialVelocity = velocityY,
                                animationSpec = exponentialDecay(
                                    frictionMultiplier = 3f,
                                    absVelocityThreshold = 50f / itemHeightPx
                                )
                            )

                            // Clamp for non-cyclic after fling
                            if (!cyclic) {
                                if (scrollIndex.value < 0f || scrollIndex.value > items.lastIndex) {
                                    scrollIndex.snapTo(scrollIndex.value.coerceIn(0f, items.lastIndex.toFloat()))
                                }
                            }

                            isFling = false
                        }
                    } else {
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
                .clipToBounds()
        ) {
            val centerY = (visibleItemsCount / 2f) * itemHeightPx
            val centerIndex = scrollIndex.value.toInt()

            // Only render ±7 items around center (total 15 items max)
            for (offset in -7..7) {
                val index = centerIndex + offset

                // For cyclic mode, use modulo to get actual item
                val actualIndex = if (cyclic) {
                    val mod = index % items.size
                    if (mod < 0) mod + items.size else mod
                } else {
                    if (index !in items.indices) continue
                    index
                }

                val item = items[actualIndex]
                val itemY = (index - scrollIndex.value) * itemHeightPx + centerY

                // Calculate offset from center
                val offsetFromCenter = (index - scrollIndex.value)

                // Only render if within visible range (±2.5)
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
                translationY = yPosition
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            },
        contentAlignment = Alignment.Center
    ) {
        if (alpha > 0f) {
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
