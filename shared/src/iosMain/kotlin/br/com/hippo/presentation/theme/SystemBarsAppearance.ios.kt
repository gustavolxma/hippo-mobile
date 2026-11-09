package br.com.hippo.presentation.theme

import androidx.compose.runtime.Composable

/*
 * No iOS a barra de status segue a aparência do sistema. Se o usuário escolher um tema
 * diferente do sistema, os ícones podem ficar com pouco contraste; dá para tratar isso
 * quando houver um Mac para testar (preferredStatusBarStyle do UIViewController).
 */
@Composable
actual fun SystemBarsAppearance(darkTheme: Boolean) = Unit
