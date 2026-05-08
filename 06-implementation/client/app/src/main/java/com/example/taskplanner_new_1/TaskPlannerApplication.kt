package com.example.taskplanner_new_1

import android.app.Application

class TaskPlannerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Apply saved night-mode preference every time the process starts
        ThemeManager.applyNightMode(ThemeManager.getNightMode(this))
    }
}
