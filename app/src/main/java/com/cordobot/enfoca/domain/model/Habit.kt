package com.cordobot.enfoca.domain.model

import java.time.LocalDate
import java.time.LocalTime

data class Habit(
    val id: Long = 0,
    val name: String,
    val emoji: String,
    val color: Long,
    // Daily reminder time, or null for no reminder
    val reminder: LocalTime?,
    val createdOn: LocalDate
)

data class HabitProgress(
    val habit: Habit,
    val doneToday: Boolean,
    val currentStreak: Int,
    val bestStreak: Int,
    // Oldest first, ending today
    val lastWeek: List<Boolean>
)

/** Streaks computed from the days a habit was completed. Pure logic, no Android. */
object Streaks {

    /**
     * Consecutive days completed ending today, or ending yesterday when today is still pending
     * (a streak is not broken until the day is over).
     */
    fun current(days: Set<LocalDate>, today: LocalDate): Int {
        var day = if (today in days) today else today.minusDays(1)
        var streak = 0
        while (day in days) {
            streak++
            day = day.minusDays(1)
        }
        return streak
    }

    fun best(days: Set<LocalDate>): Int {
        var best = 0
        var run = 0
        var previous: LocalDate? = null
        for (day in days.sorted()) {
            run = if (previous != null && previous.plusDays(1) == day) run + 1 else 1
            best = maxOf(best, run)
            previous = day
        }
        return best
    }

    fun lastDays(days: Set<LocalDate>, today: LocalDate, count: Int = 7): List<Boolean> =
        (count - 1 downTo 0).map { today.minusDays(it.toLong()) in days }
}
