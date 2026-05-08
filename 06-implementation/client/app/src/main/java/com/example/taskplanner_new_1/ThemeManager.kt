package com.example.taskplanner_new_1

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object ThemeManager {

    private const val PREFS       = "app_theme_prefs"
    private const val KEY_NIGHT   = "night_mode"   // 0=system, 1=light, 2=dark
    private const val KEY_COLOR   = "color_index"  // 0=purple…4=teal

    // Shown in AppearanceActivity
    val COLOR_NAMES = listOf("Фиолетовый", "Синий", "Зелёный", "Оранжевый", "Бирюзовый")
    val COLOR_HEX   = listOf("#6200EE",    "#1565C0", "#2E7D32", "#E65100",  "#00695C")

    // ── Night mode ───────────────────────────────────────────────────────────

    fun getNightMode(ctx: Context): Int =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_NIGHT, 0)

    fun setNightMode(ctx: Context, mode: Int) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY_NIGHT, mode).apply()
        applyNightMode(mode)
    }

    fun applyNightMode(mode: Int) {
        AppCompatDelegate.setDefaultNightMode(
            when (mode) {
                1    -> AppCompatDelegate.MODE_NIGHT_NO
                2    -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
    }

    // ── Accent color ─────────────────────────────────────────────────────────

    fun getColorIndex(ctx: Context): Int =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_COLOR, 0)

    fun setColorIndex(ctx: Context, index: Int) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY_COLOR, index).apply()
    }

    fun getThemeResId(colorIndex: Int): Int = when (colorIndex) {
        1    -> R.style.Theme_TaskPlannernew_1_Blue
        2    -> R.style.Theme_TaskPlannernew_1_Green
        3    -> R.style.Theme_TaskPlannernew_1_Orange
        4    -> R.style.Theme_TaskPlannernew_1_Teal
        else -> R.style.Theme_TaskPlannernew_1          // default purple
    }
}
