package br.com.hippo

import androidx.compose.ui.window.ComposeUIViewController
import br.com.hippo.di.initKoin
import platform.UIKit.UIViewController

/** Chamado pelo Swift (ContentView.swift) uma única vez, ao criar a tela. */
fun MainViewController(): UIViewController {
    initKoin()
    return ComposeUIViewController { App() }
}
