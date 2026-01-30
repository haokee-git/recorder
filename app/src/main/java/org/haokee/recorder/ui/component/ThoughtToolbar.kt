package org.haokee.recorder.ui.component

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun ThoughtToolbar(
    hasSelection: Boolean,
    isSingleSelection: Boolean,
    isAllTranscribed: Boolean,
    onBatchConvertClick: () -> Unit,
    onEditClick: () -> Unit,
    onSetAlarmClick: () -> Unit,
    onSetColorClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onFilterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                    onClick = onBatchConvertClick
                )
                ToolbarButton(
                    icon = Icons.Default.Edit,
                    text = "编辑",
                    enabled = isSingleSelection,
                    onClick = onEditClick
                )
                ToolbarButton(
                    icon = Icons.Default.Notifications,
                    text = "提醒",
                    enabled = hasSelection,
                    onClick = onSetAlarmClick
                )
                ToolbarButton(
                    icon = Icons.Default.Circle,
                    text = "颜色",
                    enabled = hasSelection,
                    onClick = onSetColorClick
                )
                ToolbarButton(
                    icon = Icons.Default.Delete,
                    text = "删除",
                    enabled = hasSelection,
                    onClick = onDeleteClick
                )
            }

            IconButton(onClick = onFilterClick) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "筛选",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
}

@Composable
private fun ToolbarButton(
    icon: ImageVector,
    text: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.primary,
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        ),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
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
