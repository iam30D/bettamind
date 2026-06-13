package org.bettamind.shared.design

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val PhaseOneColorScheme = lightColorScheme()

@Composable
fun BettamindTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PhaseOneColorScheme,
        content = content,
    )
}
