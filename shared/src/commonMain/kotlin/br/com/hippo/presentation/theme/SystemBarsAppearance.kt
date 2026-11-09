package br.com.hippo.presentation.theme

import androidx.compose.runtime.Composable

/**
 * Ajusta a cor dos ícones das barras do sistema (relógio, bateria...) para combinar
 * com o tema: ícones claros no tema escuro e escuros no tema claro.
 */
@Composable
expect fun SystemBarsAppearance(darkTheme: Boolean)
