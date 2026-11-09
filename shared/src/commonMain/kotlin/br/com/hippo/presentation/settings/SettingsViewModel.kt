package br.com.hippo.presentation.settings

import androidx.lifecycle.viewModelScope
import br.com.hippo.core.mvi.MviViewModel
import br.com.hippo.domain.usecase.LogoutUseCase
import br.com.hippo.domain.usecase.ObserveCurrentUserUseCase
import br.com.hippo.domain.usecase.ObserveThemeModeUseCase
import br.com.hippo.domain.usecase.SetThemeModeUseCase
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class SettingsViewModel(
    observeCurrentUser: ObserveCurrentUserUseCase,
    observeThemeMode: ObserveThemeModeUseCase,
    private val setThemeMode: SetThemeModeUseCase,
    private val logout: LogoutUseCase,
) : MviViewModel<SettingsState, SettingsIntent, SettingsEffect>(SettingsState()) {

    init {
        combine(observeCurrentUser(), observeThemeMode()) { user, mode ->
            setState { copy(userName = user?.name.orEmpty(), userEmail = user?.email.orEmpty(), themeMode = mode) }
        }.launchIn(viewModelScope)
    }

    override fun onIntent(intent: SettingsIntent) {
        when (intent) {
            // O tema muda na hora: a raiz do app observa a mesma preferência.
            is SettingsIntent.ThemeSelected -> viewModelScope.launch { setThemeMode(intent.mode) }
            SettingsIntent.ChangeAccount -> sendEffect(SettingsEffect.NavigateToChangeAccount)
            SettingsIntent.SmartNotifications ->
                sendEffect(SettingsEffect.ShowMessage("As notificações inteligentes chegam em uma próxima etapa"))
            SettingsIntent.Privacy ->
                sendEffect(SettingsEffect.ShowMessage("Privacidade e dados chega junto com a API"))
            // Ao limpar a sessão, a raiz do app volta sozinha para o Login.
            SettingsIntent.Logout -> viewModelScope.launch { logout() }
        }
    }
}
