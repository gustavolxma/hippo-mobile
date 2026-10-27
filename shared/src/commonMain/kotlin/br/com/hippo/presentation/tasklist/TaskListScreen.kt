package br.com.hippo.presentation.tasklist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.hippo.domain.model.Task
import br.com.hippo.presentation.theme.label
import org.koin.compose.viewmodel.koinViewModel

/** "Rota" da tela: conecta o ViewModel e trata os efeitos. */
@Composable
fun TaskListRoute(
    onNavigateToTaskForm: () -> Unit,
    viewModel: TaskListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                TaskListEffect.NavigateToTaskForm -> onNavigateToTaskForm()
                is TaskListEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    TaskListScreen(state, snackbarHostState, viewModel::onIntent)
}

/** Tela "burra": só desenha o estado e emite intenções. Fácil de testar e pré-visualizar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    state: TaskListState,
    snackbarHostState: SnackbarHostState,
    onIntent: (TaskListIntent) -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Minhas tarefas") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { onIntent(TaskListIntent.AddTaskClicked) }) {
                Text("Nova tarefa")
            }
        },
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                CircularProgressIndicator()
            }

            state.tasks.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Text("Nenhuma tarefa por aqui.")
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.tasks, key = { it.id }) { task ->
                    TaskItem(
                        task = task,
                        onToggle = { onIntent(TaskListIntent.ToggleDone(task)) },
                        onDelete = { onIntent(TaskListIntent.Delete(task)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskItem(task: Task, onToggle: () -> Unit, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = task.isDone, onCheckedChange = { onToggle() })
            Column(Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
                )
                Text(
                    text = buildString {
                        append(task.priority.label)
                        task.dueAt?.let { append(" • ${it.date} ${it.time}") }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onDelete) { Text("Excluir") }
        }
    }
}
