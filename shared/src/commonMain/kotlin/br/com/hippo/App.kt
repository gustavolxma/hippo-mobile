package br.com.hippo

import androidx.compose.runtime.Composable
import br.com.hippo.presentation.root.AppRoot
import br.com.hippo.presentation.theme.HippoTheme

@Composable
fun App() {
    HippoTheme {
        AppRoot()
    }
}
