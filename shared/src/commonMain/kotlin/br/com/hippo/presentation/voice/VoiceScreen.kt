package br.com.hippo.presentation.voice

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.hippo.presentation.components.HippoSecondaryButton
import br.com.hippo.presentation.theme.HippoColors
import br.com.hippo.presentation.theme.HippoShapes
import br.com.hippo.presentation.theme.HippoTheme
import br.com.hippo.presentation.theme.hippoCard
import hippo.shared.generated.resources.Res
import hippo.shared.generated.resources.ic_mic
import org.jetbrains.compose.resources.painterResource

/**
 * Esqueleto da tela de voz do protótipo. O reconhecimento de voz entra na próxima etapa;
 * por enquanto ela oferece a criação manual de tarefas.
 */
@Composable
fun VoiceScreen(onCreateManually: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 16.dp),
    ) {
        Text("Fale com o Hippo", style = HippoTheme.type.title, modifier = Modifier.semantics { heading() })
        Text(
            "Toque no microfone e diga o que precisa",
            style = HippoTheme.type.body.copy(fontSize = 14.sp),
            color = HippoColors.OnMuted,
        )
        MicHalo()
        Text(
            "O reconhecimento de voz chega em breve.",
            style = HippoTheme.type.body,
            color = HippoColors.OnMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .hippoCard(HippoShapes.Large)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        )
        HippoSecondaryButton("Criar tarefa manualmente", onClick = onCreateManually, modifier = Modifier.fillMaxWidth())
    }
}

/** Círculos concêntricos violeta com o microfone lima no centro. */
@Composable
private fun MicHalo() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(190.dp).background(HippoColors.Primary.copy(alpha = 0.22f), HippoShapes.Circle),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(140.dp).background(HippoColors.Primary.copy(alpha = 0.45f), HippoShapes.Circle),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(96.dp).background(HippoColors.Track, HippoShapes.Circle),
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_mic),
                    contentDescription = "Microfone (em breve)",
                    tint = HippoColors.OnMuted,
                    modifier = Modifier.size(44.dp),
                )
            }
        }
    }
}
