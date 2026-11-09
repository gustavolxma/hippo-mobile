package br.com.hippo.presentation.root

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.hippo.domain.model.ThemeMode
import br.com.hippo.presentation.navigation.AppNavHost
import br.com.hippo.presentation.navigation.AuthNavHost
import br.com.hippo.presentation.theme.HippoColors
import br.com.hippo.presentation.theme.HippoTheme
import br.com.hippo.presentation.theme.SystemBarsAppearance
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AppRoot(viewModel: AppRootViewModel = koinViewModel()) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM, null -> isSystemInDarkTheme()
    }
    SystemBarsAppearance(darkTheme)

    HippoTheme(darkTheme = darkTheme) {
        // Enquanto sessão e tema são lidos (é rápido), só o fundo evita um "pisca" de tela.
        if (themeMode == null || session == SessionState.Loading) {
            Box(Modifier.fillMaxSize().background(HippoColors.Background))
            return@HippoTheme
        }
        when (session) {
            SessionState.SignedOut -> AuthNavHost()
            else -> AppNavHost()
        }
    }
}
