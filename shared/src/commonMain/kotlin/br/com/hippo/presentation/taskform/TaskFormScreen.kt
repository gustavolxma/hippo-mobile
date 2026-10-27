package br.com.hippo.presentation.taskform

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.hippo.domain.model.TaskPriority
import br.com.hippo.presentation.theme.label
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TaskFormRoute(
    onNavigateBack: () -> Unit,
    viewModel: TaskFormViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                TaskFormEffect.NavigateBack -> onNavigateBack()
            }
        }
    }

    TaskFormScreen(state, viewModel::onIntent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskFormScreen(
    state: TaskFormState,
    onIntent: (TaskFormIntent) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nova tarefa") },
                navigationIcon = {
                    TextButton(onClick = { onIntent(TaskFormIntent.Back) }) { Text("Voltar") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = state.title,
                onValueChange = { onIntent(TaskFormIntent.TitleChanged(it)) },
                label = { Text("Título") },
                isError = state.titleError != null,
                supportingText = state.titleError?.let { { Text(it) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.description,
                onValueChange = { onIntent(TaskFormIntent.DescriptionChanged(it)) },
                label = { Text("Descrição") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Prioridade")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TaskPriority.entries.forEach { priority ->
                    FilterChip(
                        selected = state.priority == priority,
                        onClick = { onIntent(TaskFormIntent.PriorityChanged(priority)) },
                        label = { Text(priority.label) },
                    )
                }
            }
            Button(
                onClick = { onIntent(TaskFormIntent.Save) },
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Salvar")
            }
        }
    }
}
