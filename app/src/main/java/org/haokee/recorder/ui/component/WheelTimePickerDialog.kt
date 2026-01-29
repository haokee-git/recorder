package org.haokee.recorder.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.time.LocalDateTime
import java.time.YearMonth
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

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

    var showYearMonthPicker by remember { mutableStateOf(false) }

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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "设置提醒时间",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                // Year and Month selector
                OutlinedCard(
                    onClick = { showYearMonthPicker = !showYearMonthPicker },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${selectedYear}年 ${selectedMonth}月",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "展开"
                        )
                    }
                }

                // Year and Month picker (floating box)
                if (showYearMonthPicker) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Year selector
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("年份", style = MaterialTheme.typography.labelMedium)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    FilledTonalButton(
                                        onClick = { selectedYear-- },
                                        modifier = Modifier.size(40.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("-")
                                    }
                                    Text(
                                        text = selectedYear.toString(),
                                        modifier = Modifier
                                            .width(80.dp)
                                            .align(Alignment.CenterVertically),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    FilledTonalButton(
                                        onClick = { selectedYear++ },
                                        modifier = Modifier.size(40.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("+")
                                    }
                                }
                            }

                            // Month selector
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("月份", style = MaterialTheme.typography.labelMedium)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    FilledTonalButton(
                                        onClick = {
                                            selectedMonth = if (selectedMonth == 1) 12 else selectedMonth - 1
                                        },
                                        modifier = Modifier.size(40.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("-")
                                    }
                                    Text(
                                        text = selectedMonth.toString(),
                                        modifier = Modifier
                                            .width(60.dp)
                                            .align(Alignment.CenterVertically),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    FilledTonalButton(
                                        onClick = {
                                            selectedMonth = if (selectedMonth == 12) 1 else selectedMonth + 1
                                        },
                                        modifier = Modifier.size(40.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("+")
                                    }
                                }
                            }
                        }
                    }
                }

                Divider()

                // Semicircle wheel pickers
                Text(
                    text = "日期",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                SemicircleWheelPicker(
                    items = (1..daysInMonth).toList(),
                    selectedItem = selectedDay,
                    onItemSelected = { selectedDay = it },
                    itemLabel = { "${it}日" }
                )

                Divider()

                Text(
                    text = "时间",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Hour picker
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("小时", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        SemicircleWheelPicker(
                            items = (0..23).toList(),
                            selectedItem = selectedHour,
                            onItemSelected = { selectedHour = it },
                            itemLabel = { it.toString().padStart(2, '0') }
                        )
                    }

                    // Minute picker
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("分钟", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        SemicircleWheelPicker(
                            items = (0..59).toList(),
                            selectedItem = selectedMinute,
                            onItemSelected = { selectedMinute = it },
                            itemLabel = { it.toString().padStart(2, '0') }
                        )
                    }
                }

                Divider()

                // Preview
                Text(
                    text = "提醒时间: ${selectedYear}年${selectedMonth}月${selectedDay}日 ${selectedHour.toString().padStart(2, '0')}:${selectedMinute.toString().padStart(2, '0')}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )

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
fun SemicircleWheelPicker(
    items: List<Int>,
    selectedItem: Int,
    onItemSelected: (Int) -> Unit,
    itemLabel: (Int) -> String,
    modifier: Modifier = Modifier
) {
    var offsetAngle by remember { mutableFloatStateOf(0f) }
    val animatedOffset by animateFloatAsState(
        targetValue = offsetAngle,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "wheel_animation"
    )

    // Calculate angle per item
    val anglePerItem = 180f / (items.size - 1)
    val currentIndex = items.indexOf(selectedItem)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        // Snap to nearest item
                        val targetIndex = ((-animatedOffset / anglePerItem).roundToInt()).coerceIn(0, items.lastIndex)
                        offsetAngle = -targetIndex * anglePerItem
                        onItemSelected(items[targetIndex])
                    }
                ) { change, dragAmount ->
                    change.consume()
                    // Update offset based on drag
                    val newOffset = offsetAngle + dragAmount.x * 0.5f
                    offsetAngle = newOffset.coerceIn(-(items.lastIndex * anglePerItem), 0f)
                }
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center)
        ) {
            val centerX = size.width / 2
            val centerY = size.height
            val radius = size.height * 0.8f

            // Draw semicircle arc
            drawArc(
                color = Color.Gray.copy(alpha = 0.3f),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                style = Stroke(width = 2.dp.toPx()),
                topLeft = Offset(centerX - radius, centerY - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
            )

            // Draw items on the semicircle
            items.forEachIndexed { index, item ->
                val angle = 180f - (index * anglePerItem) + animatedOffset
                if (angle in 0f..180f) {
                    val radian = Math.toRadians(angle.toDouble())
                    val x = (centerX + radius * cos(radian)).toFloat()
                    val y = (centerY - radius * sin(radian)).toFloat()

                    // Calculate opacity based on distance from center
                    val distanceFromCenter = abs(angle - 90f)
                    val opacity = (1f - (distanceFromCenter / 90f)).coerceIn(0.3f, 1f)

                    // Draw point
                    drawCircle(
                        color = if (index == currentIndex) Color.Blue else Color.Gray,
                        radius = if (index == currentIndex) 8.dp.toPx() else 4.dp.toPx(),
                        center = Offset(x, y),
                        alpha = opacity
                    )
                }
            }
        }

        // Display selected item
        Text(
            text = itemLabel(items[(-animatedOffset / anglePerItem).roundToInt().coerceIn(0, items.lastIndex)]),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
        )
    }
}
