package br.com.hippo.presentation.home

import br.com.hippo.domain.model.Task
import br.com.hippo.domain.usecase.TaskGroup

data class HomeState(
    val isLoading: Boolean = true,
    val greeting: String = "",
    val groups: List<TaskGroup> = emptyList(),
)

sealed interface HomeIntent {
    data class ToggleDone(val task: Task) : HomeIntent
    data class OpenTask(val task: Task) : HomeIntent
}

sealed interface HomeEffect {
    data class NavigateToDetails(val taskId: String) : HomeEffect
    data class ShowMessage(val message: String) : HomeEffect
}
