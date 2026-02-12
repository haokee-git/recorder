package org.haokee.recorder.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Dark scheme: bright = primary (主), dim = secondary/outline (辅)
// 亮的为主，暗的为辅，主辅之间有明显亮度差
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF64B5F6),             // 亮蓝 — 主色
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF1A3A5C),     // 深蓝底 — 用户气泡等容器
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = Color(0xFF1C4966),            // 很暗的蓝 — 辅色，亮度约为 primary 的 1/3
    onSecondary = Color(0xFFE0E0E0),
    secondaryContainer = Color(0xFF303030),   // 中性灰底 — AI 气泡等容器
    onSecondaryContainer = Color(0xFFE0E0E0),
    tertiary = Color(0xFF133347),             // 极暗蓝 — 第三级
    onTertiary = Color(0xFFE0E0E0),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE0E0E0),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFFB0B0B0),
    outline = Color(0xFF1C4966),              // 很暗的蓝 — 和 secondary 同级
    outlineVariant = Color(0xFF444444),
    error = Color(0xFFCF6679),                // 柔红 — 主错误色，不过亮
    onError = Color(0xFF600000),
    errorContainer = Color(0xFF3A1111),       // 极深红底 — 亮度约为 error 的 1/4
    onErrorContainer = Color(0xFFFFDAD6),
    surfaceContainer = Color(0xFF252525),
    surfaceContainerHigh = Color(0xFF2C2C2C),
    surfaceContainerHighest = Color(0xFF333333)
)

private val LightColorScheme = lightColorScheme(
    primary = Blue40,
    onPrimary = Color.White,
    primaryContainer = LightBlue80,
    onPrimaryContainer = Blue40,
    secondary = BlueGrey40,
    onSecondary = Color.White,
    tertiary = LightBlue40,
    onTertiary = Color.White,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = Blue40,
    onSurfaceVariant = Color(0xFF5F5F5F),
    outline = Blue80,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Color.White,
    outlineVariant = Color(0xFFBDBDBD)
)

@Composable
fun RecorderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
