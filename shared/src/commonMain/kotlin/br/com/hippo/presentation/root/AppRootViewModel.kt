package br.com.hippo.presentation.root

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.hippo.domain.model.ThemeMode
import br.com.hippo.domain.usecase.ObserveCurrentUserUseCase
import br.com.hippo.domain.usecase.ObserveThemeModeUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface SessionState {
    /** Ainda lendo a sessão salva no DataStore. */
    data object Loading : SessionState
    data object SignedOut : SessionState
    data object SignedIn : SessionState
}

/**
 * Decide qual parte do app mostrar a partir da sessão salva. Entrar, verificar o código
 * ou sair da conta só mudam a sessão; a troca de tela acontece aqui, em um único lugar.
 * Também expõe a aparência escolhida, aplicada no tema de todo o app.
 */
class AppRootViewModel(
    observeCurrentUser: ObserveCurrentUserUseCase,
    observeThemeMode: ObserveThemeModeUseCase,
) : ViewModel() {

    val session: StateFlow<SessionState> = observeCurrentUser()
        .map { user -> if (user == null) SessionState.SignedOut else SessionState.SignedIn }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SessionState.Loading)

    /** `null` enquanto a preferência ainda está sendo lida. */
    val themeMode: StateFlow<ThemeMode?> = observeThemeMode()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
