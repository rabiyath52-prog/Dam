package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val EkativaDarkColorScheme = darkColorScheme(
    primary = GoldPrimary,
    onPrimary = OnGoldPrimary,
    primaryContainer = GoldContainer,
    onPrimaryContainer = OnGoldContainer,
    secondary = SilverMetallic,
    onSecondary = ObsidianBackground,
    secondaryContainer = ObsidianSurfaceElevated,
    onSecondaryContainer = TextPrimary,
    tertiary = EmeraldTerminal,
    onTertiary = TerminalBlack,
    tertiaryContainer = EmeraldMuted,
    onTertiaryContainer = EmeraldTerminal,
    background = ObsidianBackground,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = ObsidianOutline,
    outlineVariant = ObsidianOutlineSubtle,
    error = ErrorCrimson,
    onError = ObsidianBackground,
    errorContainer = ErrorContainerDark,
    onErrorContainer = ErrorCrimson
)

private val EkativaShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp)
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = EkativaDarkColorScheme,
        typography = Typography,
        shapes = EkativaShapes,
        content = content
    )
}
