package br.com.hippo.presentation.taskdetails

import br.com.hippo.domain.model.TaskPriority
import br.com.hippo.domain.model.TaskReminder
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

data class TaskDetailsState(
    /** `true` ao criar uma tarefa nova; `false` ao editar uma existente. */
    val isNew: Boolean = true,
    val isLoading: Boolean = false,
    val title: String = "",
    val description: String = "",
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val dueDate: LocalDate? = null,
    val dueTime: LocalTime? = null,
    val reminder: TaskReminder = TaskReminder.NONE,
    val isSaving: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val titleError: String? = null,
    val dateError: String? = null,
)

sealed interface TaskDetailsIntent {
    data class TitleChanged(val value: String) : TaskDetailsIntent
    data class DescriptionChanged(val value: String) : TaskDetailsIntent
    data class PriorityChanged(val value: TaskPriority) : TaskDetailsIntent
    data class DateChanged(val value: LocalDate?) : TaskDetailsIntent
    data class TimeChanged(val value: LocalTime?) : TaskDetailsIntent
    data class ReminderChanged(val value: TaskReminder) : TaskDetailsIntent
    data object Save : TaskDetailsIntent
    data object DeleteClicked : TaskDetailsIntent
    data object DeleteConfirmed : TaskDetailsIntent
    data object DeleteDismissed : TaskDetailsIntent
    data object Back : TaskDetailsIntent
}

sealed interface TaskDetailsEffect {
    data object NavigateBack : TaskDetailsEffect
    data class ShowMessage(val message: String) : TaskDetailsEffect
}
