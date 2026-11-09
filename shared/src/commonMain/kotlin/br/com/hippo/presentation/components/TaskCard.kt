package br.com.hippo.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.hippo.domain.model.Task
import br.com.hippo.presentation.theme.HippoColors
import br.com.hippo.presentation.theme.HippoShapes
import br.com.hippo.presentation.theme.HippoTheme
import br.com.hippo.presentation.theme.hippoCard
import hippo.shared.generated.resources.Res
import hippo.shared.generated.resources.ic_check
import org.jetbrains.compose.resources.painterResource

/** Cartão de tarefa do UI Kit: check redondo + título + "14:00 · Prioridade alta". */
@Composable
fun TaskCard(
    task: Task,
    showDate: Boolean,
    onToggleDone: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(HippoShapes.Large)
            .hippoCard(HippoShapes.Large)
            .clickable(onClickLabel = "Abrir detalhes", onClick = onClick)
            .padding(start = 8.dp, top = 8.dp, end = 16.dp, bottom = 8.dp),
    ) {
        DoneCheck(checked = task.isDone, onToggle = onToggleDone)
        Column(
            Modifier
                .weight(1f)
                .then(if (task.isDone) Modifier.alpha(0.6f) else Modifier),
        ) {
            val decoration = if (task.isDone) TextDecoration.LineThrough else null
            Text(
                text = task.title,
                style = HippoTheme.type.body.copy(fontSize = 16.sp, fontWeight = FontWeight.ExtraBold),
                textDecoration = decoration,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = task.subtitle(showDate),
                style = HippoTheme.type.body.copy(fontSize = 13.sp),
                color = HippoColors.OnMuted,
                textDecoration = decoration,
            )
        }
    }
}

@Composable
private fun DoneCheck(checked: Boolean, onToggle: () -> Unit) {
    // Área de toque de 44dp, mesmo com o círculo visual de 28dp (acessibilidade).
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(44.dp)
            .clip(HippoShapes.Circle)
            .toggleable(
                value = checked,
                role = Role.Checkbox,
                onValueChange = { onToggle() },
            ),
    ) {
        if (checked) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(28.dp).background(HippoColors.Accent, HippoShapes.Circle),
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_check),
                    contentDescription = "Concluída",
                    tint = HippoColors.OnAccent,
                    modifier = Modifier.size(18.dp),
                )
            }
        } else {
            Box(
                Modifier
                    .size(28.dp)
                    .border(3.dp, HippoColors.Primary, HippoShapes.Circle),
            )
        }
    }
}
