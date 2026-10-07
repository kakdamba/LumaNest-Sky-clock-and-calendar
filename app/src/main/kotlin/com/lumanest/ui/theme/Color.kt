package com.lumanest.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

// Cozy Night Palette for LumaNest
val DeepMidnight = Color(0xFF0F141C)
val NightSurface = Color(0xFF181F2A)
val NightSurfaceVariant = Color(0xFF222B3A)
val CozyWarmAmber = Color(0xFFFFB74D)
val StarlightBlue = Color(0xFF90CAF9)
val SoftCloud = Color(0xFFE2E8F0)
val MutedSlate = Color(0xFF94A3B8)
val DogFluffWhite = Color(0xFFF8FAFC)
val CardBorder = Color(0xFF2D3748)

val LumaNestColorScheme = darkColorScheme(
    primary = CozyWarmAmber,
    onPrimary = DeepMidnight,
    primaryContainer = NightSurfaceVariant,
    onPrimaryContainer = DogFluffWhite,
    secondary = StarlightBlue,
    onSecondary = DeepMidnight,
    background = DeepMidnight,
    onBackground = DogFluffWhite,
    surface = NightSurface,
    onSurface = DogFluffWhite,
    surfaceVariant = NightSurfaceVariant,
    onSurfaceVariant = MutedSlate,
    outline = CardBorder
)
