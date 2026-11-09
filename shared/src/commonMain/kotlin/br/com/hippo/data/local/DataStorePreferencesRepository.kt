package br.com.hippo.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import br.com.hippo.domain.model.ThemeMode
import br.com.hippo.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DataStorePreferencesRepository(private val dataStore: DataStore<Preferences>) : PreferencesRepository {

    // Sem escolha salva, o app segue o sistema.
    override val themeMode: Flow<ThemeMode> = dataStore.data.map { prefs ->
        ThemeMode.entries.firstOrNull { it.name == prefs[THEME_MODE] } ?: ThemeMode.SYSTEM
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[THEME_MODE] = mode.name }
    }

    private companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }
}
