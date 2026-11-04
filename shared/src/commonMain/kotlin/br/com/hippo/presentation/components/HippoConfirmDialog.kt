package br.com.hippo.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.window.Dialog
import br.com.hippo.presentation.theme.HippoColors
import br.com.hippo.presentation.theme.HippoShapes
import br.com.hippo.presentation.theme.HippoTheme
import hippo.shared.generated.resources.Res
import hippo.shared.generated.resources.ic_trash
import org.jetbrains.compose.resources.painterResource

/** Dialog de confirmação de ação destrutiva, como "Excluir tarefa?". */
@Composable
fun HippoConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .background(HippoColors.Surface, HippoShapes.ExtraLarge)
                .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 20.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .background(HippoColors.Danger.copy(alpha = 0.16f), HippoShapes.Circle),
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_trash),
                    contentDescription = null,
                    tint = HippoColors.Danger,
                    modifier = Modifier.size(28.dp),
                )
            }
            Text(
                text = title,
                style = HippoTheme.type.subtitle.copy(fontSize = 20.sp),
                modifier = Modifier.padding(top = 14.dp, bottom = 8.dp).semantics { heading() },
            )
            Text(
                text = message,
                style = HippoTheme.type.body.copy(fontSize = 14.sp),
                color = HippoColors.OnMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 20.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HippoSecondaryButton("Cancelar", onClick = onDismiss, modifier = Modifier.weight(1f))
                HippoDestructiveButton(confirmText, onClick = onConfirm, modifier = Modifier.weight(1f))
            }
        }
    }
}
