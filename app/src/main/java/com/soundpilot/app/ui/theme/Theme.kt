package com.soundpilot.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.soundpilot.app.data.ThemeMode

/**
 * هویت بصری SoundPilot:
 * پس‌زمینه شب‌شکافتی با ته‌مایه گرم + تأکید کهربایی درخشان + لهجه فیروزه‌ای.
 * رنگ‌های اضافی (گرادیان‌ها و شیشه) از SpColors خوانده می‌شود.
 */
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFC24B),
    onPrimary = Color(0xFF221600),
    primaryContainer = Color(0xFF4A3500),
    onPrimaryContainer = Color(0xFFFFDFA0),
    secondary = Color(0xFF7FD8C4),
    onSecondary = Color(0xFF00382C),
    secondaryContainer = Color(0xFF115043),
    onSecondaryContainer = Color(0xFFA2F1E0),
    tertiary = Color(0xFFB3C4FF),
    onTertiary = Color(0xFF152850),
    tertiaryContainer = Color(0xFF2C3D6B),
    onTertiaryContainer = Color(0xFFD6DFFF),
    background = Color(0xFF0A0C10),
    onBackground = Color(0xFFE8E5DE),
    surface = Color(0xFF101318),
    onSurface = Color(0xFFE8E5DE),
    surfaceContainer = Color(0xFF161A21),
    surfaceContainerHigh = Color(0xFF1F242D),
    surfaceContainerHighest = Color(0xFF282E39),
    surfaceVariant = Color(0xFF22262E),
    onSurfaceVariant = Color(0xFFB9B5AC),
    outline = Color(0xFF4A4E57),
    outlineVariant = Color(0xFF2E333C),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF8A5A00),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDDB0),
    onPrimaryContainer = Color(0xFF2B1A00),
    secondary = Color(0xFF3F6B5F),
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
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/**
 * رنگ‌های هویتی خارج از اسکیم متریال — گرادیان‌ها و لایه‌های شیشه‌ای.
 * فقط برای بعد بصری؛ رنگ‌کارکردی همچنان از MaterialTheme خوانده می‌شود.
 */
object SpColors {
    val Amber = Color(0xFFFFC24B)
    val AmberDeep = Color(0xFFFF8F1F)
    val Teal = Color(0xFF7FD8C4)
    val NightBase = Color(0xFF0A0C10)

    /** گرادیان اصلی دکمه‌ها و تاکیدها */
    val AmberGradient = Brush.linearGradient(listOf(Color(0xFFFFD54F), Color(0xFFFF9A2E)))

    /** درخشش اطراف دکمه پخش */
    val Glow = Color(0x66FFB350)

    /** لبه شیشه‌ای کارت‌ها در تم تیره */
    val GlassBorder = Color(0x14FFFFFF)
    val GlassFill = Color(0x0FFFFFFF)

    /** نوار پیشرفت گرادیانی */
    val ProgressGradient = Brush.horizontalGradient(
        listOf(Color(0xFFFFD54F), Color(0xFFFF9A2E), Color(0xFF7FD8C4)),
    )
}

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
