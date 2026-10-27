package com.cordobot.enfoca.presentation.focus

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.cordobot.enfoca.domain.model.PomodoroPhase
import com.cordobot.enfoca.domain.model.PomodoroRules
import com.cordobot.enfoca.domain.model.PomodoroState
import com.cordobot.enfoca.domain.usecase.PomodoroController
import com.cordobot.enfoca.presentation.theme.Coral
import com.cordobot.enfoca.presentation.theme.Teal
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FocusUiState(val timer: PomodoroState = PomodoroState(), val now: Long = 0L) {
    val remainingMillis: Long get() = timer.remainingMillis(now)
    val progress: Float get() = timer.progress(now)
}

@HiltViewModel
class FocusViewModel @Inject constructor(private val controller: PomodoroController) : ViewModel() {

    // Clock that ticks only while the screen collects it; the real deadline lives in the state
    private val ticker = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(250)
        }
    }

    val uiState: StateFlow<FocusUiState> = combine(controller.state, ticker) { timer, now ->
        if (timer.isRunning && timer.remainingMillis(now) == 0L) {
            viewModelScope.launch { controller.finishIfDue(now) }
        }
        FocusUiState(timer, now)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FocusUiState())

    fun toggle() = viewModelScope.launch {
        val now = System.currentTimeMillis()
        if (uiState.value.timer.isRunning) controller.pause(now) else controller.start(now)
    }

    fun reset() = viewModelScope.launch { controller.reset() }

    fun skip() = viewModelScope.launch { controller.skip() }
}

@Composable
fun FocusScreen(viewModel: FocusViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val phaseColor = if (state.timer.phase == PomodoroPhase.FOCUS) Coral else Teal
    val animatedProgress by animateFloatAsState(state.progress, animationSpec = tween(250), label = "progress")

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedContent(
            targetState = state.timer.phase,
            transitionSpec = { (slideInVertically { -it } + fadeIn()) togetherWith (slideOutVertically { it } + fadeOut()) },
            label = "phase"
        ) { phase ->
            Text(phase.label, style = MaterialTheme.typography.headlineLarge, color = if (phase == PomodoroPhase.FOCUS) Coral else Teal)
        }
        Spacer(Modifier.height(32.dp))

        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(280.dp)) {
            val track = MaterialTheme.colorScheme.surfaceVariant
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 18.dp.toPx()
                val arcSize = Size(size.width - stroke, size.height - stroke)
                val topLeft = Offset(stroke / 2, stroke / 2)
                drawArc(track, -90f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
                drawArc(phaseColor, -90f, 360f * animatedProgress, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(formatClock(state.remainingMillis), fontSize = 64.sp, fontWeight = FontWeight.Black)
                Text(
                    if (state.timer.isRunning) "en curso" else "en pausa",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        SessionDots(state.timer.completedFocusSessions)
        Spacer(Modifier.height(32.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
            FilledTonalIconButton(onClick = { viewModel.reset() }, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Default.Replay, contentDescription = "Reiniciar")
            }
            FilledIconButton(
                onClick = { viewModel.toggle() },
                modifier = Modifier.size(84.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = phaseColor)
            ) {
                Icon(
                    if (state.timer.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (state.timer.isRunning) "Pausar" else "Iniciar",
                    modifier = Modifier.size(40.dp)
                )
            }
            FilledTonalIconButton(onClick = { viewModel.skip() }, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Default.SkipNext, contentDescription = "Saltar fase")
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "Te avisaremos al terminar aunque cierres la app.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun SessionDots(completed: Int) {
    val inCycle = completed % PomodoroRules.SESSIONS_BEFORE_LONG_BREAK
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(PomodoroRules.SESSIONS_BEFORE_LONG_BREAK) { index ->
                Box(
                    Modifier
                        .size(14.dp)
                        .background(if (index < inCycle) Coral else MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("$completed sesiones completadas", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
    }
}

fun formatClock(millis: Long): String {
    // Rounds up so the clock shows 25:00 at the start and 00:01 in the last second
    val totalSeconds = (millis + 999) / 1000
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
