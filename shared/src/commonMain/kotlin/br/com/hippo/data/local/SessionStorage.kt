package br.com.hippo.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Guarda a sessão do usuário (token de autenticação) usando o DataStore. */
class SessionStorage(private val dataStore: DataStore<Preferences>) {

    val accessToken: Flow<String?> = dataStore.data.map { it[ACCESS_TOKEN] }

    suspend fun currentAccessToken(): String? = accessToken.first()

    suspend fun saveAccessToken(token: String) {
        dataStore.edit { it[ACCESS_TOKEN] = token }
    }

    suspend fun clear() {
        dataStore.edit { it.remove(ACCESS_TOKEN) }
    }

    private companion object {
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
    }
}
