package com.napcity.n8nmobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val N8nPink = Color(0xFFEA4B71)
private val N8nDark = Color(0xFF1A1A2E)

private val LightColors = lightColorScheme(
    primary = N8nPink,
    onPrimary = Color.White,
    secondary = N8nPink,
)

private val DarkColors = darkColorScheme(
    primary = N8nPink,
    onPrimary = Color.White,
    secondary = N8nPink,
    background = N8nDark,
    surface = Color(0xFF16213E),
)

@Composable
fun ThemeN8NMobile(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
