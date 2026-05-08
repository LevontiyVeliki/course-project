package com.example.taskplanner_new_1

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * Base class that applies the user's chosen color-accent theme
 * before the layout is inflated, and recreates itself when the
 * user changes the accent while another screen is open.
 */
abstract class BaseActivity : AppCompatActivity() {

    /** The color-index that was applied when this activity was created. */
    private var appliedColorIndex = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        appliedColorIndex = ThemeManager.getColorIndex(this)
        setTheme(ThemeManager.getThemeResId(appliedColorIndex))
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        // If the accent was changed while this activity was paused → recreate
        val current = ThemeManager.getColorIndex(this)
        if (appliedColorIndex >= 0 && current != appliedColorIndex) {
            recreate()
        }
    }
}
