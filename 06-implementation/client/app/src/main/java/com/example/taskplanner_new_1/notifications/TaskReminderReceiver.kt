package com.example.taskplanner_new_1.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.taskplanner_new_1.MainActivity
import com.example.taskplanner_new_1.R
import com.example.taskplanner_new_1.data.TaskDatabaseHelper

/**
 * Receives scheduled alarm broadcasts and shows a reminder notification.
 * Also handles BOOT_COMPLETED to reschedule all pending reminders after reboot.
 */
class TaskReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> rescheduleAll(context)
            else                         -> showNotification(context, intent)
        }
    }

    // ── Show notification ─────────────────────────────────────────────────────

    private fun showNotification(context: Context, intent: Intent) {
        val taskId    = intent.getLongExtra(NotificationHelper.EXTRA_TASK_ID, -1L)
        val taskTitle = intent.getStringExtra(NotificationHelper.EXTRA_TASK_TITLE)
            ?: context.getString(R.string.notification_title)

        // Tap opens the app (MainActivity)
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val tapPi = PendingIntent.getActivity(
            context, taskId.toInt(), tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_add_24dp)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(taskTitle)
            .setStyle(NotificationCompat.BigTextStyle().bigText(taskTitle))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(tapPi)
            .setAutoCancel(true)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(taskId.toInt(), notification)
    }

    // ── Reschedule after reboot ───────────────────────────────────────────────

    private fun rescheduleAll(context: Context) {
        val db = TaskDatabaseHelper(context)
        db.getAllTasks().forEach { task ->
            val triggerMillis = NotificationHelper.computeTriggerMillis(task.date, task.time)
            if (triggerMillis != null) {
                NotificationHelper.scheduleReminder(
                    context, task.id, task.title, triggerMillis
                )
            }
        }
    }
}
