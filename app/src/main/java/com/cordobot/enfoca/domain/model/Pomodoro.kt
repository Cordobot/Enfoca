package com.cordobot.enfoca.domain.model

enum class PomodoroPhase(val label: String, val minutes: Int) {
    FOCUS("Enfoque", 25),
    SHORT_BREAK("Descanso corto", 5),
    LONG_BREAK("Descanso largo", 15);

    val durationMillis: Long get() = minutes * 60_000L
}

/**
 * Timer state. While running, time left is derived from [endsAtMillis] (wall clock) instead of
 * counting ticks, so it stays right after the app is killed or the screen is off.
 */
data class PomodoroState(
    val phase: PomodoroPhase = PomodoroPhase.FOCUS,
    val endsAtMillis: Long? = null,
    val pausedRemainingMillis: Long = PomodoroPhase.FOCUS.durationMillis,
    val completedFocusSessions: Int = 0
) {
    val isRunning: Boolean get() = endsAtMillis != null

    fun remainingMillis(now: Long): Long =
        endsAtMillis?.let { (it - now).coerceAtLeast(0) } ?: pausedRemainingMillis

    fun progress(now: Long): Float = 1f - remainingMillis(now).toFloat() / phase.durationMillis
}

/** Classic Pomodoro rules: a long break after every fourth focus session. */
object PomodoroRules {
    const val SESSIONS_BEFORE_LONG_BREAK = 4

    fun start(state: PomodoroState, now: Long) =
        if (state.isRunning) state else state.copy(endsAtMillis = now + state.pausedRemainingMillis)

    fun pause(state: PomodoroState, now: Long) =
        if (!state.isRunning) state else state.copy(endsAtMillis = null, pausedRemainingMillis = state.remainingMillis(now))

    fun reset(state: PomodoroState) =
        state.copy(endsAtMillis = null, pausedRemainingMillis = state.phase.durationMillis)

    /** Moves to the next phase (stopped), counting the focus session that just ended. */
    fun advance(state: PomodoroState): PomodoroState {
        val completed = state.completedFocusSessions + if (state.phase == PomodoroPhase.FOCUS) 1 else 0
        val next = when {
            state.phase != PomodoroPhase.FOCUS -> PomodoroPhase.FOCUS
            completed % SESSIONS_BEFORE_LONG_BREAK == 0 -> PomodoroPhase.LONG_BREAK
            else -> PomodoroPhase.SHORT_BREAK
        }
        return PomodoroState(next, null, next.durationMillis, completed)
    }
}
