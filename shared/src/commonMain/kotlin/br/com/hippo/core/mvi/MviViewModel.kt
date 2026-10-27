package br.com.hippo.core.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Base do padrão MVI (Model-View-Intent):
 * - [S] State: tudo o que a tela precisa para se desenhar (fonte única da verdade);
 * - [I] Intent: ações do usuário enviadas pela View via [onIntent];
 * - [E] Effect: eventos de uma única vez (navegar, mostrar mensagem).
 *
 * O fluxo é unidirecional: View -> Intent -> ViewModel -> novo State -> View.
 */
abstract class MviViewModel<S, I, E>(initialState: S) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    private val _effects = Channel<E>(Channel.BUFFERED)
    val effects: Flow<E> = _effects.receiveAsFlow()

    protected val currentState: S get() = _state.value

    abstract fun onIntent(intent: I)

    protected fun setState(reducer: S.() -> S) {
        _state.update(reducer)
    }

    protected fun sendEffect(effect: E) {
        viewModelScope.launch { _effects.send(effect) }
    }
}
