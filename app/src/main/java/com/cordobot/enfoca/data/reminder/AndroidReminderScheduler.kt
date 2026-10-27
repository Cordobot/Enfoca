package com.cordobot.enfoca.data.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.cordobot.enfoca.domain.model.Habit
import com.cordobot.enfoca.domain.model.PomodoroPhase
import com.cordobot.enfoca.domain.repository.ReminderScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clock: Clock
) : ReminderScheduler {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    /**
     * One unique periodic work per habit, every 24 h, starting at the next occurrence of the
     * reminder time. WorkManager keeps it across reboots and app updates.
     */
    override fun scheduleHabitReminder(habit: Habit) {
        val reminder = habit.reminder ?: return
        val request = PeriodicWorkRequestBuilder<HabitReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delayUntil(reminder).toMillis(), TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(HabitReminderWorker.KEY_HABIT_ID to habit.id))
            .addTag(TAG_HABITS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(workName(habit.id), ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    override fun cancelHabitReminder(habitId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork(workName(habitId))
    }

    /** The end of a Pomodoro must be on time, so it uses an exact alarm when allowed. */
    override fun schedulePhaseEnd(atMillis: Long, phase: PomodoroPhase) {
        val pending = phaseEndIntent(phase)
        val exactAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        if (exactAllowed) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pending)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pending)
        }
    }

    override fun cancelPhaseEnd() {
        alarmManager.cancel(phaseEndIntent(PomodoroPhase.FOCUS))
    }

    // Same request code for every phase so a new alarm replaces the previous one
    private fun phaseEndIntent(phase: PomodoroPhase): PendingIntent = PendingIntent.getBroadcast(
        context,
        PHASE_END_REQUEST,
        Intent(context, PomodoroAlarmReceiver::class.java).putExtra(PomodoroAlarmReceiver.EXTRA_PHASE, phase.name),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun delayUntil(time: LocalTime): Duration {
        val now = LocalDateTime.now(clock)
        var next = now.toLocalDate().atTime(time)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return Duration.between(now, next)
    }

    private fun workName(habitId: Long) = "habit-reminder-$habitId"

    companion object {
        const val TAG_HABITS = "habit-reminders"
        private const val PHASE_END_REQUEST = 4_100
    }
}
