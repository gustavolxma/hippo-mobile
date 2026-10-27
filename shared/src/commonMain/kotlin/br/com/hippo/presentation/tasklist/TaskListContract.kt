package br.com.hippo.presentation.tasklist

import br.com.hippo.domain.model.Task

data class TaskListState(
    val isLoading: Boolean = true,
    val tasks: List<Task> = emptyList(),
)

sealed interface TaskListIntent {
    data class ToggleDone(val task: Task) : TaskListIntent
    data class Delete(val task: Task) : TaskListIntent
    data object AddTaskClicked : TaskListIntent
}

sealed interface TaskListEffect {
    data object NavigateToTaskForm : TaskListEffect
    data class ShowMessage(val message: String) : TaskListEffect
}
