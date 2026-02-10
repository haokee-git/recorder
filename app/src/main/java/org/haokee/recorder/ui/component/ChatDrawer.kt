package org.haokee.recorder.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import org.haokee.recorder.ui.viewmodel.ChatMessage
import org.haokee.recorder.ui.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDrawer(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    // Scroll to bottom when new message is added or streaming content changes
    val lastMessageContent = uiState.messages.lastOrNull()?.content
    LaunchedEffect(uiState.messages.size, lastMessageContent) {
        if (uiState.messages.isNotEmpty()) {
            listState.scrollToItem(uiState.messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "AI 对话",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

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
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Input area - clear context button on left, input field, send button on right
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Clear context button — blue when enabled, disabled while streaming
            val clearEnabled = uiState.messages.isNotEmpty() && !uiState.isLoading
            IconButton(
                onClick = { viewModel.clearContext() },
                enabled = clearEnabled
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "清除上下文",
                    tint = if (clearEnabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    }
                )
            }

            val inputEnabled = uiState.isLLMEnabled && uiState.isLLMConfigured
            val inputInteractionSource = remember { MutableInteractionSource() }
            BasicTextField(
                value = uiState.inputText,
                onValueChange = { viewModel.updateInputText(it) },
                modifier = Modifier.weight(1f),
                enabled = inputEnabled,
                maxLines = 4,
                textStyle = MaterialTheme.typography.bodyLarge.copy( 
                    color = if (inputEnabled) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                ),
                interactionSource = inputInteractionSource,
                decorationBox = @Composable { innerTextField ->
                    OutlinedTextFieldDefaults.DecorationBox(
                        value = uiState.inputText,
                        innerTextField = innerTextField,
                        enabled = inputEnabled,
                        singleLine = false,
                        visualTransformation = VisualTransformation.None,
                        interactionSource = inputInteractionSource,
                        placeholder = { Text("输入消息...") },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 11.dp),
                        container = {
                            OutlinedTextFieldDefaults.ContainerBox(
                                enabled = inputEnabled,
                                isError = false,
                                interactionSource = inputInteractionSource,
                                colors = OutlinedTextFieldDefaults.colors(),
                                shape = OutlinedTextFieldDefaults.shape
                            )
                        }
                    )
                }
            )

            // 接收中：红色停止按钮；否则：蓝色发送按钮
            if (uiState.isLoading) {
                IconButton(onClick = { viewModel.stopStreaming() }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "停止",
                        tint = Color.Red
                    )
                }
            } else {
                val sendEnabled = uiState.inputText.isNotBlank() &&
                        uiState.isLLMEnabled &&
                        uiState.isLLMConfigured
                IconButton(
                    onClick = { viewModel.sendMessage() },
                    enabled = sendEnabled
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "发送",
                        tint = if (sendEnabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatMessageItem(message: ChatMessage) {
    when (message.role) {
        "user" -> UserMessageBubble(message.content)
        "assistant" -> AssistantMessageBubble(message.content, message.isStreaming)
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
private fun AssistantMessageBubble(content: String, isStreaming: Boolean = false) {
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
            if (content.isEmpty() && isStreaming) {
                // Show loading spinner while waiting for first token
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            } else {
                // Show content with streaming cursor if still streaming
                val displayContent = if (isStreaming) "$content▌" else content
                MarkdownText(
                    markdown = displayContent,
                    modifier = Modifier.fillMaxWidth()
                )
            }
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
