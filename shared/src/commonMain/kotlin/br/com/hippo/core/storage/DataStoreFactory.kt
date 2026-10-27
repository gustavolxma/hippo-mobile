package br.com.hippo.core.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath

internal const val DATA_STORE_FILE_NAME = "hippo.preferences_pb"

/**
 * O DataStore é multiplataforma; só o caminho do arquivo muda entre Android e iOS.
 * Cada plataforma informa o caminho no seu `platformModule` (ver pasta di/).
 */
fun createDataStore(producePath: () -> String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(produceFile = { producePath().toPath() })
