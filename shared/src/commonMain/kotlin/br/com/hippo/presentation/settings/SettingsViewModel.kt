package br.com.hippo.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.hippo.domain.usecase.LogoutUseCase
import kotlinx.coroutines.launch

/** Por enquanto só "Sair da conta"; o restante das Configurações entra em outra etapa. */
class SettingsViewModel(private val logout: LogoutUseCase) : ViewModel() {

    fun onLogout() {
        // Ao limpar a sessão, a raiz do app volta sozinha para o Login.
        viewModelScope.launch { logout() }
    }
}
