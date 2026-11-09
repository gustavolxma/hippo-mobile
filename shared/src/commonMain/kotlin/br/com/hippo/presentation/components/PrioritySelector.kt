package br.com.hippo.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.hippo.domain.model.TaskPriority
import br.com.hippo.presentation.theme.HippoColors
import br.com.hippo.presentation.theme.HippoShapes
import br.com.hippo.presentation.theme.HippoTheme

/** Seletor segmentado do UI Kit (ex.: Baixa · Média · Alta, Claro · Escuro · Sistema). */
@Composable
fun <T> SegmentedSelector(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth().selectableGroup(),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .clip(HippoShapes.Small)
                    .background(if (isSelected) HippoColors.Accent else HippoColors.Track)
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        onClick = { onSelect(option) },
                    ),
            ) {
                Text(
                    text = label(option),
                    style = HippoTheme.type.caption.copy(fontSize = 14.sp),
                    color = if (isSelected) HippoColors.OnAccent else HippoColors.OnBackground,
                )
            }
        }
    }
}

/** Seletor de prioridade da tarefa (Baixa · Média · Alta). */
@Composable
fun PrioritySelector(
    selected: TaskPriority,
    onSelect: (TaskPriority) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        FieldLabel("Prioridade")
        SegmentedSelector(
            options = TaskPriority.entries,
            selected = selected,
            onSelect = onSelect,
            label = { it.label },
        )
    }
}
