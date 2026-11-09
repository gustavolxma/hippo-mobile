package br.com.hippo.presentation.components

import androidx.compose.ui.platform.ClipEntry

/** Conteúdo de texto simples para a área de transferência (cada plataforma cria o seu). */
expect fun plainTextClipEntry(label: String, text: String): ClipEntry
