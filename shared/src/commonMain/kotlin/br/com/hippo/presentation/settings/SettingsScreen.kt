package br.com.hippo.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.hippo.domain.model.ThemeMode
import br.com.hippo.presentation.components.SegmentedSelector
import br.com.hippo.presentation.theme.HippoColors
import br.com.hippo.presentation.theme.HippoShapes
import br.com.hippo.presentation.theme.HippoTheme
import br.com.hippo.presentation.theme.hippoCard
import hippo.shared.generated.resources.Res
import hippo.shared.generated.resources.ic_chevron_right
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsRoute(
    onNavigateToChangeAccount: () -> Unit,
    onNavigateToVoiceMetrics: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                SettingsEffect.NavigateToChangeAccount -> onNavigateToChangeAccount()
                SettingsEffect.NavigateToVoiceMetrics -> onNavigateToVoiceMetrics()
                is SettingsEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        SettingsScreen(state, viewModel::onIntent)
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
    }
}

private val ThemeMode.label: String
    get() = when (this) {
        ThemeMode.LIGHT -> "Claro"
        ThemeMode.DARK -> "Escuro"
        ThemeMode.SYSTEM -> "Sistema"
    }

@Composable
fun SettingsScreen(state: SettingsState, onIntent: (SettingsIntent) -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 16.dp),
    ) {
        Text("Configurações", style = HippoTheme.type.title, modifier = Modifier.semantics { heading() })

        SettingsCard(title = "Aparência") {
            SegmentedSelector(
                options = listOf(ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.SYSTEM),
                selected = state.themeMode,
                onSelect = { onIntent(SettingsIntent.ThemeSelected(it)) },
                label = { it.label },
            )
        }

        SettingsCard(title = "Dados do usuário") {
            InfoRow("Nome", state.userName)
            InfoRow("E-mail", state.userEmail)
        }

        SettingsRow("Alterar e-mail ou senha", onClick = { onIntent(SettingsIntent.ChangeAccount) })
        SettingsRow("Notificações inteligentes", onClick = { onIntent(SettingsIntent.SmartNotifications) })
        SettingsRow("Privacidade e dados", onClick = { onIntent(SettingsIntent.Privacy) })
        SettingsRow("Métricas de voz", onClick = { onIntent(SettingsIntent.VoiceMetrics) })
        SettingsRow("Sair da conta", onClick = { onIntent(SettingsIntent.Logout) }, textColor = HippoColors.Danger)
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .hippoCard(HippoShapes.Large)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            title,
            style = HippoTheme.type.body.copy(fontSize = 16.sp, fontWeight = FontWeight.Black),
            modifier = Modifier.semantics { heading() },
        )
        content()
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(label, style = HippoTheme.type.body.copy(fontSize = 14.sp), color = HippoColors.OnMuted)
        Text(
            value,
            style = HippoTheme.type.body.copy(fontSize = 14.sp, fontWeight = FontWeight.ExtraBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End,
        )
    }
}

/** Linha clicável das Configurações: texto à esquerda e seta à direita. */
@Composable
private fun SettingsRow(text: String, onClick: () -> Unit, textColor: Color = HippoColors.OnBackground) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(HippoShapes.Large)
            .hippoCard(HippoShapes.Large)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(PaddingValues(horizontal = 16.dp)),
    ) {
        Text(
            text,
            style = HippoTheme.type.body.copy(fontWeight = FontWeight.ExtraBold),
            color = textColor,
            modifier = Modifier.weight(1f),
        )
        Icon(
            painter = painterResource(Res.drawable.ic_chevron_right),
            contentDescription = null,
            tint = HippoColors.OnMuted,
        )
    }
}
