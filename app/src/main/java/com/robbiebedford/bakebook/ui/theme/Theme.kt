package com.robbiebedford.bakebook.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Cream = Color(0xFFFFF7EA)
val Brown = Color(0xFF7A4A28)
val Orange = Color(0xFFD9822B)
val SoftCard = Color(0xFFFFFCF6)

private val BakeBookColors = lightColorScheme(
    primary = Brown,
    onPrimary = Color.White,
    secondary = Orange,
    onSecondary = Color.White,
    background = Cream,
    onBackground = Color(0xFF2A1A10),
    surface = SoftCard,
    onSurface = Color(0xFF2A1A10),
    tertiary = Color(0xFF99621E)
)

@Composable
fun BakeBookTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BakeBookColors,
        typography = MaterialTheme.typography,
        content = content
    )
}
