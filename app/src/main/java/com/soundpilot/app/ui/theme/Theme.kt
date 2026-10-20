package com.soundpilot.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.soundpilot.app.data.ThemeMode

// پالت تیره: پس‌زمینه زغالی + تأکید کهربایی (مناسب محیط خودرو)
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFC043),
    onPrimary = Color(0xFF241A00),
    primaryContainer = Color(0xFF4A3500),
    onPrimaryContainer = Color(0xFFFFDFA0),
    secondary = Color(0xFF9CD3A8),
    onSecondary = Color(0xFF0C2A18),
    secondaryContainer = Color(0xFF244434),
    onSecondaryContainer = Color(0xFFB4E6C6),
    background = Color(0xFF0E1013),
    onBackground = Color(0xFFE6E3DC),
    surface = Color(0xFF14171C),
    onSurface = Color(0xFFE6E3DC),
    surfaceContainer = Color(0xFF191D23),
    surfaceContainerHigh = Color(0xFF232830),
    surfaceVariant = Color(0xFF22262E),
    onSurfaceVariant = Color(0xFFB9B5AC),
    outline = Color(0xFF4A4E57),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF8A5A00),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDDB0),
    onPrimaryContainer = Color(0xFF2B1A00),
    secondary = Color(0xFF3F6B4F),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFFAF7F0),
    onBackground = Color(0xFF1C1B17),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1B17),
    surfaceVariant = Color(0xFFEDE7DA),
    onSurfaceVariant = Color(0xFF4C4639),
    outline = Color(0xFF7D7667),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun SoundPilotTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit,
) {
    val useDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val scheme = if (useDark) DarkColorScheme else LightColorScheme

    // برنامه کاملاً فارسی است؛ جهت چیدمان به‌صورت قطعی راست‌چین می‌شود
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = scheme,
            shapes = AppShapes,
            content = content,
        )
    }
}
