package br.com.hippo.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import br.com.hippo.presentation.components.BottomBarItem
import br.com.hippo.presentation.components.HippoBottomBar
import br.com.hippo.presentation.home.HomeRoute
import br.com.hippo.presentation.settings.SettingsScreen
import br.com.hippo.presentation.taskdetails.TaskDetailsRoute
import br.com.hippo.presentation.theme.HippoColors
import br.com.hippo.presentation.voice.VoiceScreen
import hippo.shared.generated.resources.Res
import hippo.shared.generated.resources.ic_home
import hippo.shared.generated.resources.ic_mic
import hippo.shared.generated.resources.ic_settings
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

/** Destinos type-safe: cada tela é um tipo serializável (podem carregar argumentos). */
@Serializable
data object HomeDestination

@Serializable
data object VoiceDestination

@Serializable
data object SettingsDestination

/** [taskId] nulo abre a tela em modo "nova tarefa". */
@Serializable
data class TaskDetailsDestination(val taskId: String? = null)

/** Abas da barra inferior, na ordem em que aparecem. */
private enum class Tab(val destination: Any, val route: KClass<*>, val item: BottomBarItem) {
    HOME(HomeDestination, HomeDestination::class, BottomBarItem("Início", Res.drawable.ic_home)),
    VOICE(VoiceDestination, VoiceDestination::class, BottomBarItem("Voz", Res.drawable.ic_mic)),
    SETTINGS(SettingsDestination, SettingsDestination::class, BottomBarItem("Configurações", Res.drawable.ic_settings)),
}

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentTab = Tab.entries.firstOrNull { tab ->
        backStackEntry?.destination?.hasRoute(tab.route) == true
    }

    Column(Modifier.fillMaxSize().background(HippoColors.Background)) {
        Box(
            Modifier
                .weight(1f)
                // As telas com barra inferior cuidam só da barra de status;
                // Detalhes trata todos os recortes da tela sozinha.
                .then(if (currentTab != null) Modifier.windowInsetsPadding(WindowInsets.statusBars) else Modifier),
        ) {
            NavHost(navController = navController, startDestination = HomeDestination) {
                composable<HomeDestination> {
                    HomeRoute(onOpenTask = { navController.navigate(TaskDetailsDestination(it)) })
                }
                composable<VoiceDestination> {
                    VoiceScreen(onCreateManually = { navController.navigate(TaskDetailsDestination()) })
                }
                composable<SettingsDestination> {
                    SettingsScreen()
                }
                composable<TaskDetailsDestination> { entry ->
                    val args = entry.toRoute<TaskDetailsDestination>()
                    TaskDetailsRoute(
                        taskId = args.taskId,
                        onNavigateBack = { navController.popBackStack() },
                    )
                }
            }
        }
        if (currentTab != null) {
            HippoBottomBar(
                items = Tab.entries.map { it.item },
                selectedIndex = currentTab.ordinal,
                onSelect = { index -> navController.navigateToTab(Tab.entries[index]) },
            )
        }
    }
}

/** Troca de aba sem empilhar telas repetidas e preservando o estado de cada aba. */
private fun NavHostController.navigateToTab(tab: Tab) {
    navigate(tab.destination) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
