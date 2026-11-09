package br.com.hippo.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import br.com.hippo.domain.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Guarda a sessão do usuário (token de autenticação e dados básicos) no DataStore,
 * para que ele continue logado ao reabrir o app.
 */
class SessionStorage(private val dataStore: DataStore<Preferences>) {

    val accessToken: Flow<String?> = dataStore.data.map { it[ACCESS_TOKEN] }

    val user: Flow<User?> = dataStore.data.map { prefs ->
        val id = prefs[USER_ID] ?: return@map null
        if (prefs[ACCESS_TOKEN] == null) return@map null
        User(id = id, name = prefs[USER_NAME].orEmpty(), email = prefs[USER_EMAIL].orEmpty())
    }

    suspend fun currentAccessToken(): String? = accessToken.first()

    suspend fun saveSession(token: String, user: User) {
        dataStore.edit {
            it[ACCESS_TOKEN] = token
            it[USER_ID] = user.id
            it[USER_NAME] = user.name
            it[USER_EMAIL] = user.email
        }
    }

    /** Remove só os dados da sessão; preferências do aparelho (como o tema) ficam. */
    suspend fun clear() {
        dataStore.edit { prefs ->
            listOf(ACCESS_TOKEN, USER_ID, USER_NAME, USER_EMAIL).forEach { prefs.remove(it) }
        }
    }

    private companion object {
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val USER_ID = stringPreferencesKey("user_id")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_EMAIL = stringPreferencesKey("user_email")
    }
}
