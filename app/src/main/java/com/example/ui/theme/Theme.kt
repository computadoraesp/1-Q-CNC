package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val IndustrialDarkColorScheme = darkColorScheme(
  primary = IndDarkPrimary,
  onPrimary = Color.Black,
  primaryContainer = IndDarkSurfaceVariant,
  onPrimaryContainer = IndDarkPrimary,
  secondary = IndDarkSecondary,
  onSecondary = Color.Black,
  secondaryContainer = IndDarkSurfaceVariant,
  onSecondaryContainer = IndDarkSecondary,
  tertiary = IndDarkTertiary,
  onTertiary = Color.Black,
  background = IndDarkBg,
  onBackground = IndDarkOnBg,
  surface = IndDarkSurface,
  onSurface = IndDarkOnSurface,
  surfaceVariant = IndDarkSurfaceVariant,
  onSurfaceVariant = IndDarkTextMuted,
  outline = IndDarkBorder,
  error = IndDarkEmergency,
  onError = Color.White
)

private val IndustrialLightColorScheme = lightColorScheme(
  primary = IndLightPrimary,
  onPrimary = Color.White,
  primaryContainer = IndLightSurfaceVariant,
  onPrimaryContainer = IndLightPrimary,
  secondary = IndLightSecondary,
  onSecondary = Color.White,
  secondaryContainer = IndLightSurfaceVariant,
  onSecondaryContainer = IndLightSecondary,
  tertiary = IndLightTertiary,
  onTertiary = Color.White,
  background = IndLightBg,
  onBackground = IndLightOnBg,
  surface = IndLightSurface,
  onSurface = IndLightOnSurface,
  surfaceVariant = IndLightSurfaceVariant,
  onSurfaceVariant = IndLightTextMuted,
  outline = IndLightBorder,
  error = IndLightEmergency,
  onError = Color.White
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Default to high-contrast industrial dark cockpit
  content: @Composable () -> Unit
) {
  val colorScheme = if (darkTheme) IndustrialDarkColorScheme else IndustrialLightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
