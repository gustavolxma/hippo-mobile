package br.com.hippo.presentation.taskdetails

import androidx.lifecycle.viewModelScope
import br.com.hippo.core.mvi.MviViewModel
import br.com.hippo.domain.model.Task
import br.com.hippo.domain.usecase.DeleteTaskUseCase
import br.com.hippo.domain.usecase.GetTaskUseCase
import br.com.hippo.domain.usecase.SaveTaskUseCase
import br.com.hippo.domain.usecase.TaskInput
import br.com.hippo.domain.usecase.TaskValidationException
import kotlinx.coroutines.launch

/**
 * Tela de criar/editar tarefa. Com [taskId] nulo, cria uma nova (pré-preenchida com
 * [draft], quando vem da tela de Voz); caso contrário, carrega a tarefa para edição.
 */
class TaskDetailsViewModel(
    private val taskId: String?,
    draft: TaskInput?,
    private val getTask: GetTaskUseCase,
    private val saveTask: SaveTaskUseCase,
    private val deleteTask: DeleteTaskUseCase,
) : MviViewModel<TaskDetailsState, TaskDetailsIntent, TaskDetailsEffect>(
    TaskDetailsState(isNew = taskId == null, isLoading = taskId != null).withDraft(draft),
) {

    private var original: Task? = null

    init {
        if (taskId != null) load(taskId)
    }

    private fun load(id: String) = viewModelScope.launch {
        val task = runCatching { getTask(id) }.getOrNull()
        if (task == null) {
            sendEffect(TaskDetailsEffect.ShowMessage("Tarefa não encontrada"))
            sendEffect(TaskDetailsEffect.NavigateBack)
            return@launch
        }
        original = task
        setState {
            copy(
                isLoading = false,
                title = task.title,
                description = task.description,
                priority = task.priority,
                dueDate = task.dueDate,
                dueTime = task.dueTime,
                reminder = task.reminder,
            )
        }
    }

    override fun onIntent(intent: TaskDetailsIntent) {
        when (intent) {
            is TaskDetailsIntent.TitleChanged -> setState { copy(title = intent.value, titleError = null) }
            is TaskDetailsIntent.DescriptionChanged -> setState { copy(description = intent.value) }
            is TaskDetailsIntent.PriorityChanged -> setState { copy(priority = intent.value) }
            is TaskDetailsIntent.DateChanged -> setState { copy(dueDate = intent.value, dateError = null) }
            is TaskDetailsIntent.TimeChanged -> setState { copy(dueTime = intent.value, dateError = null) }
            is TaskDetailsIntent.ReminderChanged -> setState { copy(reminder = intent.value) }
            TaskDetailsIntent.Save -> save()
            TaskDetailsIntent.DeleteClicked -> setState { copy(showDeleteDialog = true) }
            TaskDetailsIntent.DeleteDismissed -> setState { copy(showDeleteDialog = false) }
            TaskDetailsIntent.DeleteConfirmed -> delete()
            TaskDetailsIntent.Back -> sendEffect(TaskDetailsEffect.NavigateBack)
        }
    }

    private fun save() {
        if (currentState.isSaving) return
        setState { copy(isSaving = true) }
        val input = with(currentState) {
            TaskInput(title, description, priority, dueDate, dueTime, reminder)
        }
        viewModelScope.launch {
            saveTask(input, existing = original)
                .onSuccess { sendEffect(TaskDetailsEffect.NavigateBack) }
                .onFailure { error ->
                    setState { copy(isSaving = false) }
                    when {
                        error !is TaskValidationException ->
                            sendEffect(TaskDetailsEffect.ShowMessage("Não foi possível salvar a tarefa"))
                        currentState.title.isBlank() -> setState { copy(titleError = error.message) }
                        else -> setState { copy(dateError = error.message) }
                    }
                }
        }
    }

    private fun delete() {
        val id = original?.id ?: return
        setState { copy(showDeleteDialog = false) }
        viewModelScope.launch {
            deleteTask(id)
                .onSuccess { sendEffect(TaskDetailsEffect.NavigateBack) }
                .onFailure { sendEffect(TaskDetailsEffect.ShowMessage("Não foi possível excluir a tarefa")) }
        }
    }
}

private fun TaskDetailsState.withDraft(draft: TaskInput?): TaskDetailsState =
    if (draft == null) this else copy(
        title = draft.title,
        description = draft.description,
        priority = draft.priority,
        dueDate = draft.dueDate,
        dueTime = draft.dueTime,
        reminder = draft.reminder,
    )
