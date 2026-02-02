package org.haokee.recorder.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.haokee.recorder.ui.viewmodel.ChatMessage
import org.haokee.recorder.ui.viewmodel.ChatViewModel

@Composable
fun ChatDrawer(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new message arrives
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(360.dp)
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "AI 对话",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            IconButton(onClick = { viewModel.clearContext() }) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "清除上下文"
                )
            }
        }

        // Status indicator
        if (!uiState.isLLMEnabled || !uiState.isLLMConfigured) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = if (!uiState.isLLMEnabled) {
                        "大模型功能未启用，请在设置中启用"
                    } else {
                        "未配置 API Key，请在设置中配置"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Messages list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(uiState.messages) { message ->
                ChatMessageItem(message = message)
            }

            // Loading indicator
            if (uiState.isLoading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Input area
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = uiState.inputText,
                onValueChange = { viewModel.updateInputText(it) },
                placeholder = { Text("输入消息...") },
                modifier = Modifier.weight(1f),
                enabled = uiState.isLLMEnabled && uiState.isLLMConfigured && !uiState.isLoading,
                maxLines = 4
            )

            IconButton(
                onClick = { viewModel.sendMessage() },
                enabled = uiState.inputText.isNotBlank() &&
                        uiState.isLLMEnabled &&
                        uiState.isLLMConfigured &&
                        !uiState.isLoading
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "发送",
                    tint = if (uiState.inputText.isNotBlank() &&
                        uiState.isLLMEnabled &&
                        uiState.isLLMConfigured &&
                        !uiState.isLoading
                    ) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    }
                )
            }
        }
    }

    // Error snackbar
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // TODO: Show snackbar
            viewModel.clearError()
        }
    }
}

@Composable
private fun ChatMessageItem(message: ChatMessage) {
    when (message.role) {
        "user" -> UserMessageBubble(message.content)
        "assistant" -> AssistantMessageBubble(message.content)
        "system" -> SystemMessageDivider(message.content)
    }
}

@Composable
private fun UserMessageBubble(content: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(12.dp)
                .widthIn(max = 280.dp)
        ) {
            Text(
                text = content,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun AssistantMessageBubble(content: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .padding(12.dp)
                .widthIn(max = 280.dp)
        ) {
            MarkdownText(
                markdown = content,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SystemMessageDivider(content: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f))
        Text(
            text = content,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        HorizontalDivider(modifier = Modifier.weight(1f))
    }
}
