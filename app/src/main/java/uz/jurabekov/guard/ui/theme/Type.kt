package uz.jurabekov.guard.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Typography scale — sanoat muhitida o'qish oson bo'lishi uchun
 * font'lar yetarlicha katta va bold.
 */
val AppTypography = Typography(
    displayLarge = TextStyle(
        fontSize = 36.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 44.sp
    ),
    displayMedium = TextStyle(
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 36.sp
    ),
    headlineLarge = TextStyle(
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 32.sp
    ),
    headlineMedium = TextStyle(
        fontSize = 22.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 30.sp
    ),
    titleLarge = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 24.sp
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontSize = 17.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 24.sp
    ),
    labelMedium = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 16.sp
    )
)
