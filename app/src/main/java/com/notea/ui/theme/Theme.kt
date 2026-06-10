package com.notea.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NoteaColors = lightColorScheme(
    primary = Color(0xFF355070),
    onPrimary = Color.White,
    secondary = Color(0xFF6D597A),
    tertiary = Color(0xFFB56576),
    background = Color(0xFFF7F8FA),
    surface = Color.White,
    surfaceVariant = Color(0xFFE9EEF5)
)

@Composable
fun NoteaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NoteaColors,
        typography = Typography(),
        content = content
    )
}
