package br.com.hippo.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import br.com.hippo.presentation.taskform.TaskFormRoute
import br.com.hippo.presentation.tasklist.TaskListRoute
import kotlinx.serialization.Serializable

/** Destinos type-safe: cada tela é um tipo serializável (podem carregar argumentos). */
@Serializable
data object TaskListDestination

@Serializable
data object TaskFormDestination

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = TaskListDestination) {
        composable<TaskListDestination> {
            TaskListRoute(onNavigateToTaskForm = { navController.navigate(TaskFormDestination) })
        }
        composable<TaskFormDestination> {
            TaskFormRoute(onNavigateBack = { navController.popBackStack() })
        }
    }
}
