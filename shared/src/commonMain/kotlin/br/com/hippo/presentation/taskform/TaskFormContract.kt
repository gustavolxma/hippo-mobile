package br.com.hippo.presentation.taskform

import br.com.hippo.domain.model.TaskPriority

data class TaskFormState(
    val title: String = "",
    val description: String = "",
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val isSaving: Boolean = false,
    val titleError: String? = null,
)

sealed interface TaskFormIntent {
    data class TitleChanged(val value: String) : TaskFormIntent
    data class DescriptionChanged(val value: String) : TaskFormIntent
    data class PriorityChanged(val value: TaskPriority) : TaskFormIntent
    data object Save : TaskFormIntent
    data object Back : TaskFormIntent
}

sealed interface TaskFormEffect {
    data object NavigateBack : TaskFormEffect
}
