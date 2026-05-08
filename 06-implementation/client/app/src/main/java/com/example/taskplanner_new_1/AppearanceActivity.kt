package com.example.taskplanner_new_1

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButtonToggleGroup

class AppearanceActivity : BaseActivity() {

    private val chipIds = listOf(
        R.id.color_chip_0,
        R.id.color_chip_1,
        R.id.color_chip_2,
        R.id.color_chip_3,
        R.id.color_chip_4
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_appearance)

        // Toolbar back button
        findViewById<MaterialToolbar>(R.id.appearance_toolbar)
            .setNavigationOnClickListener { finish() }

        setupThemeToggle()
        setupColorChips()
    }

    // ── Night-mode toggle ─────────────────────────────────────────────────────

    private fun setupThemeToggle() {
        val group = findViewById<MaterialButtonToggleGroup>(R.id.toggle_theme)

        // Pre-select the saved mode
        val checked = when (ThemeManager.getNightMode(this)) {
            1    -> R.id.btn_theme_light
            2    -> R.id.btn_theme_dark
            else -> R.id.btn_theme_system
        }
        group.check(checked)

        group.addOnButtonCheckedListener { _, id, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val mode = when (id) {
                R.id.btn_theme_light -> 1
                R.id.btn_theme_dark  -> 2
                else                 -> 0
            }
            ThemeManager.setNightMode(this, mode)
            // Dark/light changes apply automatically via AppCompatDelegate;
            // no recreate() needed.
        }
    }

    // ── Color chips ───────────────────────────────────────────────────────────

    private fun setupColorChips() {
        val tvName = findViewById<TextView>(R.id.tv_color_name)
        val selectedIndex = ThemeManager.getColorIndex(this)

        chipIds.forEachIndexed { index, resId ->
            val chip = findViewById<View>(resId)
            applyChipDrawable(chip, index, index == selectedIndex)
            chip.setOnClickListener {
                val wasSame = ThemeManager.getColorIndex(this) == index
                if (wasSame) return@setOnClickListener

                ThemeManager.setColorIndex(this, index)
                tvName.text = ThemeManager.COLOR_NAMES[index]

                // Redraw all chips to reflect new selection
                chipIds.forEachIndexed { i, id ->
                    applyChipDrawable(findViewById(id), i, i == index)
                }
                // Recreate this activity so toolbar and buttons pick up new color
                recreate()
            }
        }

        tvName.text = ThemeManager.COLOR_NAMES[selectedIndex]
    }

    /**
     * Draws a filled circle with an optional white ring border
     * to indicate the selected color.
     */
    private fun applyChipDrawable(view: View, colorIndex: Int, selected: Boolean) {
        val color = Color.parseColor(ThemeManager.COLOR_HEX[colorIndex])
        GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
            if (selected) {
                setStroke(
                    (4 * resources.displayMetrics.density).toInt(),
                    Color.WHITE
                )
            } else {
                setStroke(0, Color.TRANSPARENT)
            }
        }.also { view.background = it }
    }

    override fun onDestroy() {
        super.onDestroy()
        supportActionBar?.show()
    }
}
