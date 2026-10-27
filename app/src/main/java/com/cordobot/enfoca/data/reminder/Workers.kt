package com.cordobot.enfoca.data.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cordobot.enfoca.domain.model.PomodoroPhase
import com.cordobot.enfoca.domain.repository.HabitRepository
import com.cordobot.enfoca.domain.usecase.PomodoroController
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Daily reminder: only notifies when the habit has not been completed today. */
@HiltWorker
class HabitReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val habits: HabitRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val id = inputData.getLong(KEY_HABIT_ID, -1)
        val habit = habits.getHabit(id) ?: return Result.success() // deleted meanwhile
        if (!habits.isDoneToday(id)) {
            Notifications.show(
                applicationContext,
                id = id.toInt(),
                channel = Notifications.CHANNEL_HABITS,
                title = "${habit.emoji} ${habit.name}",
                text = "Aún no lo marcas hoy. ¡No rompas la racha!"
            )
        }
        return Result.success()
    }

    companion object {
        const val KEY_HABIT_ID = "habit_id"
    }
}

/** Fired by AlarmManager when a Pomodoro phase ends, even if the app was closed. */
@AndroidEntryPoint
class PomodoroAlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var controller: PomodoroController

    override fun onReceive(context: Context, intent: Intent) {
        val phase = intent.getStringExtra(EXTRA_PHASE)?.let { runCatching { PomodoroPhase.valueOf(it) }.getOrNull() }
            ?: PomodoroPhase.FOCUS
        val (title, text) = when (phase) {
            PomodoroPhase.FOCUS -> "¡Sesión de enfoque completada! 🎉" to "Tómate un descanso, te lo ganaste."
            else -> "Descanso terminado" to "Hora de volver a enfocarte."
        }
        Notifications.show(context, NOTIFICATION_ID, Notifications.CHANNEL_POMODORO, title, text, openFocus = true)

        val pending = goAsync()
        scope.launch {
            try {
                controller.finishIfDue(System.currentTimeMillis())
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val EXTRA_PHASE = "phase"
        private const val NOTIFICATION_ID = 4_200
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
