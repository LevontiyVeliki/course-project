package com.example.taskplanner_new_1.data

import java.io.Serializable

data class Folder(
    val id: Long = 0L,
    val name: String,
    val colorIndex: Int = 0,       // index into FOLDER_COLORS
    val taskCount: Int = 0,        // computed on load, not stored in DB
    val serverId: Long = -1L       // server-side task_list id (-1 = not synced yet)
) : Serializable {

    companion object {
        /** Folder colours — each maps to a task priority level. */
        val FOLDER_COLORS = listOf(
            "#4CAF50", // 0 Зелёный   → LOW     (Низкий)
            "#2196F3", // 1 Синий     → MEDIUM  (Средний)
            "#F44336", // 2 Красный   → HIGH    (Высокий)
            "#9C27B0"  // 3 Фиолетовый→ URGENT  (Наивысший)
        )

        val COLOR_NAMES = listOf(
            "Низкий приоритет",
            "Средний приоритет",
            "Высокий приоритет",
            "Наивысший приоритет"
        )

        val PRIORITY_KEYS = listOf("LOW", "MEDIUM", "HIGH", "URGENT")
    }
}
