package com.example.taskplanner_new_1

import android.app.Application
import com.example.taskplanner_new_1.notifications.NotificationHelper

class TaskPlannerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Apply saved night-mode preference every time the process starts
        ThemeManager.applyNightMode(ThemeManager.getNightMode(this))
        // Create notification channel for task reminders
        NotificationHelper.createChannel(this)
    }
}
