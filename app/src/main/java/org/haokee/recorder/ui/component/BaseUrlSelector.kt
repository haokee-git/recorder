package org.haokee.recorder.ui.component

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.haokee.recorder.data.model.BaseUrlPreset

@Composable
fun BaseUrlSelector(
    presets: List<BaseUrlPreset>,
    selectedPresetId: String,
    isExpanded: Boolean,
    enabled: Boolean,
    onToggleExpand: () -> Unit,
    onSelectPreset: (String) -> Unit,
    onEditPreset: (id: String, name: String, url: String) -> Unit,
    onDeletePreset: (String) -> Unit,
    onAddPreset: (name: String, url: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val selectedPreset = presets.find { it.id == selectedPresetId }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingPreset by remember { mutableStateOf<BaseUrlPreset?>(null) }
    var deletingPresetId by remember { mutableStateOf<String?>(null) }

    val contentAlpha = if (enabled) 1f else 0.38f

    Surface(
        modifier = modifier
            .border(
                1.dp,
                if (enabled) MaterialTheme.colorScheme.outline
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        Column {
            // Collapsed header - always visible
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = enabled) {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onToggleExpand()
                    }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Link,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selectedPreset?.name ?: "Base URL",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
                    )
                    Text(
                        text = selectedPreset?.url ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "收起" else "展开",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
                )
            }

            // Expanded list
            AnimatedVisibility(
                visible = isExpanded && enabled,
                enter = expandVertically(
                    animationSpec = tween(durationMillis = 200),
                    expandFrom = Alignment.Top
                ),
                exit = shrinkVertically(
                    animationSpec = tween(durationMillis = 200),
                    shrinkTowards = Alignment.Top
                )
            ) {
                Column {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    presets.forEach { preset ->
                        PresetRow(
                            preset = preset,
                            isSelected = preset.id == selectedPresetId,
                            onSelect = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                onSelectPreset(preset.id)
                            },
                            onEdit = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                editingPreset = preset
                            },
                            onDelete = if (!preset.isBuiltIn) {
                                {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    deletingPresetId = preset.id
                                }
                            } else null
                        )
                    }

                    // Add new button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                showAddDialog = true
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "新建 Base URL",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }

    // Add dialog
    if (showAddDialog) {
        PresetEditDialog(
            title = "新建 Base URL",
            initialName = "",
            initialUrl = "",
            showNameField = true,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, url ->
                onAddPreset(name, url)
                showAddDialog = false
            }
        )
    }

    // Edit dialog
    editingPreset?.let { preset ->
        PresetEditDialog(
            title = "编辑 Base URL",
            initialName = preset.name,
            initialUrl = preset.url,
            showNameField = !preset.isBuiltIn,
            onDismiss = { editingPreset = null },
            onConfirm = { name, url ->
                onEditPreset(preset.id, name, url)
                editingPreset = null
            }
        )
    }

    // Delete confirmation dialog
    deletingPresetId?.let { presetId ->
        val presetName = presets.find { it.id == presetId }?.name ?: ""
        AlertDialog(
            onDismissRequest = { deletingPresetId = null },
            title = { Text("删除 Base URL") },
            text = { Text("确定要删除「$presetName」吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeletePreset(presetId)
                        deletingPresetId = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingPresetId = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun PresetRow(
    preset: BaseUrlPreset,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onSelect,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = preset.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = preset.url,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Default.Edit,
                contentDescription = "编辑",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (onDelete != null) {
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun PresetEditDialog(
    title: String,
    initialName: String,
    initialUrl: String,
    showNameField: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (name: String, url: String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var url by remember { mutableStateOf(initialUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                if (showNameField) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                }
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Base URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, url) },
                enabled = url.isNotBlank() && (!showNameField || name.isNotBlank())
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
