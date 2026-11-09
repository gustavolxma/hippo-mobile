package br.com.hippo.presentation.components

import androidx.compose.runtime.Composable

/**
 * Devolve uma função que pede permissão para mostrar notificações (lembretes).
 * Pedimos só quando o usuário escolhe um lembrete, para o pedido fazer sentido para ele.
 * Se já foi concedida (ou o sistema não exige), não mostra nada.
 */
@Composable
expect fun rememberNotificationPermissionRequest(): () -> Unit
