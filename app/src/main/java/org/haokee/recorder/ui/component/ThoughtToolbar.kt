package org.haokee.recorder.ui.component

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ThoughtToolbar(
    hasSelection: Boolean,
    isSingleSelection: Boolean,
    isAllTranscribed: Boolean,
    isDeletePending: Boolean,
    onBatchConvertClick: () -> Unit,
    onEditClick: () -> Unit,
    onSetAlarmClick: () -> Unit,
    onSetColorClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onFilterClick: () -> Unit,
    activeFilterCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 0.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ToolbarButton(
                    icon = Icons.Default.Refresh,
                    text = "转换",
                    enabled = hasSelection && !isAllTranscribed,
                    onClick = onBatchConvertClick,
                    view = view
                )
                ToolbarButton(
                    icon = Icons.Default.Edit,
                    text = "编辑",
                    enabled = isSingleSelection,
                    onClick = onEditClick,
                    view = view
                )
                ToolbarButton(
                    icon = Icons.Default.Notifications,
                    text = "提醒",
                    enabled = hasSelection,
                    onClick = onSetAlarmClick,
                    view = view
                )
                ToolbarButton(
                    icon = Icons.Default.Circle,
                    text = "颜色",
                    enabled = hasSelection,
                    onClick = onSetColorClick,
                    view = view
                )
                DeleteButton(
                    enabled = hasSelection,
                    isPending = isDeletePending,
                    onClick = onDeleteClick,
                    view = view
                )
            }

            Box {
                IconButton(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onFilterClick()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "筛选",
                        modifier = Modifier.size(18.dp)
                    )
                }
                if (activeFilterCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-4).dp, y = 4.dp)
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = activeFilterCount.toString(),
                            color = MaterialTheme.colorScheme.onError,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            lineHeight = 9.sp
                        )
                    }
                }
            }
        }
}

@Composable
private fun DeleteButton(
    enabled: Boolean,
    isPending: Boolean,
    onClick: () -> Unit,
    view: android.view.View
) {
    TextButton(
        onClick = {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            onClick()
        },
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(
            containerColor = if (isPending) MaterialTheme.colorScheme.error else androidx.compose.ui.graphics.Color.Transparent,
            contentColor = if (isPending) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.primary,
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        ),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        modifier = Modifier.height(32.dp),
        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = if (isPending) "确定" else "删除",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isPending) "确定" else "删除",
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun ToolbarButton(
    icon: ImageVector,
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    view: android.view.View
) {
    TextButton(
        onClick = {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            onClick()
        },
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.primary,
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        ),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        modifier = Modifier.height(32.dp),
        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}
