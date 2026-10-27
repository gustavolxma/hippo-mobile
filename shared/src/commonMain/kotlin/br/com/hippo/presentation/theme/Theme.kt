package br.com.hippo.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import br.com.hippo.domain.model.TaskPriority

@Composable
fun HippoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme(),
        content = content,
    )
}

val TaskPriority.label: String
    get() = when (this) {
        TaskPriority.LOW -> "Baixa"
        TaskPriority.MEDIUM -> "Média"
        TaskPriority.HIGH -> "Alta"
        TaskPriority.URGENT -> "Urgente"
    }
