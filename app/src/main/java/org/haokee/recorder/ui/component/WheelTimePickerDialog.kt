package org.haokee.recorder.ui.component

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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

    // Adjust day if it exceeds the days in the selected month
    LaunchedEffect(daysInMonth) {
        if (selectedDay > daysInMonth) {
            selectedDay = daysInMonth
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
                        text = String.format("%04d/%02d/%02d", selectedYear, selectedMonth, selectedDay),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
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
                            selectedItem = selectedDay,
                            onItemSelected = { selectedDay = it },
                            modifier = Modifier.weight(1f),
                            cyclic = true
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
                            .height(180.dp),
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
    cyclic: Boolean = false
) {
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    // Find initial index
    val initialIndex = items.indexOf(selectedItem).takeIf { it >= 0 } ?: 0
    val visibleItemsCount = 5
    val itemHeight = 36.dp

    // For cyclic scrolling, we need to create an infinite list
    val displayItems = if (cyclic) {
        // Repeat items to create infinite scroll effect
        items + items + items
    } else {
        items
    }

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = if (cyclic) {
            initialIndex + items.size // Start at middle repetition
        } else {
            initialIndex
        }.coerceAtLeast(0)
    )

    var lastSelectedIndex by remember { mutableIntStateOf(-1) }

    // Track scroll position and trigger haptic feedback
    LaunchedEffect(listState.isScrollInProgress) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { index ->
                if (!listState.isScrollInProgress) {
                    val centerIndex = index + visibleItemsCount / 2
                    val actualIndex = if (cyclic) centerIndex % items.size else centerIndex

                    if (actualIndex in items.indices && actualIndex != lastSelectedIndex) {
                        lastSelectedIndex = actualIndex
                        onItemSelected(items[actualIndex])

                        // Trigger vibration
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    }

                    // For cyclic scroll, reset to middle repetition when needed
                    if (cyclic) {
                        if (centerIndex < items.size / 2) {
                            // Scrolled too far up, jump to middle repetition
                            scope.launch {
                                listState.scrollToItem(centerIndex + items.size)
                            }
                        } else if (centerIndex >= items.size * 2 + items.size / 2) {
                            // Scrolled too far down, jump to middle repetition
                            scope.launch {
                                listState.scrollToItem(centerIndex - items.size)
                            }
                        }
                    }
                }
            }
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(80.dp),
        contentAlignment = Alignment.Center
    ) {
        // Center highlight background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp)
                )
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight * visibleItemsCount),
            flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
        ) {
            // Add padding items at top
            items(visibleItemsCount / 2) {
                Spacer(modifier = Modifier.height(itemHeight))
            }

            // Display items
            items(displayItems.size) { index ->
                val item = displayItems[index]
                val offsetFromCenter = (index - listState.firstVisibleItemIndex) - (visibleItemsCount / 2)

                PickerItem(
                    value = item,
                    offsetFromCenter = offsetFromCenter,
                    itemHeight = itemHeight
                )
            }

            // Add padding items at bottom
            items(visibleItemsCount / 2) {
                Spacer(modifier = Modifier.height(itemHeight))
            }
        }
    }
}

@Composable
fun PickerItem(
    value: Int,
    offsetFromCenter: Int,
    itemHeight: androidx.compose.ui.unit.Dp
) {
    val absOffset = abs(offsetFromCenter).toFloat()

    // Calculate scale and alpha based on distance from center
    val scale = when {
        absOffset == 0f -> 1.0f
        absOffset == 1f -> 0.7f
        absOffset == 2f -> 0.5f
        else -> 0.3f
    }

    val alpha = when {
        absOffset == 0f -> 1.0f
        absOffset == 1f -> 0.6f
        absOffset == 2f -> 0.4f
        else -> 0.2f
    }

    val color = when {
        absOffset == 0f -> Color.Black
        absOffset == 1f -> Color.Gray
        else -> Color.LightGray
    }

    val fontWeight = when {
        absOffset == 0f -> FontWeight.Bold
        absOffset == 1f -> FontWeight.Medium
        else -> FontWeight.Normal
    }

    Text(
        text = String.format("%02d", value),
        modifier = Modifier
            .height(itemHeight)
            .fillMaxWidth()
            .wrapContentHeight()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            },
        fontSize = (20 * scale).sp,
        fontWeight = fontWeight,
        color = color,
        textAlign = TextAlign.Center
    )
}
