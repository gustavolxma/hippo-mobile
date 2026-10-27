package br.com.hippo.di

import br.com.hippo.core.storage.DATA_STORE_FILE_NAME
import br.com.hippo.core.storage.createDataStore
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
}
