package br.com.hippo.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.hippo.presentation.theme.HippoColors
import br.com.hippo.presentation.theme.HippoShapes
import br.com.hippo.presentation.theme.HippoTheme

private val ButtonMinHeight = 48.dp

/** Botão de ação principal (lima). */
@Composable
fun HippoPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    FilledHippoButton(text, onClick, modifier, enabled, container = HippoColors.Accent, content = HippoColors.OnAccent)
}

/** Botão para ações destrutivas, como excluir (vermelho claro). */
@Composable
fun HippoDestructiveButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    FilledHippoButton(text, onClick, modifier, enabled, container = HippoColors.Danger, content = HippoColors.OnDanger)
}

/** Botão secundário com contorno violeta. */
@Composable
fun HippoSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedHippoButton(text, onClick, modifier, enabled, accent = HippoColors.Primary, content = HippoColors.OnBackground)
}

/** Ação destrutiva menos enfática (contorno vermelho), como "Excluir tarefa". */
@Composable
fun HippoDangerOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedHippoButton(text, onClick, modifier, enabled, accent = HippoColors.Danger, content = HippoColors.Danger)
}

@Composable
private fun OutlinedHippoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    accent: Color,
    content: Color,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = HippoShapes.Medium,
        border = BorderStroke(2.dp, if (enabled) accent else HippoColors.Track),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = content,
            disabledContentColor = HippoColors.OnMuted,
        ),
        modifier = modifier.heightIn(min = ButtonMinHeight),
    ) {
        Text(text, style = HippoTheme.type.body.copy(fontWeight = FontWeight.ExtraBold, fontSize = 15.sp))
    }
}

@Composable
private fun FilledHippoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    container: Color,
    content: Color,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = HippoShapes.Medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = HippoColors.Track,
            disabledContentColor = HippoColors.OnMuted,
        ),
        modifier = modifier.heightIn(min = ButtonMinHeight),
    ) {
        Text(text, style = HippoTheme.type.body.copy(fontWeight = FontWeight.Black))
    }
}

/** Ação em forma de texto lima, como "Esqueci minha senha" e "Entrar". */
@Composable
fun HippoTextLink(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(
            contentColor = HippoColors.AccentText,
            disabledContentColor = HippoColors.OnMuted,
        ),
        modifier = modifier.heightIn(min = 44.dp),
    ) {
        Text(text, style = HippoTheme.type.caption.copy(fontSize = 14.sp))
    }
}
