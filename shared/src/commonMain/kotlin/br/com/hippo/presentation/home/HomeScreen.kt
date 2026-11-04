package br.com.hippo.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.hippo.domain.usecase.TaskSection
import br.com.hippo.presentation.components.SectionTitle
import br.com.hippo.presentation.components.TaskCard
import br.com.hippo.presentation.components.title
import br.com.hippo.presentation.theme.HippoColors
import br.com.hippo.presentation.theme.HippoTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeRoute(
    onOpenTask: (taskId: String) -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is HomeEffect.NavigateToDetails -> onOpenTask(effect.taskId)
                is HomeEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        HomeScreen(state, viewModel::onIntent)
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
fun HomeScreen(state: HomeState, onIntent: (HomeIntent) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "header") {
            Column(Modifier.padding(bottom = 6.dp)) {
                Text(state.greeting, style = HippoTheme.type.caption.copy(fontSize = 14.sp), color = HippoColors.OnMuted)
                Text("Suas tarefas", style = HippoTheme.type.title, modifier = Modifier.semantics { heading() })
            }
        }

        when {
            state.isLoading -> item(key = "loading") {
                Box(Modifier.fillMaxSize().padding(top = 48.dp), Alignment.Center) {
                    CircularProgressIndicator(color = HippoColors.Accent)
                }
            }

            state.groups.isEmpty() -> item(key = "empty") {
                Text(
                    text = "Nenhuma tarefa por aqui.\nToque em Voz para criar a primeira.",
                    style = HippoTheme.type.body,
                    color = HippoColors.OnMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxSize().padding(top = 48.dp),
                )
            }

            else -> state.groups.forEach { group ->
                item(key = "section-${group.section}") {
                    SectionTitle(group.section.title, Modifier.padding(top = 8.dp))
                }
                items(group.tasks, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        showDate = group.section != TaskSection.TODAY,
                        onToggleDone = { onIntent(HomeIntent.ToggleDone(task)) },
                        onClick = { onIntent(HomeIntent.OpenTask(task)) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}
