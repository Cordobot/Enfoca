package com.cordobot.enfoca.presentation.habits

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cordobot.enfoca.domain.model.HabitProgress
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HabitsScreen(
    onAddHabit: () -> Unit,
    onEditHabit: (Long) -> Unit,
    viewModel: HabitsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { TodayHeader(state) }
            if (!state.isLoading && state.habits.isEmpty()) {
                item { EmptyHabits() }
            }
            items(state.habits, key = { it.habit.id }) { progress ->
                HabitCard(
                    progress = progress,
                    onToggle = { viewModel.onToggle(progress.habit.id) },
                    onClick = { onEditHabit(progress.habit.id) },
                    modifier = Modifier.animateItem()
                )
            }
        }
        ExtendedFloatingActionButton(
            onClick = onAddHabit,
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("Nuevo hábito") },
            containerColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }
}

@Composable
private fun TodayHeader(state: HabitsUiState) {
    val date = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale.forLanguageTag("es")))
    val progress by animateFloatAsState(state.todayProgress, label = "today")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
        Text(date.replaceFirstChar { it.uppercase() }, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Tus hábitos", style = MaterialTheme.typography.headlineLarge)
        if (state.habits.isNotEmpty()) {
            Text(
                if (state.doneToday == state.habits.size) "¡Día completo! 🎉"
                else "${state.doneToday} de ${state.habits.size} completados hoy",
                fontWeight = FontWeight.Medium
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

/** One habit with its streak and last week; the round button marks today. */
@Composable
fun HabitCard(
    progress: HabitProgress,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val habitColor = Color(progress.habit.color)
    val buttonColor by animateColorAsState(
        if (progress.doneToday) habitColor else Color.Transparent,
        label = "check"
    )
    val scale by animateFloatAsState(
        if (progress.doneToday) 1f else 0.92f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(16.dp)
            .testTag("habit_${progress.habit.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(habitColor.copy(alpha = 0.18f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(progress.habit.emoji, fontSize = 24.sp)
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(progress.habit.name, style = MaterialTheme.typography.titleMedium)
                if (progress.habit.reminder != null) {
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.Default.NotificationsActive,
                        contentDescription = "Con recordatorio",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Text(
                text = if (progress.currentStreak > 0) "🔥 ${days(progress.currentStreak)} · mejor ${progress.bestStreak}" else "Empieza tu racha hoy",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            WeekDots(progress.lastWeek, habitColor)
        }
        Box(
            modifier = Modifier
                .size(44.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(buttonColor)
                .border(2.dp, habitColor, CircleShape)
                .clickable(onClick = onToggle)
                .semantics {
                    contentDescription = if (progress.doneToday) "Desmarcar ${progress.habit.name}" else "Completar ${progress.habit.name}"
                },
            contentAlignment = Alignment.Center
        ) {
            CheckMark(visible = progress.doneToday)
        }
    }
}

// Own function so AnimatedVisibility is not resolved against the enclosing RowScope
@Composable
private fun CheckMark(visible: Boolean) {
    AnimatedVisibility(visible = visible, enter = scaleIn(), exit = scaleOut()) {
        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
    }
}

private fun days(count: Int) = if (count == 1) "1 día" else "$count días"

@Composable
private fun WeekDots(days: List<Boolean>, color: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        days.forEach { done ->
            Box(
                Modifier
                    .size(10.dp)
                    .background(if (done) color else MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            )
        }
    }
}

@Composable
private fun EmptyHabits() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🌱", fontSize = 48.sp)
        Spacer(Modifier.height(12.dp))
        Text("Crea tu primer hábito", style = MaterialTheme.typography.titleMedium)
        Text(
            "Elige una hora y Enfoca te lo recordará cada día.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
