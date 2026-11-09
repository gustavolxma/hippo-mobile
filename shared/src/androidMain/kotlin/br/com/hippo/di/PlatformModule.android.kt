package br.com.hippo.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import br.com.hippo.core.storage.DATABASE_FILE_NAME
import br.com.hippo.core.storage.DATA_STORE_FILE_NAME
import br.com.hippo.core.storage.createDataStore
import br.com.hippo.data.local.db.HippoDatabase
import br.com.hippo.data.reminder.AndroidReminderScheduler
import br.com.hippo.data.voice.AndroidVoiceRecognizer
import br.com.hippo.domain.reminder.ReminderScheduler
import br.com.hippo.domain.voice.VoiceRecognizer
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    // 10.0.2.2 é o "localhost" do computador visto de dentro do emulador Android.
    single { AppConfig(baseUrl = "http://10.0.2.2:8080/api/") }
    single<HttpClientEngine> { OkHttp.create() }
    single {
        createDataStore { androidContext().filesDir.resolve(DATA_STORE_FILE_NAME).absolutePath }
    }
    single<SqlDriver> { AndroidSqliteDriver(HippoDatabase.Schema, androidContext(), DATABASE_FILE_NAME) }
    single<VoiceRecognizer> { AndroidVoiceRecognizer(androidContext()) }
    single<ReminderScheduler> { AndroidReminderScheduler(androidContext()) }
}
