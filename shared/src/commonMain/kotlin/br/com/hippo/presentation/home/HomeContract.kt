package br.com.hippo.presentation.home

import br.com.hippo.domain.model.Task
import br.com.hippo.domain.usecase.TaskGroup

data class HomeState(
    /** Ainda não há nada para mostrar: lendo o aparelho ou, na primeira vez, buscando na API. */
    val isLoading: Boolean = true,
    val greeting: String = "",
    val groups: List<TaskGroup> = emptyList(),
)

sealed interface HomeIntent {
    /** A tela apareceu: busca as tarefas mais recentes. */
    data object Refresh : HomeIntent
    data class ToggleDone(val task: Task) : HomeIntent
    data class OpenTask(val task: Task) : HomeIntent
}

sealed interface HomeEffect {
    data class NavigateToDetails(val taskId: String) : HomeEffect
    data class ShowMessage(val message: String) : HomeEffect
}
