package com.example.taskplanner_new_1.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object NotificationHelper {

    const val CHANNEL_ID = "task_reminders"
    const val EXTRA_TASK_ID = "task_id"
    const val EXTRA_TASK_TITLE = "task_title"

    /** Creates the notification channel (call once from Application.onCreate). */
    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(com.example.taskplanner_new_1.R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(
                    com.example.taskplanner_new_1.R.string.notification_channel_desc
                )
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    /**
     * Schedules an exact alarm that fires [triggerMillis] ms from epoch.
     * Does nothing if [triggerMillis] is null or in the past.
     */
    fun scheduleReminder(
        context: Context,
        taskId: Long,
        title: String,
        triggerMillis: Long
    ) {
        if (triggerMillis <= System.currentTimeMillis()) return

        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = buildPendingIntent(context, taskId, title)

        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                if (am.canScheduleExactAlarms()) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pi)
                } else {
                    // Exact alarms not permitted — use inexact (fires roughly on time)
                    am.set(AlarmManager.RTC_WAKEUP, triggerMillis, pi)
                }
            }
            else -> am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pi)
        }
    }

    /** Cancels a previously scheduled reminder for [taskId]. */
    fun cancelReminder(context: Context, taskId: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            Intent(context, TaskReminderReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        am.cancel(pi)
        pi.cancel()
    }

    /**
     * Converts date ("yyyy-MM-dd") + time ("HH:mm") strings to epoch millis.
     * If time is blank, defaults to 09:00.
     * Returns null if date is blank or the computed time is already in the past.
     */
    fun computeTriggerMillis(date: String, time: String): Long? {
        if (date.isBlank()) return null
        val parts = date.split("-")
        if (parts.size != 3) return null
        return try {
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR,        parts[0].toInt())
                set(Calendar.MONTH,       parts[1].toInt() - 1)
                set(Calendar.DAY_OF_MONTH, parts[2].toInt())
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (time.isNotBlank()) {
                    val tp = time.split(":")
                    set(Calendar.HOUR_OF_DAY, tp[0].toInt())
                    set(Calendar.MINUTE,       tp[1].toInt())
                } else {
                    set(Calendar.HOUR_OF_DAY, 9)
                    set(Calendar.MINUTE, 0)
                }
            }
            if (cal.timeInMillis > System.currentTimeMillis()) cal.timeInMillis else null
        } catch (_: Exception) {
            null
        }
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private fun buildPendingIntent(
        context: Context,
        taskId: Long,
        title: String
    ): PendingIntent {
        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TASK_TITLE, title)
        }
        return PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
