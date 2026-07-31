package com.gratitudelogger.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.gratitudelogger.theme.AppTheme

data class HeaderColors(val container: Color, val content: Color)

val LocalHeaderColors = staticCompositionLocalOf { HeaderColors(Color(0xFFFF6B35), Color.White) }

private data class AppThemeColors(
    val colorScheme: ColorScheme,
    val header: HeaderColors
)

private val SunsetGoldColors = AppThemeColors(
    colorScheme = lightColorScheme(
        primary = Color(0xFFE85D04),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFFFB662),
        onPrimaryContainer = Color(0xFF5C4033),
        secondary = Color(0xFFFF9F1C),
        onSecondary = Color(0xFF5C4033),
        secondaryContainer = Color(0xFFEAD8C8),
        onSecondaryContainer = Color(0xFF5C4033),
        tertiary = Color(0xFFE83C24),
        onTertiary = Color.White,
        background = Color(0xFFFFFDF7),
        onBackground = Color(0xFF5C4033),
        surface = Color(0xFFFAF3E8),
        onSurface = Color(0xFF7A6251),
        surfaceVariant = Color(0xFFEAD8C8),
        onSurfaceVariant = Color(0xFF9B8070),
        outline = Color(0xFFEBDCC6),
        outlineVariant = Color(0xFFEBDCC6),
        error = Color(0xFFD07D39),
        onError = Color.White,
        surfaceContainerLowest = Color(0xFFFFFDF7),
        surfaceContainerLow = Color(0xFFFAF3E8),
        surfaceContainer = Color(0xFFFAF3E8),
        surfaceContainerHigh = Color(0xFFEAD8C8),
        surfaceContainerHighest = Color(0xFFEAD8C8)
    ),
    header = HeaderColors(container = Color(0xFFFF6B35), content = Color.White)
)

private val GoldenHourColors = AppThemeColors(
    colorScheme = lightColorScheme(
        primary = Color(0xFFFFB347),
        onPrimary = Color(0xFF4A3B2C),
        primaryContainer = Color(0xFFFFD9A0),
        onPrimaryContainer = Color(0xFF4A3B2C),
        secondary = Color(0xFFE86A32),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFFBD8C4),
        onSecondaryContainer = Color(0xFF4A3B2C),
        tertiary = Color(0xFFC0392B),
        onTertiary = Color.White,
        background = Color(0xFFFDF5EA),
        onBackground = Color(0xFF4A3B2C),
        surface = Color(0xFFFBFAF4),
        onSurface = Color(0xFF6D5E51),
        surfaceVariant = Color(0xFFF0E4D3),
        onSurfaceVariant = Color(0xFF8A7A6B),
        outline = Color(0xFFE8DCC8),
        outlineVariant = Color(0xFFE8DCC8),
        error = Color(0xFFC0392B),
        onError = Color.White,
        surfaceContainerLowest = Color(0xFFFDF5EA),
        surfaceContainerLow = Color(0xFFFBFAF4),
        surfaceContainer = Color(0xFFFBFAF4),
        surfaceContainerHigh = Color(0xFFF0E4D3),
        surfaceContainerHighest = Color(0xFFF0E4D3)
    ),
    header = HeaderColors(container = Color(0xFFC19D5F), content = Color(0xFF4A3B2C))
)

private val TerraCottaColors = AppThemeColors(
    colorScheme = lightColorScheme(
        primary = Color(0xFFC07F42),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFE8C39D),
        onPrimaryContainer = Color(0xFF4A3320),
        secondary = Color(0xFFA97C50),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFEDD9C4),
        onSecondaryContainer = Color(0xFF4A3320),
        tertiary = Color(0xFFA6432D),
        onTertiary = Color.White,
        background = Color(0xFFFFF9F3),
        onBackground = Color(0xFF4A3320),
        surface = Color(0xFFFFFBF8),
        onSurface = Color(0xFF6B4E36),
        surfaceVariant = Color(0xFFF2E4D5),
        onSurfaceVariant = Color(0xFF8A7364),
        outline = Color(0xFFE8D8C8),
        outlineVariant = Color(0xFFE8D8C8),
        error = Color(0xFFA6432D),
        onError = Color.White,
        surfaceContainerLowest = Color(0xFFFFF9F3),
        surfaceContainerLow = Color(0xFFFFFBF8),
        surfaceContainer = Color(0xFFFFFBF8),
        surfaceContainerHigh = Color(0xFFF2E4D5),
        surfaceContainerHighest = Color(0xFFF2E4D5)
    ),
    header = HeaderColors(container = Color(0xFF8B5A2B), content = Color.White)
)

private fun AppTheme.colors(): AppThemeColors = when (this) {
    AppTheme.SUNSET_GOLD -> SunsetGoldColors
    AppTheme.GOLDEN_HOUR -> GoldenHourColors
    AppTheme.TERRA_COTTA -> TerraCottaColors
}

/** The scheme's primary/CTA color, for rendering a preview swatch in a theme picker. */
fun AppTheme.swatchColor(): Color = colors().colorScheme.primary

@Composable
fun GratitudeLoggerTheme(appTheme: AppTheme, content: @Composable () -> Unit) {
    val themeColors = appTheme.colors()
    CompositionLocalProvider(LocalHeaderColors provides themeColors.header) {
        MaterialTheme(
            colorScheme = themeColors.colorScheme,
            typography = Typography,
            content = content
        )
    }
}
