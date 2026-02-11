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
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
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
import android.view.HapticFeedbackConstants
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import org.haokee.recorder.ui.viewmodel.ChatMessage
import org.haokee.recorder.ui.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDrawer(
    viewModel: ChatViewModel,
    onClose: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    // Scroll to bottom when new message is added or streaming content changes
    val lastMessageContent = uiState.messages.lastOrNull()?.content
    LaunchedEffect(uiState.messages.size, lastMessageContent) {
        if (uiState.messages.isNotEmpty()) {
            listState.scrollToItem(uiState.messages.size - 1)
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
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
            IconButton(onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onClose()
            }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "关闭"
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
                ChatMessageItem(
                    message = message,
                    isLoading = uiState.isLoading,
                    onRegenerate = { viewModel.regenerate(it) }
                )
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
                            OutlinedTextFieldDefaults.Container(
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

            // 接收中：红底白色停止按钮（与录音按钮风格一致）；否则：蓝色发送按钮
            if (uiState.isLoading) {
                FilledIconButton(
                    onClick = { viewModel.stopStreaming() },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color.Red,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "停止"
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
                        imageVector = Icons.AutoMirrored.Filled.Send,
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
}

@Composable
private fun ChatMessageItem(
    message: ChatMessage,
    isLoading: Boolean,
    onRegenerate: (String) -> Unit
) {
    when (message.role) {
        "user" -> UserMessageBubble(message.content)
        "assistant" -> AssistantMessageBubble(message, isLoading, onRegenerate)
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
            SelectionContainer {
                Text(
                    text = content,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun AssistantMessageBubble(
    message: ChatMessage,
    isLoading: Boolean,
    onRegenerate: (String) -> Unit
) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .padding(12.dp)
                .widthIn(max = 280.dp)
        ) {
            if (message.content.isEmpty() && message.isStreaming) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            } else {
                val displayContent = if (message.isStreaming) "${message.content}▌" else message.content
                MarkdownText(
                    markdown = displayContent,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Copy / Regenerate buttons — only when not streaming
        if (!message.isStreaming) {
            val copyEnabled = message.content.isNotEmpty()
            Row(
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("AI回复", message.content))
                        android.widget.Toast.makeText(context, "已复制到剪切板", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    enabled = copyEnabled,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "复制",
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("复制", style = MaterialTheme.typography.labelSmall)
                }
                TextButton(
                    onClick = { onRegenerate(message.id) },
                    enabled = !isLoading,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "重新生成",
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("重新生成", style = MaterialTheme.typography.labelSmall)
                }
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
