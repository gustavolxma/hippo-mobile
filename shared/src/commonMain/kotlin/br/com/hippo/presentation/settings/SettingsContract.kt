package br.com.hippo.presentation.settings

import br.com.hippo.domain.model.ThemeMode

data class SettingsState(
    val userName: String = "",
    val userEmail: String = "",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
)

sealed interface SettingsIntent {
    data class ThemeSelected(val mode: ThemeMode) : SettingsIntent
    data object ChangeAccount : SettingsIntent
    data object SmartNotifications : SettingsIntent
    data object Privacy : SettingsIntent
    data object Logout : SettingsIntent
}

sealed interface SettingsEffect {
    data object NavigateToChangeAccount : SettingsEffect
    data class ShowMessage(val message: String) : SettingsEffect
}
