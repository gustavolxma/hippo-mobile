package br.com.hippo.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.hippo.presentation.theme.HippoColors
import br.com.hippo.presentation.theme.HippoTheme
import hippo.shared.generated.resources.Res
import hippo.shared.generated.resources.ic_back
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/** Cabeçalho das telas internas: botão voltar + título grande. */
@Composable
fun HippoTopBar(title: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(44.dp).background(HippoColors.Surface, CircleShape),
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_back),
                contentDescription = "Voltar",
                tint = HippoColors.OnBackground,
            )
        }
        Text(
            text = title,
            style = HippoTheme.type.title.copy(fontSize = 24.sp),
            modifier = Modifier.semantics { heading() },
        )
    }
}

/** Título de seção ("Hoje", "Próximas"...). */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = HippoTheme.type.subtitle,
        modifier = modifier.semantics { heading() },
    )
}

data class BottomBarItem(val label: String, val icon: DrawableResource)

/** Barra inferior do protótipo: Início · Voz · Configurações. */
@Composable
fun HippoBottomBar(
    items: List<BottomBarItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceAround,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(HippoColors.Surface)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 8.dp, end = 8.dp, top = 10.dp, bottom = 12.dp)
            .selectableGroup(),
    ) {
        items.forEachIndexed { index, item ->
            val selected = index == selectedIndex
            val color = if (selected) HippoColors.Accent else HippoColors.OnMuted
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .widthIn(min = 88.dp)
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(index) }),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(painterResource(item.icon), contentDescription = null, tint = color)
                    Text(item.label, style = HippoTheme.type.label, color = color)
                }
            }
        }
    }
}
