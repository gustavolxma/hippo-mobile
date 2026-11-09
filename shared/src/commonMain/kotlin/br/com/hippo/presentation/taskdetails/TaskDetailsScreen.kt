package br.com.hippo.presentation.taskdetails

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.hippo.domain.model.TaskReminder
import br.com.hippo.domain.usecase.TaskInput
import br.com.hippo.presentation.components.HippoConfirmDialog
import br.com.hippo.presentation.components.HippoDangerOutlinedButton
import br.com.hippo.presentation.components.HippoPickerField
import br.com.hippo.presentation.components.HippoPrimaryButton
import br.com.hippo.presentation.components.HippoTextField
import br.com.hippo.presentation.components.HippoTopBar
import br.com.hippo.presentation.components.PrioritySelector
import br.com.hippo.presentation.components.formatted
import br.com.hippo.presentation.components.rememberNotificationPermissionRequest
import br.com.hippo.presentation.components.label
import br.com.hippo.presentation.theme.HippoColors
import br.com.hippo.presentation.theme.HippoShapes
import br.com.hippo.presentation.theme.HippoTheme
import br.com.hippo.presentation.theme.hippoCard
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.time.Instant

@Composable
fun TaskDetailsRoute(
    taskId: String?,
    onNavigateBack: () -> Unit,
    draft: TaskInput? = null,
    // Cada entrada da navegação tem seu próprio ViewModel; os parâmetros só valem na criação.
    viewModel: TaskDetailsViewModel = koinViewModel { parametersOf(taskId, draft) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                TaskDetailsEffect.NavigateBack -> onNavigateBack()
                is TaskDetailsEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        TaskDetailsScreen(state, viewModel::onIntent)
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
fun TaskDetailsScreen(state: TaskDetailsState, onIntent: (TaskDetailsIntent) -> Unit) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(HippoColors.Background)
            .safeDrawingPadding()
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        HippoTopBar(
            title = if (state.isNew) "Nova tarefa" else "Detalhes da tarefa",
            onBack = { onIntent(TaskDetailsIntent.Back) },
        )

        if (state.isLoading) {
            Box(Modifier.fillMaxWidth().weight(1f), Alignment.Center) {
                CircularProgressIndicator(color = HippoColors.AccentText)
            }
            return@Column
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .hippoCard(HippoShapes.Large)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                HippoTextField(
                    label = "Título",
                    value = state.title,
                    onValueChange = { onIntent(TaskDetailsIntent.TitleChanged(it)) },
                    error = state.titleError,
                )
                HippoTextField(
                    label = "Descrição",
                    value = state.description,
                    onValueChange = { onIntent(TaskDetailsIntent.DescriptionChanged(it)) },
                    singleLine = false,
                    minLines = 2,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HippoPickerField(
                        label = "Data",
                        value = state.dueDate?.formatted().orEmpty(),
                        placeholder = "Sem data",
                        onClick = { showDatePicker = true },
                        modifier = Modifier.weight(1f),
                    )
                    HippoPickerField(
                        label = "Hora",
                        value = state.dueTime?.formatted().orEmpty(),
                        placeholder = "Sem hora",
                        onClick = { showTimePicker = true },
                        modifier = Modifier.weight(1f),
                    )
                }
                state.dateError?.let {
                    Text(it, style = HippoTheme.type.caption, color = HippoColors.Danger)
                }
                PrioritySelector(
                    selected = state.priority,
                    onSelect = { onIntent(TaskDetailsIntent.PriorityChanged(it)) },
                )
                ReminderRow(
                    reminder = state.reminder,
                    enabled = state.dueTime != null,
                    onSelect = { onIntent(TaskDetailsIntent.ReminderChanged(it)) },
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            HippoPrimaryButton(
                text = if (state.isNew) "Criar tarefa" else "Salvar alterações",
                onClick = { onIntent(TaskDetailsIntent.Save) },
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            )
            if (!state.isNew) {
                HippoDangerOutlinedButton(
                    text = "Excluir tarefa",
                    onClick = { onIntent(TaskDetailsIntent.DeleteClicked) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    if (showDatePicker) {
        TaskDatePicker(
            initial = state.dueDate,
            onDismiss = { showDatePicker = false },
            onConfirm = {
                showDatePicker = false
                onIntent(TaskDetailsIntent.DateChanged(it))
            },
        )
    }
    if (showTimePicker) {
        TaskTimePicker(
            initial = state.dueTime,
            onDismiss = { showTimePicker = false },
            onConfirm = {
                showTimePicker = false
                onIntent(TaskDetailsIntent.TimeChanged(it))
            },
        )
    }
    if (state.showDeleteDialog) {
        HippoConfirmDialog(
            title = "Excluir tarefa?",
            message = "A tarefa “${state.title}” será removida de forma permanente. Essa ação não pode ser desfeita.",
            confirmText = "Excluir",
            onConfirm = { onIntent(TaskDetailsIntent.DeleteConfirmed) },
            onDismiss = { onIntent(TaskDetailsIntent.DeleteDismissed) },
        )
    }
}

/** Linha "Lembrete · 30 min antes" que abre um menu com as opções. */
@Composable
private fun ReminderRow(reminder: TaskReminder, enabled: Boolean, onSelect: (TaskReminder) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val requestNotificationPermission = rememberNotificationPermissionRequest()
    Box {
        HippoPickerField(
            label = "Lembrete",
            value = if (enabled) reminder.label else "",
            placeholder = "Defina um horário para usar lembretes",
            enabled = enabled,
            onClick = { expanded = true },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            TaskReminder.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label, style = HippoTheme.type.body) },
                    onClick = {
                        expanded = false
                        onSelect(option)
                        if (option != TaskReminder.NONE) requestNotificationPermission()
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskDatePicker(initial: LocalDate?, onDismiss: () -> Unit, onConfirm: (LocalDate?) -> Unit) {
    // O DatePicker trabalha com milissegundos em UTC; convertemos de/para LocalDate.
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial?.atStartOfDayIn(TimeZone.UTC)?.toEpochMilliseconds(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val date = state.selectedDateMillis?.let {
                    Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.UTC).date
                }
                onConfirm(date)
            }) { Text("OK") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onConfirm(null) }) { Text("Remover") }
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        },
    ) {
        DatePicker(state = state)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskTimePicker(initial: LocalTime?, onDismiss: () -> Unit, onConfirm: (LocalTime?) -> Unit) {
    val state = rememberTimePickerState(
        initialHour = initial?.hour ?: 9,
        initialMinute = initial?.minute ?: 0,
        is24Hour = true,
    )
    Dialog(onDismissRequest = onDismiss) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .background(HippoColors.Surface, HippoShapes.ExtraLarge)
                .padding(20.dp),
        ) {
            Text(
                "Escolha o horário",
                style = HippoTheme.type.subtitle,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            TimePicker(state = state)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { onConfirm(null) }) { Text("Remover") }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text("Cancelar") }
                TextButton(onClick = { onConfirm(LocalTime(state.hour, state.minute)) }) { Text("OK") }
            }
        }
    }
}
