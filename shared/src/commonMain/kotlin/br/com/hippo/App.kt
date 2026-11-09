package br.com.hippo

import androidx.compose.runtime.Composable
import br.com.hippo.presentation.root.AppRoot

/** Ponto de entrada da interface, chamado pelo Android (MainActivity) e pelo iOS. */
@Composable
fun App() {
    AppRoot()
}
