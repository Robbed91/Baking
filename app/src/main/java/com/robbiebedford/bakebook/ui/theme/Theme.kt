package com.robbiebedford.bakebook.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Cream = Color(0xFFFFF8F2)
val Brown = Color(0xFF8A2F56)
val Orange = Color(0xFFC95F72)
val SoftCard = Color(0xFFFFFFFF)
val Vanilla = Color(0xFFFFFBF7)
val Sage = Color(0xFF6F8F7A)
val Charcoal = Color(0xFF261D22)
val MutedText = Color(0xFF6F6268)
val SoftStroke = Color(0xFFE9DDE3)
val BerryContainer = Color(0xFFFFD8E6)
val SageContainer = Color(0xFFDDEDE1)

private val BakeBookColors = lightColorScheme(
    primary = Brown,
    onPrimary = Color.White,
    primaryContainer = BerryContainer,
    onPrimaryContainer = Color(0xFF3E1026),
    secondary = Sage,
    onSecondary = Color.White,
    secondaryContainer = SageContainer,
    onSecondaryContainer = Color(0xFF163323),
    background = Cream,
    onBackground = Charcoal,
    surface = SoftCard,
    onSurface = Charcoal,
    surfaceVariant = Vanilla,
    onSurfaceVariant = MutedText,
    outline = SoftStroke,
    tertiary = Orange,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE0D8),
    onTertiaryContainer = Color(0xFF442019),
    error = Color(0xFFB3261E),
    onError = Color.White
)

private val BakeBookTypography = Typography(
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        color = Charcoal
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        color = Charcoal
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        color = Charcoal
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        color = Charcoal
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = Charcoal
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )
)

private val BakeBookShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
)

@Composable
fun BakeBookTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BakeBookColors,
        typography = BakeBookTypography,
        shapes = BakeBookShapes,
        content = content
    )
}
