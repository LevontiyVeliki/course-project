package com.example.taskplanner_new_1

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import com.example.taskplanner_new_1.data.Folder
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class CreateFolderActivity : BaseActivity() {

    companion object {
        const val EXTRA_FOLDER_NAME  = "extra_folder_name"
        const val EXTRA_FOLDER_COLOR = "extra_folder_color"
    }

    private var selectedColorIndex = 1   // default: Синий

    private val chipIds = listOf(
        R.id.fc_chip_0, R.id.fc_chip_1, R.id.fc_chip_2, R.id.fc_chip_3
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_create_folder)

        // Back navigation
        val toolbar = findViewById<MaterialToolbar>(R.id.create_folder_toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        // Build colour chips
        setupColorChips()

        // Save button
        val etName  = findViewById<TextInputEditText>(R.id.et_folder_name)
        val btnSave = findViewById<MaterialButton>(R.id.btn_create_folder)
        btnSave.setOnClickListener {
            val name = etName.text?.toString()?.trim() ?: ""
            if (name.isEmpty()) {
                Toast.makeText(this, "Введите название папки", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Pass folder data back to FolderListFragment — it will insert via ViewModel
            val resultIntent = Intent().apply {
                putExtra(EXTRA_FOLDER_NAME,  name)
                putExtra(EXTRA_FOLDER_COLOR, selectedColorIndex)
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }

    private fun setupColorChips() {
        chipIds.forEachIndexed { index, viewId ->
            val chip = findViewById<android.view.View>(viewId)
            val hex  = Folder.FOLDER_COLORS.getOrElse(index) { "#2196F3" }

            chip.background = buildChipDrawable(hex, index == selectedColorIndex)
            chip.setOnClickListener {
                selectedColorIndex = index
                refreshChips()
                findViewById<TextView>(R.id.tv_selected_color_name).text =
                    Folder.COLOR_NAMES.getOrElse(index) { "" }
            }
        }

        // Set initial label
        findViewById<TextView>(R.id.tv_selected_color_name).text =
            Folder.COLOR_NAMES.getOrElse(selectedColorIndex) { "" }
    }

    private fun refreshChips() {
        chipIds.forEachIndexed { index, viewId ->
            val chip = findViewById<android.view.View>(viewId)
            val hex  = Folder.FOLDER_COLORS.getOrElse(index) { "#2196F3" }
            chip.background = buildChipDrawable(hex, index == selectedColorIndex)
        }
    }

    private fun buildChipDrawable(hex: String, selected: Boolean): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor(hex))
            if (selected) {
                setStroke(5, Color.WHITE)
            }
        }
    }
}
