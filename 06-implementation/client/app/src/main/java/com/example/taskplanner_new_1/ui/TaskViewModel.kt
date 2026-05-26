package com.example.taskplanner_new_1.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.taskplanner_new_1.api.RetrofitClient
import com.example.taskplanner_new_1.api.TaskListRequest
import com.example.taskplanner_new_1.api.TaskRequest
import com.example.taskplanner_new_1.data.Folder
import com.example.taskplanner_new_1.data.Task
import com.example.taskplanner_new_1.data.Subtask
import com.example.taskplanner_new_1.data.TaskDatabaseHelper
import com.example.taskplanner_new_1.notifications.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

private const val TAG = "TaskViewModel"

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val dbHelper = TaskDatabaseHelper(application)
    private val _tasks = MutableLiveData<List<Task>>()
    val tasks: LiveData<List<Task>> = _tasks

    private var currentFolderId: Long = -1L

    init {
        loadTasks()
    }

    fun loadTasks() {
        _tasks.value = dbHelper.getAllTasks()
    }

    /** Load only tasks belonging to a specific folder. */
    fun loadTasksForFolder(folderId: Long) {
        currentFolderId = folderId
        _tasks.value = dbHelper.getTasksForFolder(folderId)
    }

    private fun refreshTasks() {
        if (currentFolderId != -1L) {
            _tasks.value = dbHelper.getTasksForFolder(currentFolderId)
        } else {
            _tasks.value = dbHelper.getAllTasks()
        }
    }

    // ── Task operations ───────────────────────────────────────────────────────

    fun insertTask(task: Task): Long {
        val localId = dbHelper.insertTask(task)
        refreshTasks()
        viewModelScope.launch(Dispatchers.IO) {
            syncCreateTask(localId, task)
        }
        val triggerMillis = NotificationHelper.computeTriggerMillis(task.date, task.time)
        if (triggerMillis != null) {
            NotificationHelper.scheduleReminder(getApplication(), localId, task.title, triggerMillis)
        }
        return localId
    }

    fun updateTask(task: Task) {
        val existingServerId = dbHelper.getTask(task.id)?.serverId ?: -1L
        val taskWithServer = task.copy(serverId = existingServerId)
        dbHelper.updateTask(taskWithServer)
        refreshTasks()
        viewModelScope.launch(Dispatchers.IO) {
            syncSaveTask(taskWithServer)
        }
        NotificationHelper.cancelReminder(getApplication(), task.id)
        val triggerMillis = NotificationHelper.computeTriggerMillis(task.date, task.time)
        if (triggerMillis != null) {
            NotificationHelper.scheduleReminder(getApplication(), task.id, task.title, triggerMillis)
        }
    }

    fun deleteTask(taskId: Long) {
        val task = dbHelper.getTask(taskId)
        val serverId = task?.serverId ?: -1L
        NotificationHelper.cancelReminder(getApplication(), taskId)
        dbHelper.deleteTask(taskId)
        refreshTasks()
        viewModelScope.launch(Dispatchers.IO) {
            syncDeleteTask(serverId)
        }
    }

    fun getTaskById(taskId: Long): Task? = dbHelper.getTask(taskId)

    // ── Subtask operations ────────────────────────────────────────────────────

    fun insertSubtask(subtask: Subtask) {
        dbHelper.insertSubtask(subtask)
        refreshTasks()
    }

    fun updateSubtask(subtask: Subtask) {
        dbHelper.updateSubtask(subtask)
        refreshTasks()
    }

    fun deleteSubtask(subtaskId: Long) {
        dbHelper.deleteSubtask(subtaskId)
        refreshTasks()
    }

    // ── Private sync helpers ──────────────────────────────────────────────────

    /**
     * Creates the task on the server under the parent folder's task_list.
     * If the folder doesn't have a serverId yet (old folder / race condition),
     * it is lazily synced first so the task can be attached to it.
     */
    private suspend fun syncCreateTask(localId: Long, task: Task) {
        try {
            val folder = dbHelper.getFolder(task.folderId)
            val folderServerId = ensureFolderOnServer(folder)
            if (folderServerId <= 0) {
                Log.w(TAG, "Could not get serverId for folder ${task.folderId} — task $localId stays local")
                return
            }
            val request = buildRequest(task)
            var response = RetrofitClient.instance.createServerTask(
                taskListId = folderServerId,
                request    = request
            )
            // If server rejected (e.g. unknown priority value) — retry with HIGH as safe fallback
            if (!response.isSuccessful && response.code() == 400 && request.priority != "HIGH") {
                Log.w(TAG, "createServerTask HTTP 400 for priority=${request.priority}, retrying with HIGH")
                response = RetrofitClient.instance.createServerTask(
                    taskListId = folderServerId,
                    request    = request.copy(priority = "HIGH")
                )
            }
            if (response.isSuccessful) {
                val serverId = response.body()?.id ?: -1L
                if (serverId > 0) {
                    dbHelper.updateTaskServerId(localId, serverId)
                    Log.d(TAG, "Task $localId synced → serverId=$serverId")
                }
            } else {
                Log.e(TAG, "createServerTask failed: HTTP ${response.code()} — ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "syncCreateTask exception: ${e.message}", e)
        }
    }

    /**
     * Updates the task on the server if serverId is known.
     * If serverId is -1, attempts to create it (covers tasks created while offline).
     */
    private suspend fun syncSaveTask(task: Task) {
        try {
            val request = buildRequest(task)
            if (task.serverId > 0) {
                var response = RetrofitClient.instance.updateServerTask(task.serverId, request)
                if (!response.isSuccessful && response.code() == 400 && request.priority != "HIGH") {
                    response = RetrofitClient.instance.updateServerTask(task.serverId, request.copy(priority = "HIGH"))
                }
                if (!response.isSuccessful) {
                    Log.e(TAG, "updateServerTask failed: HTTP ${response.code()}")
                }
            } else {
                // No server id yet → try to create now
                val folder = dbHelper.getFolder(task.folderId)
                val folderServerId = ensureFolderOnServer(folder)
                if (folderServerId > 0) {
                    var response = RetrofitClient.instance.createServerTask(folderServerId, request)
                    if (!response.isSuccessful && response.code() == 400 && request.priority != "HIGH") {
                        response = RetrofitClient.instance.createServerTask(folderServerId, request.copy(priority = "HIGH"))
                    }
                    if (response.isSuccessful) {
                        val serverId = response.body()?.id ?: -1L
                        if (serverId > 0) dbHelper.updateTaskServerId(task.id, serverId)
                    } else {
                        Log.e(TAG, "create-on-update failed: HTTP ${response.code()}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "syncSaveTask exception: ${e.message}", e)
        }
    }

    /**
     * Returns the server-side task_list id for [folder].
     * If the folder has no serverId yet, POSTs it to the server first and saves
     * the returned id locally (lazy sync). Returns -1 on failure.
     */
    private suspend fun ensureFolderOnServer(folder: Folder?): Long {
        if (folder == null) return -1L
        if (folder.serverId > 0) return folder.serverId
        return try {
            val request = TaskListRequest(
                name       = folder.name,
                targetDate = LocalDate.now().toString()
            )
            val response = RetrofitClient.instance.createTaskList(request)
            if (response.isSuccessful) {
                val serverId = response.body()?.id ?: -1L
                if (serverId > 0) {
                    dbHelper.updateFolderServerId(folder.id, serverId)
                    Log.d(TAG, "Folder ${folder.id} lazily synced → serverId=$serverId")
                }
                serverId
            } else {
                Log.e(TAG, "ensureFolderOnServer failed: HTTP ${response.code()}")
                -1L
            }
        } catch (e: Exception) {
            Log.e(TAG, "ensureFolderOnServer exception: ${e.message}", e)
            -1L
        }
    }

    /** Deletes the task from the server by its server-side id. */
    private suspend fun syncDeleteTask(serverId: Long) {
        if (serverId <= 0) return
        try {
            val response = RetrofitClient.instance.deleteServerTask(serverId)
            if (response.isSuccessful) {
                Log.d(TAG, "Deleted task serverId=$serverId from server")
            } else {
                Log.e(TAG, "deleteServerTask failed: HTTP ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "syncDeleteTask exception: ${e.message}", e)
        }
    }

    /** Builds a [TaskRequest] from a local [Task], deriving priority from the folder colour. */
    private fun buildRequest(task: Task): TaskRequest {
        val folder = dbHelper.getFolder(task.folderId)
        val priority = Folder.PRIORITY_KEYS.getOrElse(folder?.colorIndex ?: 1) { "MEDIUM" }
        return TaskRequest(
            description = task.title,
            status      = if (task.isDone) "COMPLETED" else "PENDING",
            priority    = priority,
            orderIndex  = 0
        )
    }
}
