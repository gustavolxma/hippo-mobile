package br.com.hippo.di

import br.com.hippo.core.storage.DATA_STORE_FILE_NAME
import br.com.hippo.core.storage.createDataStore
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/*
 * Repare: este é código "iOS", mas escrito em Kotlin. Os pacotes `platform.*`
 * expõem as APIs da Apple (Foundation, UIKit, Speech...) diretamente para o Kotlin,
 * então quase nunca é preciso escrever Swift.
 */
actual val platformModule: Module = module {
    // No simulador iOS, localhost aponta para o próprio Mac.
    single { AppConfig(baseUrl = "http://localhost:8080/api/") }
    single<HttpClientEngine> { Darwin.create() }
    single { createDataStore { documentDirectory() + "/$DATA_STORE_FILE_NAME" } }
}

@OptIn(ExperimentalForeignApi::class)
private fun documentDirectory(): String {
    val url = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )
    return requireNotNull(url?.path) { "Diretório de documentos não encontrado" }
}
