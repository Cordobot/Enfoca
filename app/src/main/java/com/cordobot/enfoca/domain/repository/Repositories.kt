package com.cordobot.enfoca.domain.repository

import com.cordobot.enfoca.domain.model.Habit
import com.cordobot.enfoca.domain.model.HabitProgress
import com.cordobot.enfoca.domain.model.PomodoroPhase
import com.cordobot.enfoca.domain.model.PomodoroState
import kotlinx.coroutines.flow.Flow

interface HabitRepository {
    fun observeProgress(): Flow<List<HabitProgress>>
    suspend fun getHabit(id: Long): Habit?
    suspend fun isDoneToday(id: Long): Boolean
    suspend fun save(habit: Habit): Long
    suspend fun delete(id: Long)
    suspend fun toggleToday(id: Long)
}

interface PomodoroRepository {
    val state: Flow<PomodoroState>
    suspend fun save(state: PomodoroState)
}

/** Background work: implemented with WorkManager (habits) and AlarmManager (timer). */
interface ReminderScheduler {
    fun scheduleHabitReminder(habit: Habit)
    fun cancelHabitReminder(habitId: Long)
    fun schedulePhaseEnd(atMillis: Long, phase: PomodoroPhase)
    fun cancelPhaseEnd()
}
