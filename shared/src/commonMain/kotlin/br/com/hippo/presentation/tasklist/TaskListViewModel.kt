package br.com.hippo.presentation.tasklist

import androidx.lifecycle.viewModelScope
import br.com.hippo.core.mvi.MviViewModel
import br.com.hippo.domain.usecase.DeleteTaskUseCase
import br.com.hippo.domain.usecase.ObserveTasksUseCase
import br.com.hippo.domain.usecase.ToggleTaskDoneUseCase
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class TaskListViewModel(
    observeTasks: ObserveTasksUseCase,
    private val toggleTaskDone: ToggleTaskDoneUseCase,
    private val deleteTask: DeleteTaskUseCase,
) : MviViewModel<TaskListState, TaskListIntent, TaskListEffect>(TaskListState()) {

    init {
        observeTasks()
            .onEach { tasks -> setState { copy(isLoading = false, tasks = tasks) } }
            .catch {
                setState { copy(isLoading = false) }
                sendEffect(TaskListEffect.ShowMessage("Não foi possível carregar as tarefas"))
            }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: TaskListIntent) {
        when (intent) {
            is TaskListIntent.ToggleDone -> viewModelScope.launch {
                toggleTaskDone(intent.task).onFailure {
                    sendEffect(TaskListEffect.ShowMessage("Não foi possível atualizar a tarefa"))
                }
            }

            is TaskListIntent.Delete -> viewModelScope.launch {
                deleteTask(intent.task.id).onFailure {
                    sendEffect(TaskListEffect.ShowMessage("Não foi possível excluir a tarefa"))
                }
            }

            TaskListIntent.AddTaskClicked -> sendEffect(TaskListEffect.NavigateToTaskForm)
        }
    }
}
