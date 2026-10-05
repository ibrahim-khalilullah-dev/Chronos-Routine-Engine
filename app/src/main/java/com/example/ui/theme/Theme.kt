package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val OneUiColorScheme = darkColorScheme(
  primary = AnchorGradientStart,
  onPrimary = Color.White,
  primaryContainer = AnchorGradientEnd,
  onPrimaryContainer = Color.White,
  secondary = DynamicGradientEnd,
  onSecondary = Color.Black,
  secondaryContainer = DynamicGradientStart,
  onSecondaryContainer = Color.White,
  tertiary = ElasticGradientStart,
  onTertiary = Color.Black,
  background = OneUiBackground,
  onBackground = TextPrimary,
  surface = OneUiSurface,
  onSurface = TextPrimary,
  surfaceVariant = OneUiSurfaceElevated,
  onSurfaceVariant = TextSecondary,
  outline = OneUiBorder
)

val OneUiShapes = Shapes(
  small = RoundedCornerShape(12.dp),
  medium = RoundedCornerShape(20.dp),
  large = RoundedCornerShape(26.dp),
  extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun MyApplicationTheme(
  content: @Composable () -> Unit
) {
  MaterialTheme(
    colorScheme = OneUiColorScheme,
    typography = Typography,
    shapes = OneUiShapes,
    content = content
  )
}
