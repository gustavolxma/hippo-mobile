package br.com.hippo.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.hippo.presentation.theme.HippoColors
import br.com.hippo.presentation.theme.HippoShapes
import br.com.hippo.presentation.theme.HippoTheme

/** Rótulo pequeno acima dos campos ("Título", "Prioridade"...). */
@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = HippoTheme.type.caption.copy(fontSize = 12.sp),
        color = HippoColors.OnMuted,
        modifier = modifier.padding(bottom = 6.dp),
    )
}

/** Campo de texto no estilo do UI Kit: rótulo em cima, borda que acende ao focar. */
@Composable
fun HippoTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1,
    error: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    /** Esconde o texto digitado (campos de senha). */
    isPassword: Boolean = false,
    enabled: Boolean = true,
) {
    var focused by remember { mutableStateOf(false) }

    // O texto vive aqui, de forma síncrona. Se o campo dependesse só do `value` vindo do
    // ViewModel (StateFlow, entregue à tela com atraso), letras digitadas em sequência
    // rápida seriam descartadas. Por isso, enquanto o campo está em foco, quem manda é o
    // que o usuário digita; o `value` externo só é aplicado fora de foco (ex.: tarefa carregada).
    var fieldValue by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
    if (!focused && value != fieldValue.text) {
        fieldValue = TextFieldValue(value, TextRange(value.length))
    }
    val borderColor = when {
        error != null -> HippoColors.Danger
        focused -> HippoColors.Primary
        else -> HippoColors.Track
    }
    Column(modifier) {
        FieldLabel(label)
        BasicTextField(
            value = fieldValue,
            onValueChange = { newValue ->
                val textChanged = newValue.text != fieldValue.text
                fieldValue = newValue
                // Mudanças só de cursor/seleção não interessam ao ViewModel.
                if (textChanged) onValueChange(newValue.text)
            },
            singleLine = singleLine,
            minLines = minLines,
            enabled = enabled,
            keyboardOptions = if (isPassword) keyboardOptions.copy(keyboardType = KeyboardType.Password) else keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            textStyle = HippoTheme.type.body.copy(color = HippoColors.OnBackground),
            cursorBrush = SolidColor(HippoColors.Primary),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focused = it.isFocused },
            decorationBox = { inner ->
                FieldFrame(borderColor) { inner() }
            },
        )
        if (error != null) {
            Spacer(Modifier.height(4.dp))
            Text(error, style = HippoTheme.type.caption, color = HippoColors.Danger)
        }
    }
}

/**
 * Campo somente leitura que abre um seletor ao ser tocado (data, hora, lembrete).
 * Visualmente igual ao [HippoTextField].
 */
@Composable
fun HippoPickerField(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    enabled: Boolean = true,
) {
    Column(modifier) {
        FieldLabel(label)
        Box(
            Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, role = Role.Button, onClickLabel = label, onClick = onClick),
        ) {
            FieldFrame(HippoColors.Track) {
                Text(
                    text = value.ifEmpty { placeholder },
                    style = HippoTheme.type.body,
                    color = if (value.isEmpty() || !enabled) HippoColors.OnMuted else HippoColors.OnBackground,
                )
            }
        }
    }
}

@Composable
private fun FieldFrame(borderColor: Color, content: @Composable () -> Unit) {
    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .background(HippoColors.Background, HippoShapes.Medium)
            .border(2.dp, borderColor, HippoShapes.Medium)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        content()
    }
}
