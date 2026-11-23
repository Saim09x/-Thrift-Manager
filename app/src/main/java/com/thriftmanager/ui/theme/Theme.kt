package com.thriftmanager.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Palette = lightColorScheme(
    primary = Color(0xFF48535D),
    onPrimary = Color.White,
    secondary = Color(0xFF74818C),
    background = Color(0xFFF7F8F9),
    surface = Color.White,
    onSurface = Color(0xFF232A30),
    onBackground = Color(0xFF232A30),
    error = Color(0xFFB3261E)
)

@Composable
fun ThriftManagerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Palette, content = content)
}
