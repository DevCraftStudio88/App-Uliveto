package com.example.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.FarmRepository
import com.example.data.RecurrenceType
import com.example.data.ReminderItem
import com.example.data.ReminderStatus
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object ReminderNotificationManager {

    const val CHANNEL_ID = "farm_reminders_channel"
    private const val CHANNEL_NAME = "Promemoria Aziendali"
    private const val CHANNEL_DESC = "Notifiche per promemoria e attività dell'azienda agricola"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .build()

            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                setSound(defaultSoundUri, audioAttributes)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun sendTestNotification(context: Context) {
        createNotificationChannel(context)
        val notifSettings = FarmRepository.settings.value.notificationSettings
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_reminders", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            9999,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = when (notifSettings.privacyMode) {
            "NASCONDI" -> "Promemoria Uliveto"
            else -> "Test Promemoria: Notifica di prova"
        }
        val text = when (notifSettings.privacyMode) {
            "NASCONDI" -> "Sblocca il dispositivo per visualizzare i dettagli"
            "SOLO_TITOLO" -> "Promemoria programmato"
            else -> "Notifica di test inviata con successo!"
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (!notifSettings.soundEnabled) {
            builder.setSilent(true)
        }
        if (!notifSettings.vibrationEnabled) {
            builder.setVibrate(longArrayOf(0))
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        try {
            notificationManager?.notify(9999, builder.build())
        } catch (_: SecurityException) {}
    }

    /**
     * Schedules a local system alarm for a given reminder
     */
    fun scheduleReminderAlarm(context: Context, reminder: ReminderItem) {
        if (!reminder.notificationEnabled || reminder.status != ReminderStatus.ATTIVO) {
            cancelReminderAlarm(context, reminder.id)
            return
        }

        val triggerTimeMs = calculateTriggerTimeMs(reminder.date, reminder.time, reminder.notificationOffset)
        val now = System.currentTimeMillis()

        // If the calculated time is in the past, don't schedule unless it's within last 1 minute
        if (triggerTimeMs <= now) {
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderNotificationReceiver::class.java).apply {
            putExtra("reminder_id", reminder.id)
            putExtra("title", reminder.title)
            putExtra("description", reminder.description)
            putExtra("category", reminder.category)
            putExtra("priority", reminder.priority.name)
            putExtra("zone_name", reminder.zoneName ?: "")
            putExtra("linked_type", reminder.linkedEntityType.name)
            putExtra("linked_id", reminder.linkedEntityId ?: "")
        }

        val requestCode = reminder.id.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
            }
        } catch (_: SecurityException) {
            // In case exact alarm permission was denied on Android 12+, fall back to inexact
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
        }
    }

    fun cancelReminderAlarm(context: Context, reminderId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderNotificationReceiver::class.java)
        val requestCode = reminderId.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    /**
     * Reschedules all active reminders after boot, time change, or app start
     */
    fun rescheduleAllActiveReminders(context: Context) {
        createNotificationChannel(context)
        val activeReminders = FarmRepository.reminders.value.filter {
            it.status == ReminderStatus.ATTIVO && it.notificationEnabled
        }
        for (reminder in activeReminders) {
            scheduleReminderAlarm(context, reminder)
        }
    }

    /**
     * Calculates the epoch millis for a given date (GG/MM/AAAA) and time (HH:mm)
     */
    fun calculateTriggerTimeMs(dateStr: String, timeStr: String, offsetMinutes: Int): Long {
        return try {
            val parts = dateStr.split("/")
            if (parts.size != 3) return 0L
            val day = parts[0].toInt()
            val month = parts[1].toInt() - 1
            val year = parts[2].toInt()

            val timeParts = timeStr.split(":")
            val hour = if (timeParts.isNotEmpty()) timeParts[0].toInt() else 8
            val min = if (timeParts.size > 1) timeParts[1].toInt() else 0

            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, day)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, min)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (offsetMinutes > 0) {
                cal.add(Calendar.MINUTE, -offsetMinutes)
            }

            cal.timeInMillis
        } catch (_: Exception) {
            0L
        }
    }

    /**
     * Compute next date for recurrence
     */
    fun computeNextRecurrenceDate(
        currentDateStr: String,
        recurrence: RecurrenceType,
        customInterval: Int,
        customUnit: String,
        endDateStr: String?
    ): String? {
        if (recurrence == RecurrenceType.NESSUNA) return null

        try {
            val parts = currentDateStr.split("/")
            if (parts.size != 3) return null
            val day = parts[0].toInt()
            val month = parts[1].toInt() - 1
            val year = parts[2].toInt()

            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, day)
            }

            when (recurrence) {
                RecurrenceType.OGNI_GIORNO -> cal.add(Calendar.DAY_OF_YEAR, 1)
                RecurrenceType.OGNI_SETTIMANA -> cal.add(Calendar.WEEK_OF_YEAR, 1)
                RecurrenceType.OGNI_MESE -> cal.add(Calendar.MONTH, 1)
                RecurrenceType.OGNI_ANNO -> cal.add(Calendar.YEAR, 1)
                RecurrenceType.PERSONALIZZATA -> {
                    val step = if (customInterval > 0) customInterval else 1
                    when (customUnit.uppercase(Locale.ROOT)) {
                        "GIORNI" -> cal.add(Calendar.DAY_OF_YEAR, step)
                        "SETTIMANE" -> cal.add(Calendar.WEEK_OF_YEAR, step)
                        "MESI" -> cal.add(Calendar.MONTH, step)
                        else -> cal.add(Calendar.DAY_OF_YEAR, step)
                    }
                }
                RecurrenceType.NESSUNA -> return null
            }

            val nextDateFormatted = String.format("%02d/%02d/%04d", cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR))

            // Check if past recurrenceEndDate
            if (!endDateStr.isNullOrBlank()) {
                val endParts = endDateStr.split("/")
                if (endParts.size == 3) {
                    val endCal = Calendar.getInstance().apply {
                        set(Calendar.YEAR, endParts[2].toInt())
                        set(Calendar.MONTH, endParts[1].toInt() - 1)
                        set(Calendar.DAY_OF_MONTH, endParts[0].toInt())
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                    }
                    if (cal.after(endCal)) {
                        return null // Reached end of recurrence
                    }
                }
            }

            return nextDateFormatted
        } catch (_: Exception) {
            return null
        }
    }
}
