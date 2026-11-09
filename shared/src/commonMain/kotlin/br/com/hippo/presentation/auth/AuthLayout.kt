package br.com.hippo.presentation.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.hippo.presentation.components.HippoTextLink
import br.com.hippo.presentation.theme.HippoColors
import br.com.hippo.presentation.theme.HippoShapes
import br.com.hippo.presentation.theme.HippoTheme
import br.com.hippo.presentation.theme.hippoCard

/** Estrutura comum das telas de autenticação: rolagem, margens e espaço do teclado. */
@Composable
internal fun AuthScaffold(
    snackbarHostState: SnackbarHostState? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(Modifier.fillMaxSize().background(HippoColors.Background)) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            content = content,
        )
        if (snackbarHostState != null) {
            SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter).safeDrawingPadding())
        }
    }
}

/** Cartão que agrupa os campos do formulário, como no protótipo. */
@Composable
internal fun FormCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .hippoCard(HippoShapes.Large)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        content = content,
    )
}

/** Erro geral do formulário (ex.: "E-mail ou senha incorretos"), anunciado por leitores de tela. */
@Composable
internal fun FormErrorText(message: String?) {
    if (message == null) return
    Text(
        text = message,
        style = HippoTheme.type.caption,
        color = HippoColors.Danger,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}

/** "Reenviar código em 0:45" durante a espera; depois, o link "Reenviar código". */
@Composable
internal fun ResendCode(secondsLeft: Int, onResend: () -> Unit) {
    if (secondsLeft > 0) {
        Text(
            "Reenviar código em ${formatCountdown(secondsLeft)}",
            style = HippoTheme.type.body.copy(fontSize = 14.sp),
            color = HippoColors.OnMuted,
        )
    } else {
        HippoTextLink("Reenviar código", onClick = onResend)
    }
}
