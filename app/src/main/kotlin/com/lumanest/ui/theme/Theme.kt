package com.lumanest.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun LumaNestTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LumaNestColorScheme,
        content = content
    )
}
