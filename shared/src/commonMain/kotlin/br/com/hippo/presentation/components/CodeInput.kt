package br.com.hippo.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.hippo.presentation.theme.HippoColors
import br.com.hippo.presentation.theme.HippoShapes
import br.com.hippo.presentation.theme.HippoTheme

/**
 * Campo de código de verificação (ex.: 6 dígitos). É um único campo de texto desenhado
 * como caixas separadas, então colar o código inteiro e apagar funcionam naturalmente.
 */
@Composable
fun CodeInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    length: Int = 6,
    isError: Boolean = false,
    onDone: () -> Unit = {},
) {
    var focused by remember { mutableStateOf(false) }
    // Mesmo cuidado do HippoTextField: o texto fica aqui para não perder dígitos.
    var fieldValue by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
    if (!focused && value != fieldValue.text) {
        fieldValue = TextFieldValue(value, TextRange(value.length))
    }

    BasicTextField(
        value = fieldValue,
        onValueChange = { newValue ->
            val digits = newValue.text.filter { it.isDigit() }.take(length)
            val changed = digits != fieldValue.text
            fieldValue = TextFieldValue(digits, TextRange(digits.length))
            if (changed) onValueChange(digits)
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .semantics { contentDescription = "Código de $length dígitos" },
        decorationBox = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                modifier = Modifier.fillMaxWidth(),
            ) {
                val text = fieldValue.text
                repeat(length) { index ->
                    val char = text.getOrNull(index)
                    val isCurrent = focused && index == text.length.coerceAtMost(length - 1)
                    val borderColor = when {
                        isError -> HippoColors.Danger
                        char != null -> HippoColors.AccentText
                        isCurrent -> HippoColors.Primary
                        else -> HippoColors.Track
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(width = 46.dp, height = 56.dp)
                            .background(HippoColors.Background, HippoShapes.Medium)
                            .border(2.dp, borderColor, HippoShapes.Medium),
                    ) {
                        Text(
                            text = char?.toString().orEmpty(),
                            style = HippoTheme.type.title.copy(fontSize = 22.sp),
                        )
                    }
                }
            }
        },
    )
}
