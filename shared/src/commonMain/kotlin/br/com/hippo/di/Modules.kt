package br.com.hippo.di

import br.com.hippo.core.network.createHttpClient
import br.com.hippo.data.local.DataStorePreferencesRepository
import br.com.hippo.data.local.SessionStorage
import br.com.hippo.data.local.db.HippoDatabase
import br.com.hippo.data.remote.TaskApi
import br.com.hippo.data.repository.FakeAuthRepository
import br.com.hippo.data.repository.LocalTaskRepository
import br.com.hippo.data.repository.RemoteTaskRepository
import br.com.hippo.data.voice.LocalCommandInterpreter
import br.com.hippo.domain.reminder.TaskReminderSync
import br.com.hippo.domain.repository.AuthRepository
import br.com.hippo.domain.repository.PreferencesRepository
import br.com.hippo.domain.repository.TaskRepository
import br.com.hippo.domain.usecase.CancelAllRemindersUseCase
import br.com.hippo.domain.usecase.DeleteTaskUseCase
import br.com.hippo.domain.usecase.GetTaskUseCase
import br.com.hippo.domain.usecase.InterpretVoiceCommandUseCase
import br.com.hippo.domain.usecase.LoginUseCase
import br.com.hippo.domain.usecase.LogoutUseCase
import br.com.hippo.domain.usecase.ObserveCurrentUserUseCase
import br.com.hippo.domain.usecase.ObserveThemeModeUseCase
import br.com.hippo.domain.usecase.ObserveTasksUseCase
import br.com.hippo.domain.usecase.RegisterUseCase
import br.com.hippo.domain.usecase.RequestAccountChangeCodeUseCase
import br.com.hippo.domain.usecase.ResendCodeUseCase
import br.com.hippo.domain.usecase.ResyncRemindersUseCase
import br.com.hippo.domain.usecase.SaveTaskUseCase
import br.com.hippo.domain.usecase.SetThemeModeUseCase
import br.com.hippo.domain.usecase.TaskInput
import br.com.hippo.domain.usecase.ToggleTaskDoneUseCase
import br.com.hippo.domain.usecase.UpdateAccountUseCase
import br.com.hippo.domain.usecase.VerifyCodeUseCase
import br.com.hippo.domain.voice.CommandInterpreter
import br.com.hippo.presentation.auth.login.LoginViewModel
import br.com.hippo.presentation.auth.register.RegisterViewModel
import br.com.hippo.presentation.auth.verification.VerificationViewModel
import br.com.hippo.presentation.home.HomeViewModel
import br.com.hippo.presentation.root.AppRootViewModel
import br.com.hippo.presentation.settings.SettingsViewModel
import br.com.hippo.presentation.settings.changeaccount.ChangeAccountViewModel
import br.com.hippo.presentation.taskdetails.TaskDetailsViewModel
import br.com.hippo.presentation.voice.VoiceViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.scope.Scope
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
    // O SqlDriver (SQLite de cada plataforma) vem do platformModule.
    single { HippoDatabase(get()) }
    singleOf(::SessionStorage)
    single { createHttpClient(engine = get(), baseUrl = get<AppConfig>().baseUrl, sessionStorage = get()) }
    singleOf(::TaskApi)
    single<TaskRepository> {
        if (get<AppConfig>().useFakeBackend) {
            LocalTaskRepository(database = get(), currentUser = get<SessionStorage>().user)
        } else {
            RemoteTaskRepository(get())
        }
    }
    // Ainda não existe login na API: quando existir, crie um RemoteAuthRepository e
    // escolha entre ele e o falso aqui, como no TaskRepository acima.
    single<AuthRepository> { FakeAuthRepository(get()) }
    single<PreferencesRepository> { DataStorePreferencesRepository(get()) }
    // Interpretação local por regras. Quando a API tiver a IA, troque por uma
    // implementação que envia o texto ao backend (a tela de Voz não muda).
    single<CommandInterpreter> { LocalCommandInterpreter() }
    // O ReminderScheduler vem do platformModule (Android/iOS).
    single { TaskReminderSync(scheduler = get(), clock = get()) }
}

/** No modo sem API, as telas de código mostram o código fixo aceito, para facilitar os testes. */
private fun Scope.demoCode(): String? =
    FakeAuthRepository.FAKE_CODE.takeIf { get<AuthRepository>() is FakeAuthRepository }

val domainModule = module {
    factoryOf(::ObserveTasksUseCase)
    factoryOf(::GetTaskUseCase)
    factoryOf(::SaveTaskUseCase)
    factoryOf(::ToggleTaskDoneUseCase)
    factoryOf(::DeleteTaskUseCase)

    factoryOf(::ObserveCurrentUserUseCase)
    factoryOf(::LoginUseCase)
    factoryOf(::RegisterUseCase)
    factoryOf(::VerifyCodeUseCase)
    factoryOf(::ResendCodeUseCase)
    factoryOf(::LogoutUseCase)
    factoryOf(::RequestAccountChangeCodeUseCase)
    factoryOf(::UpdateAccountUseCase)

    factoryOf(::ObserveThemeModeUseCase)
    factoryOf(::SetThemeModeUseCase)

    factoryOf(::InterpretVoiceCommandUseCase)

    factoryOf(::ResyncRemindersUseCase)
    factoryOf(::CancelAllRemindersUseCase)
}

val presentationModule = module {
    viewModelOf(::AppRootViewModel)
    viewModelOf(::LoginViewModel)
    viewModelOf(::RegisterViewModel)
    viewModel { params -> VerificationViewModel(params.get(), demoCode(), get(), get()) }
    viewModelOf(::HomeViewModel)
    viewModel { params ->
        TaskDetailsViewModel(params.getOrNull<String>(), params.getOrNull<TaskInput>(), get(), get(), get())
    }
    viewModelOf(::VoiceViewModel)
    viewModelOf(::SettingsViewModel)
    viewModel { ChangeAccountViewModel(demoCode(), get(), get(), get()) }
}

fun initKoin(config: KoinAppDeclaration? = null) {
    startKoin {
        config?.invoke(this)
        modules(platformModule, dataModule, domainModule, presentationModule)
    }
}
