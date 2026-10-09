package com.example.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.FarmRepository
import com.example.data.RecurrenceType
import com.example.data.ReminderItem
import com.example.data.ReminderPriority
import com.example.data.ReminderStatus

class ReminderNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getStringExtra("reminder_id") ?: return
        val title = intent.getStringExtra("title") ?: "Promemoria Uliveto"
        val description = intent.getStringExtra("description") ?: ""
        val category = intent.getStringExtra("category") ?: "Generale"
        val priorityStr = intent.getStringExtra("priority") ?: ReminderPriority.NORMALE.name
        val zoneName = intent.getStringExtra("zone_name") ?: ""

        val priority = try {
            ReminderPriority.valueOf(priorityStr)
        } catch (_: Exception) {
            ReminderPriority.NORMALE
        }

        val settings = FarmRepository.settings.value
        if (!settings.remindersEnabled) {
            return
        }
        val notifSettings = settings.notificationSettings

        // Notification channel check
        ReminderNotificationManager.createNotificationChannel(context)

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_reminders", true)
            putExtra("reminder_id", reminderId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            reminderId.hashCode(),
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notifPriority = when (priority) {
            ReminderPriority.URGENTE -> NotificationCompat.PRIORITY_MAX
            ReminderPriority.ALTA -> NotificationCompat.PRIORITY_HIGH
            ReminderPriority.NORMALE -> NotificationCompat.PRIORITY_DEFAULT
            ReminderPriority.BASSA -> NotificationCompat.PRIORITY_LOW
        }

        val finalTitle = when (notifSettings.privacyMode) {
            "NASCONDI" -> "Promemoria Uliveto"
            else -> title
        }

        val contentText = when (notifSettings.privacyMode) {
            "NASCONDI" -> "Sblocca il dispositivo per visualizzare i dettagli"
            "SOLO_TITOLO" -> if (zoneName.isNotBlank()) "Zona: $zoneName" else "Promemoria programmato"
            else -> buildString {
                if (zoneName.isNotBlank()) append("[$zoneName] ")
                if (description.isNotBlank()) append(description) else append("Promemoria per $category")
            }
        }

        val builder = NotificationCompat.Builder(context, ReminderNotificationManager.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(finalTitle)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(notifPriority)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (!notifSettings.soundEnabled) {
            builder.setSilent(true)
        }
        if (!notifSettings.vibrationEnabled) {
            builder.setVibrate(longArrayOf(0))
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val notificationId = reminderId.hashCode()
        try {
            notificationManager?.notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // Android 13+ permission not granted
        }

        // Handle recurring reminder
        val currentReminder = FarmRepository.reminders.value.find { it.id == reminderId }
        if (currentReminder != null && currentReminder.recurrence != RecurrenceType.NESSUNA) {
            val nextDate = ReminderNotificationManager.computeNextRecurrenceDate(
                currentDateStr = currentReminder.date,
                recurrence = currentReminder.recurrence,
                customInterval = currentReminder.customRecurrenceInterval,
                customUnit = currentReminder.customRecurrenceUnit,
                endDateStr = currentReminder.recurrenceEndDate
            )

            if (nextDate != null) {
                val updated = currentReminder.copy(
                    date = nextDate,
                    status = ReminderStatus.ATTIVO
                )
                FarmRepository.updateReminder(updated, context)
            }
        }
    }
}
