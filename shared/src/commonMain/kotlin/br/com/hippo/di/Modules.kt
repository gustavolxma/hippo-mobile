package br.com.hippo.di

import br.com.hippo.core.network.createHttpClient
import br.com.hippo.data.local.SessionStorage
import br.com.hippo.data.remote.TaskApi
import br.com.hippo.data.repository.InMemoryTaskRepository
import br.com.hippo.data.repository.RemoteTaskRepository
import br.com.hippo.domain.repository.TaskRepository
import br.com.hippo.domain.usecase.DeleteTaskUseCase
import br.com.hippo.domain.usecase.GetTaskUseCase
import br.com.hippo.domain.usecase.ObserveTasksUseCase
import br.com.hippo.domain.usecase.SaveTaskUseCase
import br.com.hippo.domain.usecase.ToggleTaskDoneUseCase
import br.com.hippo.presentation.home.HomeViewModel
import br.com.hippo.presentation.taskdetails.TaskDetailsViewModel
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import kotlin.time.Clock

/** Configurações que variam por plataforma/ambiente. */
data class AppConfig(
    val baseUrl: String,
    /** Enquanto o backend não existir, mantenha `true` para usar dados em memória. */
    val useFakeBackend: Boolean = true,
)

/** Cada plataforma fornece: AppConfig, HttpClientEngine e DataStore. */
expect val platformModule: Module

val dataModule = module {
    single<Clock> { Clock.System }
    singleOf(::SessionStorage)
    single { createHttpClient(engine = get(), baseUrl = get<AppConfig>().baseUrl, sessionStorage = get()) }
    singleOf(::TaskApi)
    single<TaskRepository> {
        if (get<AppConfig>().useFakeBackend) {
            val today = get<Clock>().todayIn(TimeZone.currentSystemDefault())
            InMemoryTaskRepository(InMemoryTaskRepository.sampleTasks(today))
        } else {
            RemoteTaskRepository(get())
        }
    }
}

val domainModule = module {
    factoryOf(::ObserveTasksUseCase)
    factoryOf(::GetTaskUseCase)
    factoryOf(::SaveTaskUseCase)
    factoryOf(::ToggleTaskDoneUseCase)
    factoryOf(::DeleteTaskUseCase)
}

val presentationModule = module {
    viewModelOf(::HomeViewModel)
    viewModel { params -> TaskDetailsViewModel(params.getOrNull(), get(), get(), get()) }
}

fun initKoin(config: KoinAppDeclaration? = null) {
    startKoin {
        config?.invoke(this)
        modules(platformModule, dataModule, domainModule, presentationModule)
    }
}
