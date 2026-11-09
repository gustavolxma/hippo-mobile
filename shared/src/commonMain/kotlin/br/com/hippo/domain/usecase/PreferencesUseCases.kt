package br.com.hippo.domain.usecase

import br.com.hippo.domain.model.ThemeMode
import br.com.hippo.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow

class ObserveThemeModeUseCase(private val repository: PreferencesRepository) {
    operator fun invoke(): Flow<ThemeMode> = repository.themeMode
}

class SetThemeModeUseCase(private val repository: PreferencesRepository) {
    suspend operator fun invoke(mode: ThemeMode) = repository.setThemeMode(mode)
}
