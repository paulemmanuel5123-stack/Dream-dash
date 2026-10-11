package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DreamDashColorScheme = darkColorScheme(
    primary = AstralGold,
    onPrimary = MysticPurpleDark,
    primaryContainer = MysticPurpleLight,
    onPrimaryContainer = AstralGoldBright,
    secondary = StarlightCyan,
    onSecondary = MysticPurpleDark,
    secondaryContainer = MysticPurpleMid,
    onSecondaryContainer = StarlightCyanBright,
    tertiary = DreamEmerald,
    onTertiary = MysticPurpleDark,
    background = FantasyDarkBg,
    onBackground = TextPrimary,
    surface = FantasySurface,
    onSurface = TextPrimary,
    surfaceVariant = FantasySurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = FantasyCardBorder
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DreamDashColorScheme,
        typography = Typography,
        content = content
    )
}
