package com.learningcalendar.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val WarmBackground = Color(0xFFFBF8F2)
val SurfaceWarm = Color(0xFFFFFDF8)
val DeepBlue = Color(0xFF183247)
val MutedBlue = Color(0xFF53646C)
val Sage = Color(0xFF54785C)
val SageDark = Color(0xFF36553D)
val SageSoft = Color(0xFFDDE9DD)
val Amber = Color(0xFFB4772E)
val AmberSoft = Color(0xFFF4E8D6)
val CardOutline = Color(0xFFE4E1D9)
val ScheduleSurface = Color(0xFFF2F5EF)

private val LearningCalendarColors = lightColorScheme(
    primary = Sage,
    onPrimary = Color.White,
    primaryContainer = SageSoft,
    onPrimaryContainer = DeepBlue,
    secondary = Amber,
    onSecondary = Color.White,
    secondaryContainer = AmberSoft,
    onSecondaryContainer = DeepBlue,
    background = WarmBackground,
    onBackground = DeepBlue,
    surface = SurfaceWarm,
    onSurface = DeepBlue,
    surfaceVariant = ScheduleSurface,
    onSurfaceVariant = MutedBlue,
    outline = CardOutline,
)

private val LearningCalendarTypography = Typography(
    displaySmall = TextStyle(
        fontSize = 30.sp,
        lineHeight = 38.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    titleLarge = TextStyle(
        fontSize = 21.sp,
        lineHeight = 29.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    titleMedium = TextStyle(
        fontSize = 17.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        lineHeight = 25.sp,
        fontWeight = FontWeight.Normal,
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        lineHeight = 21.sp,
        fontWeight = FontWeight.Normal,
    ),
    labelLarge = TextStyle(
        fontSize = 15.sp,
        lineHeight = 21.sp,
        fontWeight = FontWeight.Medium,
    ),
    labelMedium = TextStyle(
        fontSize = 12.sp,
        lineHeight = 17.sp,
        fontWeight = FontWeight.Medium,
    ),
)

@Composable
fun LearningCalendarTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LearningCalendarColors,
        typography = LearningCalendarTypography,
        content = content,
    )
}
