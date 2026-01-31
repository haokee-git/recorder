package org.haokee.recorder.ui.component

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

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
            containerColor = if (isPending) androidx.compose.ui.graphics.Color.Red else androidx.compose.ui.graphics.Color.Transparent,
            contentColor = if (isPending) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.primary,
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        ),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
        modifier = Modifier.height(24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = if (isPending) "确定" else "删除",
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = if (isPending) "确定" else "删除",
                style = MaterialTheme.typography.labelSmall
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
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
        modifier = Modifier.height(24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
