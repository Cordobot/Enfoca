package com.cordobot.enfoca.data.repository

import com.cordobot.enfoca.data.local.CompletionEntity
import com.cordobot.enfoca.data.local.HabitDao
import com.cordobot.enfoca.data.local.HabitEntity
import com.cordobot.enfoca.domain.model.Habit
import com.cordobot.enfoca.domain.model.HabitProgress
import com.cordobot.enfoca.domain.model.Streaks
import com.cordobot.enfoca.domain.repository.HabitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HabitRepositoryImpl @Inject constructor(
    private val dao: HabitDao,
    private val clock: Clock
) : HabitRepository {

    override fun observeProgress(): Flow<List<HabitProgress>> =
        combine(dao.observeHabits(), dao.observeCompletions()) { habits, completions ->
            val today = LocalDate.now(clock)
            val daysByHabit = completions.groupBy({ it.habitId }, { LocalDate.ofEpochDay(it.epochDay) })
            habits.map { entity ->
                val days = daysByHabit[entity.id].orEmpty().toSet()
                HabitProgress(
                    habit = entity.toDomain(),
                    doneToday = today in days,
                    currentStreak = Streaks.current(days, today),
                    bestStreak = Streaks.best(days),
                    lastWeek = Streaks.lastDays(days, today)
                )
            }
        }

    override suspend fun getHabit(id: Long): Habit? = dao.getHabit(id)?.toDomain()

    override suspend fun isDoneToday(id: Long): Boolean = dao.isCompleted(id, LocalDate.now(clock).toEpochDay())

    override suspend fun save(habit: Habit): Long {
        val id = dao.upsert(habit.toEntity())
        // Upsert returns -1 when it updated an existing row
        return if (id == -1L) habit.id else id
    }

    override suspend fun delete(id: Long) = dao.delete(id)

    override suspend fun toggleToday(id: Long) {
        val today = LocalDate.now(clock).toEpochDay()
        if (dao.isCompleted(id, today)) dao.deleteCompletion(id, today) else dao.insertCompletion(CompletionEntity(id, today))
    }
}

private fun HabitEntity.toDomain() = Habit(
    id = id,
    name = name,
    emoji = emoji,
    color = color,
    reminder = reminderMinuteOfDay?.let { LocalTime.of(it / 60, it % 60) },
    createdOn = LocalDate.ofEpochDay(createdOnEpochDay)
)

private fun Habit.toEntity() = HabitEntity(
    id = id,
    name = name,
    emoji = emoji,
    color = color,
    reminderMinuteOfDay = reminder?.let { it.hour * 60 + it.minute },
    createdOnEpochDay = createdOn.toEpochDay()
)
