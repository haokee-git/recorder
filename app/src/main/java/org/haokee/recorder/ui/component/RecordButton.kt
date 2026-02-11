package org.haokee.recorder.ui.component

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun RecordButton(
    isRecording: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current

    // Animate corner radius: circle (36.dp) to rounded rect (12.dp)
    val cornerRadius by animateDpAsState(
        targetValue = if (isRecording) 12.dp else 36.dp,
        animationSpec = tween(durationMillis = 200),
        label = "cornerRadius"
    )

    FloatingActionButton(
        onClick = {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            onClick()
        },
        containerColor = if (isRecording) MaterialTheme.colorScheme.error else Color(0xFF4CAF50),
        shape = RoundedCornerShape(cornerRadius),
        modifier = modifier.size(72.dp)
    ) {
        Icon(
            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
            contentDescription = if (isRecording) "停止录音" else "开始录音",
            tint = Color.White,
            modifier = Modifier.size(36.dp)
        )
    }
}
