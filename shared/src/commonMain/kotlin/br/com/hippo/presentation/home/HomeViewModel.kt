package br.com.hippo.presentation.home

import androidx.lifecycle.viewModelScope
import br.com.hippo.core.mvi.MviViewModel
import br.com.hippo.domain.usecase.ObserveTasksUseCase
import br.com.hippo.domain.usecase.ToggleTaskDoneUseCase
import br.com.hippo.domain.usecase.groupTasksByDay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

class HomeViewModel(
    observeTasks: ObserveTasksUseCase,
    private val toggleTaskDone: ToggleTaskDoneUseCase,
    private val clock: Clock,
) : MviViewModel<HomeState, HomeIntent, HomeEffect>(HomeState()) {

    init {
        observeTasks()
            .onEach { tasks ->
                val now = clock.now().toLocalDateTime(TimeZone.currentSystemDefault())
                setState {
                    copy(
                        isLoading = false,
                        greeting = greetingFor(now.hour),
                        groups = groupTasksByDay(tasks, today = now.date),
                    )
                }
            }
            .catch {
                setState { copy(isLoading = false) }
                sendEffect(HomeEffect.ShowMessage("Não foi possível carregar as tarefas"))
            }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.ToggleDone -> viewModelScope.launch {
                toggleTaskDone(intent.task).onFailure {
                    sendEffect(HomeEffect.ShowMessage("Não foi possível atualizar a tarefa"))
                }
            }

            is HomeIntent.OpenTask -> sendEffect(HomeEffect.NavigateToDetails(intent.task.id))
        }
    }
}

internal fun greetingFor(hour: Int): String = when (hour) {
    in 5..11 -> "Bom dia!"
    in 12..17 -> "Boa tarde!"
    else -> "Boa noite!"
}
