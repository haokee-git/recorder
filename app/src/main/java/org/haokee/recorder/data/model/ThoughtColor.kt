package org.haokee.recorder.data.model

import androidx.compose.ui.graphics.Color

enum class ThoughtColor(val displayName: String, val color: Color) {
    RED("红", Color(0xFFE53935)),
    ORANGE("橙", Color(0xFFFB8C00)),
    YELLOW("黄", Color(0xFFFDD835)),
    GREEN("绿", Color(0xFF43A047)),
    CYAN("青", Color(0xFF00ACC1)),
    BLUE("蓝", Color(0xFF1E88E5)),
    PURPLE("紫", Color(0xFF8E24AA)),
    BLACK("黑", Color(0xFF424242))
}
