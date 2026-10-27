package com.cordobot.enfoca.domain.usecase

import com.cordobot.enfoca.domain.model.Habit
import com.cordobot.enfoca.domain.model.PomodoroRules
import com.cordobot.enfoca.domain.model.PomodoroState
import com.cordobot.enfoca.domain.repository.HabitRepository
import com.cordobot.enfoca.domain.repository.PomodoroRepository
import com.cordobot.enfoca.domain.repository.ReminderScheduler
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class SaveHabitUseCase @Inject constructor(
    private val habits: HabitRepository,
    private val scheduler: ReminderScheduler
) {
    /** Saves the habit and (re)schedules its daily reminder. Fails when the name is blank. */
    suspend operator fun invoke(habit: Habit): Result<Long> {
        val name = habit.name.trim()
        if (name.isEmpty()) return Result.failure(IllegalArgumentException("Escribe un nombre para el hábito"))
        if (name.length > 40) return Result.failure(IllegalArgumentException("Máximo 40 caracteres"))

        val id = habits.save(habit.copy(name = name))
        val saved = habit.copy(id = id, name = name)
        if (saved.reminder != null) scheduler.scheduleHabitReminder(saved) else scheduler.cancelHabitReminder(id)
        return Result.success(id)
    }
}

class DeleteHabitUseCase @Inject constructor(
    private val habits: HabitRepository,
    private val scheduler: ReminderScheduler
) {
    suspend operator fun invoke(id: Long) {
        scheduler.cancelHabitReminder(id)
        habits.delete(id)
    }
}

class ToggleHabitUseCase @Inject constructor(private val habits: HabitRepository) {
    suspend operator fun invoke(id: Long) = habits.toggleToday(id)
}

/** Every Pomodoro action goes through here so the saved state and the alarm never disagree. */
class PomodoroController @Inject constructor(
    private val repository: PomodoroRepository,
    private val scheduler: ReminderScheduler
) {
    val state = repository.state

    suspend fun start(now: Long) = update { PomodoroRules.start(it, now) }.also { started ->
        started.endsAtMillis?.let { scheduler.schedulePhaseEnd(it, started.phase) }
    }

    suspend fun pause(now: Long) = update { PomodoroRules.pause(it, now) }.also { scheduler.cancelPhaseEnd() }

    suspend fun reset() = update { PomodoroRules.reset(it) }.also { scheduler.cancelPhaseEnd() }

    suspend fun skip() = update { PomodoroRules.advance(it) }.also { scheduler.cancelPhaseEnd() }

    /** Called when the time is up: the alarm already notified, so only the phase moves on. */
    suspend fun finishIfDue(now: Long) {
        val current = repository.state.first()
        if (current.isRunning && current.remainingMillis(now) == 0L) repository.save(PomodoroRules.advance(current))
    }

    private suspend fun update(transform: (PomodoroState) -> PomodoroState): PomodoroState {
        val next = transform(repository.state.first())
        repository.save(next)
        return next
    }
}
