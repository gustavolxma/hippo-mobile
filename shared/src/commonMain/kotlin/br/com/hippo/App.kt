package br.com.hippo

import androidx.compose.runtime.Composable
import br.com.hippo.presentation.navigation.AppNavHost
import br.com.hippo.presentation.theme.HippoTheme

@Composable
fun App() {
    HippoTheme {
        AppNavHost()
    }
}
