package br.com.hippo.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import br.com.hippo.presentation.components.HippoDangerOutlinedButton
import br.com.hippo.presentation.theme.HippoColors
import br.com.hippo.presentation.theme.HippoTheme
import org.koin.compose.viewmodel.koinViewModel

/** Esqueleto da tela de Configurações; o conteúdo do protótipo entra em outra etapa. */
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = koinViewModel()) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 20.dp, end = 20.dp, top = 24.dp),
    ) {
        Text("Configurações", style = HippoTheme.type.title, modifier = Modifier.semantics { heading() })
        Text("Em breve.", style = HippoTheme.type.body, color = HippoColors.OnMuted)
        HippoDangerOutlinedButton(
            text = "Sair da conta",
            onClick = viewModel::onLogout,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        )
    }
}
