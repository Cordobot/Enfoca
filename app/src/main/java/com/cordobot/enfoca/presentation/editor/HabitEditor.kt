package com.cordobot.enfoca.presentation.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.cordobot.enfoca.domain.model.Habit
import com.cordobot.enfoca.domain.repository.HabitRepository
import com.cordobot.enfoca.domain.usecase.DeleteHabitUseCase
import com.cordobot.enfoca.domain.usecase.SaveHabitUseCase
import com.cordobot.enfoca.presentation.theme.HabitColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

val HabitEmojis = listOf("💧", "📚", "🏃", "🧘", "🥗", "😴", "✍️", "🎸", "💊", "🌱", "🧹", "💻")

data class EditorState(
    val id: Long = 0,
    val name: String = "",
    val emoji: String = HabitEmojis.first(),
    val color: Long = HabitColors.first(),
    val reminder: LocalTime? = LocalTime.of(20, 0),
    val createdOn: LocalDate = LocalDate.now(),
    val error: String? = null,
    val saved: Boolean = false
) {
    val isNew: Boolean get() = id == 0L
}

@HiltViewModel
class HabitEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val habits: HabitRepository,
    private val saveHabit: SaveHabitUseCase,
    private val deleteHabit: DeleteHabitUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state.asStateFlow()

    init {
        val id = savedStateHandle.get<Long>("id") ?: 0L
        if (id != 0L) {
            viewModelScope.launch {
                habits.getHabit(id)?.let { habit ->
                    _state.value = EditorState(habit.id, habit.name, habit.emoji, habit.color, habit.reminder, habit.createdOn)
                }
            }
        }
    }

    fun onNameChange(value: String) = _state.update { it.copy(name = value, error = null) }
    fun onEmojiChange(value: String) = _state.update { it.copy(emoji = value) }
    fun onColorChange(value: Long) = _state.update { it.copy(color = value) }
    fun onReminderChange(value: LocalTime?) = _state.update { it.copy(reminder = value) }

    fun save() {
        val current = _state.value
        viewModelScope.launch {
            saveHabit(Habit(current.id, current.name, current.emoji, current.color, current.reminder, current.createdOn))
                .onSuccess { _state.update { it.copy(saved = true) } }
                .onFailure { error -> _state.update { it.copy(error = error.message) } }
        }
    }

    fun delete() {
        viewModelScope.launch {
            deleteHabit(_state.value.id)
            _state.update { it.copy(saved = true) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HabitEditorScreen(onDone: () -> Unit, viewModel: HabitEditorViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var pickingTime by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(state.saved) { if (state.saved) onDone() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDone) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
            Text(
                if (state.isNew) "Nuevo hábito" else "Editar hábito",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 22.sp),
                modifier = Modifier.weight(1f)
            )
            if (!state.isNew) {
                IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Default.Delete, contentDescription = "Eliminar") }
            }
        }

        OutlinedTextField(
            value = state.name,
            onValueChange = viewModel::onNameChange,
            label = { Text("Nombre") },
            placeholder = { Text("Ej. Beber 2 litros de agua") },
            isError = state.error != null,
            supportingText = state.error?.let { { Text(it) } },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Text("Ícono", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            HabitEmojis.forEach { emoji ->
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (emoji == state.emoji) Color(state.color).copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface,
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { viewModel.onEmojiChange(emoji) },
                    contentAlignment = Alignment.Center
                ) { Text(emoji, fontSize = 24.sp) }
            }
        }

        Text("Color", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HabitColors.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(color), CircleShape)
                        .border(3.dp, if (color == state.color) Color.White else Color.Transparent, CircleShape)
                        .clickable { viewModel.onColorChange(color) }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Recordatorio diario", style = MaterialTheme.typography.titleMedium)
                Text(
                    state.reminder?.let { "A las %02d:%02d si aún no lo completas".format(it.hour, it.minute) } ?: "Sin recordatorio",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable(enabled = state.reminder != null) { pickingTime = true }
                )
            }
            Switch(
                checked = state.reminder != null,
                onCheckedChange = { enabled -> viewModel.onReminderChange(if (enabled) LocalTime.of(20, 0) else null) }
            )
        }
        if (state.reminder != null) {
            TextButton(onClick = { pickingTime = true }) { Text("Cambiar hora") }
        }

        Spacer(Modifier.height(8.dp))
        Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text(if (state.isNew) "Crear hábito" else "Guardar cambios")
        }
    }

    if (pickingTime) {
        val initial = state.reminder ?: LocalTime.of(20, 0)
        val timeState = rememberTimePickerState(initial.hour, initial.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { pickingTime = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onReminderChange(LocalTime.of(timeState.hour, timeState.minute))
                    pickingTime = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { pickingTime = false }) { Text("Cancelar") } },
            text = { TimePicker(state = timeState) }
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("¿Eliminar \"${state.name}\"?") },
            text = { Text("Se borrará su historial y su recordatorio.") },
            confirmButton = { TextButton(onClick = viewModel::delete) { Text("Eliminar") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") } }
        )
    }
}
