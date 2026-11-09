package br.com.hippo.presentation.root

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.hippo.presentation.navigation.AppNavHost
import br.com.hippo.presentation.navigation.AuthNavHost
import br.com.hippo.presentation.theme.HippoColors
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AppRoot(viewModel: AppRootViewModel = koinViewModel()) {
    val session by viewModel.session.collectAsStateWithLifecycle()

    when (session) {
        // Leitura da sessão é rápida; só o fundo evita um "pisca" da tela de login.
        SessionState.Loading -> Box(Modifier.fillMaxSize().background(HippoColors.Background))
        SessionState.SignedOut -> AuthNavHost()
        SessionState.SignedIn -> AppNavHost()
    }
}
