package com.example.taskplanner_new_1.data

import java.io.Serializable

data class Task(
    val id: Long = 0L,
    val title: String,
    val description: String = "",
    val date: String = "",      // yyyy-MM-dd
    val time: String = "",      // HH:mm
    val isDone: Boolean = false,
    val subtasks: List<Subtask> = emptyList(),
    val serverId: Long = -1L,   // server-side TaskList id (-1 = not synced yet)
    val folderId: Long = -1L    // local folder id (-1 = no folder / legacy)
) : Serializable

data class Subtask(
    val id: Long = 0L,
    val taskId: Long,
    val title: String,
    val isDone: Boolean = false
) : Serializable
