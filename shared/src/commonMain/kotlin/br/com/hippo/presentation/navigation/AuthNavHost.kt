package br.com.hippo.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import br.com.hippo.presentation.auth.login.LoginRoute
import br.com.hippo.presentation.auth.register.RegisterRoute
import br.com.hippo.presentation.auth.verification.VerificationRoute
import kotlinx.serialization.Serializable

@Serializable
data object LoginDestination

@Serializable
data object RegisterDestination

@Serializable
data class VerificationDestination(val email: String)

/**
 * Fluxo de quem ainda não entrou. Tanto o Login quanto o Cadastro terminam na
 * Verificação de código, que é onde a sessão é aberta.
 */
@Composable
fun AuthNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = LoginDestination) {
        composable<LoginDestination> {
            LoginRoute(
                onNavigateToRegister = { navController.navigate(RegisterDestination) },
                onNavigateToVerification = { email -> navController.navigate(VerificationDestination(email)) },
            )
        }
        composable<RegisterDestination> {
            RegisterRoute(
                onNavigateToVerification = { email -> navController.navigate(VerificationDestination(email)) },
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable<VerificationDestination> { entry ->
            VerificationRoute(
                email = entry.toRoute<VerificationDestination>().email,
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}
