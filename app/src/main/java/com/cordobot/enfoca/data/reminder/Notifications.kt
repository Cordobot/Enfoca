package com.cordobot.enfoca.data.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.cordobot.enfoca.MainActivity
import com.cordobot.enfoca.R

/** Notification channels: users can mute habit reminders and keep the timer, or the other way round. */
object Notifications {
    const val CHANNEL_HABITS = "habit_reminders"
    const val CHANNEL_POMODORO = "pomodoro"
    const val EXTRA_OPEN_FOCUS = "open_focus"

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(CHANNEL_HABITS, "Recordatorios de hábitos", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Aviso diario a la hora elegida si el hábito sigue pendiente"
                },
                NotificationChannel(CHANNEL_POMODORO, "Temporizador Pomodoro", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Aviso al terminar cada sesión de enfoque o descanso"
                    enableVibration(true)
                }
            )
        )
    }

    fun show(context: Context, id: Int, channel: String, title: String, text: String, openFocus: Boolean = false) {
        val canPost = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!canPost) return

        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(EXTRA_OPEN_FOCUS, openFocus)
        val contentIntent = PendingIntent.getActivity(
            context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setCategory(if (channel == CHANNEL_POMODORO) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER)
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
    }
}
