package com.example.taskplanner_new_1.data

import java.io.Serializable

data class Folder(
    val id: Long = 0L,
    val name: String,
    val colorIndex: Int = 0,       // index into FOLDER_COLORS
    val taskCount: Int = 0         // computed on load, not stored in DB
) : Serializable {

    companion object {
        /** Preset palette for folder colours. */
        val FOLDER_COLORS = listOf(
            "#2196F3", // 0 Синий
            "#4CAF50", // 1 Зелёный
            "#FF9800", // 2 Оранжевый
            "#9C27B0"  // 3 Фиолетовый
        )

        val COLOR_NAMES = listOf(
            "Синий", "Зелёный", "Оранжевый", "Фиолетовый"
        )
    }
}
