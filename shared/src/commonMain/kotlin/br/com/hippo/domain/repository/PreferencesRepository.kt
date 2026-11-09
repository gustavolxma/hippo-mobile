package br.com.hippo.domain.repository

import br.com.hippo.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

/** Preferências do aparelho, que continuam valendo mesmo depois de sair da conta. */
interface PreferencesRepository {
    val themeMode: Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
}
