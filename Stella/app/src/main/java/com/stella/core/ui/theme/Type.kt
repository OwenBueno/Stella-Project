package com.stella.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private fun stellaTextStyle(
    fontSize: androidx.compose.ui.unit.TextUnit,
    lineHeight: androidx.compose.ui.unit.TextUnit,
    fontWeight: FontWeight = FontWeight.Normal,
    letterSpacing: androidx.compose.ui.unit.TextUnit = 0.sp,
    fontFamily: FontFamily = FontFamily.Default,
) = TextStyle(
    fontFamily = fontFamily,
    fontWeight = fontWeight,
    fontSize = fontSize,
    lineHeight = lineHeight,
    letterSpacing = letterSpacing,
    color = TextPrimary,
)

val StellaTypography = Typography(
    displayLarge = stellaTextStyle(57.sp, 64.sp),
    displayMedium = stellaTextStyle(45.sp, 52.sp),
    displaySmall = stellaTextStyle(36.sp, 44.sp),
    headlineLarge = stellaTextStyle(28.sp, 34.sp, FontWeight.Bold, (-0.5).sp),
    headlineMedium = stellaTextStyle(24.sp, 30.sp, FontWeight.Bold),
    headlineSmall = stellaTextStyle(20.sp, 26.sp, FontWeight.SemiBold),
    titleLarge = stellaTextStyle(22.sp, 28.sp, FontWeight.Bold),
    titleMedium = stellaTextStyle(18.sp, 24.sp, FontWeight.SemiBold),
    titleSmall = stellaTextStyle(16.sp, 22.sp, FontWeight.Medium),
    bodyLarge = stellaTextStyle(16.sp, 24.sp, letterSpacing = 0.25.sp),
    bodyMedium = stellaTextStyle(14.sp, 20.sp, letterSpacing = 0.15.sp),
    bodySmall = stellaTextStyle(12.sp, 16.sp),
    labelLarge = stellaTextStyle(14.sp, 20.sp, FontWeight.Medium),
    labelMedium = stellaTextStyle(12.sp, 16.sp, FontWeight.Medium),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.5.sp,
        color = TextPrimary,
    ),
)
