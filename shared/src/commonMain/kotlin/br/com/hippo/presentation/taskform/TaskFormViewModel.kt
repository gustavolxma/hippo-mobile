package br.com.hippo.presentation.taskform

import androidx.lifecycle.viewModelScope
import br.com.hippo.core.mvi.MviViewModel
import br.com.hippo.domain.usecase.AddTaskUseCase
import kotlinx.coroutines.launch

class TaskFormViewModel(
    private val addTask: AddTaskUseCase,
) : MviViewModel<TaskFormState, TaskFormIntent, TaskFormEffect>(TaskFormState()) {

    override fun onIntent(intent: TaskFormIntent) {
        when (intent) {
            is TaskFormIntent.TitleChanged -> setState { copy(title = intent.value, titleError = null) }
            is TaskFormIntent.DescriptionChanged -> setState { copy(description = intent.value) }
            is TaskFormIntent.PriorityChanged -> setState { copy(priority = intent.value) }
            TaskFormIntent.Back -> sendEffect(TaskFormEffect.NavigateBack)
            TaskFormIntent.Save -> save()
        }
    }

    private fun save() {
        if (currentState.isSaving) return
        setState { copy(isSaving = true) }
        viewModelScope.launch {
            addTask(
                title = currentState.title,
                description = currentState.description,
                priority = currentState.priority,
            ).onSuccess {
                sendEffect(TaskFormEffect.NavigateBack)
            }.onFailure { error ->
                setState { copy(isSaving = false, titleError = error.message) }
            }
        }
    }
}
