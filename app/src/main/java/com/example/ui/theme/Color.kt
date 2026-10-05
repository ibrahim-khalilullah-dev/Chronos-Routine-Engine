package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// One UI Deep AMOLED Dark Palette
val OneUiBackground = Color(0xFF0B0E14)
val OneUiSurface = Color(0xFF131822)
val OneUiSurfaceElevated = Color(0xFF1C2230)
val OneUiSurfaceHigh = Color(0xFF252C3D)
val OneUiBorder = Color(0x1AFFFFFF) // 10% white border

// Accent Gradients
val SleepGradientStart = Color(0xFF1E1B4B)
val SleepGradientEnd = Color(0xFF312E81)
val SleepBrush = Brush.linearGradient(listOf(SleepGradientStart, SleepGradientEnd))

val AnchorGradientStart = Color(0xFF4F46E5)
val AnchorGradientEnd = Color(0xFF7C3AED)
val AnchorBrush = Brush.linearGradient(listOf(AnchorGradientStart, AnchorGradientEnd))

val ElasticGradientStart = Color(0xFFF59E0B)
val ElasticGradientEnd = Color(0xFFEF4444)
val ElasticBrush = Brush.linearGradient(listOf(ElasticGradientStart, ElasticGradientEnd))

val DynamicGradientStart = Color(0xFF06B6D4)
val DynamicGradientEnd = Color(0xFF10B981)
val DynamicBrush = Brush.linearGradient(listOf(DynamicGradientStart, DynamicGradientEnd))

val OverflowGradientStart = Color(0xFF334155)
val OverflowGradientEnd = Color(0xFF475569)
val OverflowBrush = Brush.linearGradient(listOf(OverflowGradientStart, OverflowGradientEnd))

fun gradientBrushWithAlpha(start: Color, end: Color, alpha: Float): Brush =
  Brush.linearGradient(listOf(start.copy(alpha = alpha), end.copy(alpha = alpha)))

val DynamicSubtleBrush = gradientBrushWithAlpha(DynamicGradientStart, DynamicGradientEnd, 0.14f)
val OverflowSubtleBrush = gradientBrushWithAlpha(OverflowGradientStart, OverflowGradientEnd, 0.35f)

// Semantic Colors
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val TextTertiary = Color(0xFF64748B)

val SuccessGreen = Color(0xFF10B981)
val WarningAmber = Color(0xFFF59E0B)
val DangerRed = Color(0xFFEF4444)
val PurpleAccent = Color(0xFFA855F7)
val CyanAccent = Color(0xFF06B6D4)
val BlueAccent = Color(0xFF3B82F6)
