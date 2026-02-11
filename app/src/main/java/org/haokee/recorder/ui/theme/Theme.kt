package org.haokee.recorder.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Dark scheme: same hue relationships as light, just inverted for dark background
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF64B5F6),            // bright blue (light: 0xFF004370 dark blue)
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF1A3A5C),    // dark blue tint (light: 0xFFBBDEFB light blue)
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = Color(0xFF90CAF9),
    onSecondary = Color(0xFF003258),
    secondaryContainer = Color(0xFF303030),  // neutral dark grey (light: Material default grey)
    onSecondaryContainer = Color(0xFFE0E0E0),
    tertiary = Color(0xFFBBDEFB),
    onTertiary = Color(0xFF003258),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE0E0E0),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFFB0B0B0),
    outline = Color(0xFF64B5F6),
    outlineVariant = Color(0xFF444444),
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF600000),
    errorContainer = Color(0xFF93000A),
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
