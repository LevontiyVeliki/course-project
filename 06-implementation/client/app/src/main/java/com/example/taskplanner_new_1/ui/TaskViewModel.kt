package com.example.taskplanner_new_1.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.taskplanner_new_1.api.RetrofitClient
import com.example.taskplanner_new_1.api.TaskListRequest
import com.example.taskplanner_new_1.data.Task
import com.example.taskplanner_new_1.data.TaskDatabaseHelper
import com.example.taskplanner_new_1.data.Subtask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

private const val TAG = "TaskViewModel"

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val dbHelper = TaskDatabaseHelper(application)
    private val _tasks = MutableLiveData<List<Task>>()
    val tasks: LiveData<List<Task>> = _tasks

    init {
        loadTasks()
    }

    fun loadTasks() {
        _tasks.value = dbHelper.getAllTasks()
    }

    /** Load only tasks belonging to a specific folder. */
    fun loadTasksForFolder(folderId: Long) {
        _tasks.value = dbHelper.getTasksForFolder(folderId)
    }

    // ── Task operations ───────────────────────────────────────────────────────

    fun insertTask(task: Task): Long {
        val localId = dbHelper.insertTask(task)
        loadTasks()
        viewModelScope.launch(Dispatchers.IO) {
            syncCreateTask(localId, task)
        }
        return localId
    }

    fun updateTask(task: Task) {
        val existingServerId = dbHelper.getTask(task.id)?.serverId ?: -1L
        val taskWithServer = task.copy(serverId = existingServerId)
        dbHelper.updateTask(taskWithServer)
        loadTasks()
        viewModelScope.launch(Dispatchers.IO) {
            syncSaveTask(taskWithServer)
        }
    }

    fun deleteTask(taskId: Long) {
        val serverId = dbHelper.getTask(taskId)?.serverId ?: -1L
        val taskTitle = dbHelper.getTask(taskId)?.title ?: ""
        dbHelper.deleteTask(taskId)
        loadTasks()
        viewModelScope.launch(Dispatchers.IO) {
            syncDeleteTask(serverId, taskTitle)
        }
    }

    fun getTaskById(taskId: Long): Task? = dbHelper.getTask(taskId)

    // ── Subtask operations ────────────────────────────────────────────────────

    fun insertSubtask(subtask: Subtask) {
        dbHelper.insertSubtask(subtask)
        loadTasks()
    }

    fun updateSubtask(subtask: Subtask) {
        dbHelper.updateSubtask(subtask)
        loadTasks()
    }

    fun deleteSubtask(subtaskId: Long) {
        dbHelper.deleteSubtask(subtaskId)
        loadTasks()
    }

    // ── Private sync helpers ──────────────────────────────────────────────────

    /** Creates the record on server; saves returned server id locally. */
    private suspend fun syncCreateTask(localId: Long, task: Task) {
        try {
            val response = RetrofitClient.instance.createTaskList(buildRequest(task))
            if (response.isSuccessful) {
                val serverId = response.body()?.id ?: -1L
                if (serverId > 0) {
                    dbHelper.updateTaskServerId(localId, serverId)
                    Log.d(TAG, "Task $localId synced to server with serverId=$serverId")
                } else {
                    Log.w(TAG, "Server returned unexpected id=$serverId for task $localId")
                }
            } else {
                Log.e(TAG, "Create failed: HTTP ${response.code()} — ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Create sync exception: ${e.message}", e)
        }
    }

    /** Updates server record if we have a serverId, otherwise creates. */
    private suspend fun syncSaveTask(task: Task) {
        try {
            val request = buildRequest(task)
            if (task.serverId > 0) {
                val response = RetrofitClient.instance.updateTaskList(task.serverId, request)
                if (!response.isSuccessful) {
                    Log.e(TAG, "Update failed: HTTP ${response.code()} — ${response.errorBody()?.string()}")
                }
            } else {
                val response = RetrofitClient.instance.createTaskList(request)
                if (response.isSuccessful) {
                    val serverId = response.body()?.id ?: -1L
                    if (serverId > 0) dbHelper.updateTaskServerId(task.id, serverId)
                } else {
                    Log.e(TAG, "Create-on-update failed: HTTP ${response.code()}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Save sync exception: ${e.message}", e)
        }
    }

    /**
     * Deletes from server.
     * If serverId is known — sends DELETE directly.
     * If serverId == -1 (task was created before fix) — fetches all server tasks,
     * finds by title, and deletes the matching one.
     */
    private suspend fun syncDeleteTask(serverId: Long, title: String) {
        try {
            if (serverId > 0) {
                // Прямое удаление по известному id
                val response = RetrofitClient.instance.deleteTaskList(serverId)
                if (response.isSuccessful) {
                    Log.d(TAG, "Deleted serverId=$serverId from server")
                } else {
                    Log.e(TAG, "Delete failed: HTTP ${response.code()} — ${response.errorBody()?.string()}")
                }
            } else {
                // serverId неизвестен — ищем по названию на сервере
                Log.w(TAG, "serverId unknown for '$title', searching on server…")
                val listResponse = RetrofitClient.instance.getTaskLists()
                if (listResponse.isSuccessful) {
                    val match = listResponse.body()?.firstOrNull { it.name == title }
                    if (match != null && match.id > 0) {
                        val delResponse = RetrofitClient.instance.deleteTaskList(match.id)
                        if (delResponse.isSuccessful) {
                            Log.d(TAG, "Deleted server task '${match.name}' id=${match.id} by name lookup")
                        } else {
                            Log.e(TAG, "Delete-by-name failed: HTTP ${delResponse.code()}")
                        }
                    } else {
                        Log.w(TAG, "No matching server task found for '$title'")
                    }
                } else {
                    Log.e(TAG, "Fetch list failed: HTTP ${listResponse.code()}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Delete sync exception: ${e.message}", e)
        }
    }

    /** Builds a [TaskListRequest] from a local [Task]. */
    private fun buildRequest(task: Task): TaskListRequest {
        val dateStr = if (task.date.isNotBlank()) {
            task.date
        } else {
            val cal = Calendar.getInstance()
            val y = cal.get(Calendar.YEAR)
            val m = String.format("%02d", cal.get(Calendar.MONTH) + 1)
            val d = String.format("%02d", cal.get(Calendar.DAY_OF_MONTH))
            "$y-$m-$d"
        }
        return TaskListRequest(
            name       = task.title,
            targetDate = dateStr,
            status     = if (task.isDone) "COMPLETED" else "ACTIVE"
        )
    }
}
