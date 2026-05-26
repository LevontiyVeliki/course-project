package com.example.taskplanner_new_1.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.taskplanner_new_1.api.RetrofitClient
import com.example.taskplanner_new_1.api.TaskListRequest
import com.example.taskplanner_new_1.auth.SessionManager
import com.example.taskplanner_new_1.data.Folder
import com.example.taskplanner_new_1.data.Task
import com.example.taskplanner_new_1.data.TaskDatabaseHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

private const val TAG = "FolderViewModel"

class FolderViewModel(application: Application) : AndroidViewModel(application) {

    private val dbHelper        = TaskDatabaseHelper(application)
    private val sessionManager  = SessionManager(application)
    private val _folders = MutableLiveData<List<Folder>>()
    val folders: LiveData<List<Folder>> = _folders

    init {
        loadFolders()
        syncFromServer()
    }

    fun loadFolders() {
        _folders.value = dbHelper.getAllFolders()
    }

    /**
     * Full sync from server: pulls all folders (task_lists) and the tasks inside each.
     * - New server folders  → inserted locally
     * - Existing folders    → name updated if changed
     * - New server tasks    → inserted locally under the correct folder
     * - Existing local tasks (matched by serverId) → left as-is (optimistic local wins)
     *
     * Runs in background; reloads folder list on the main thread when done.
     */
    fun syncFromServer() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val foldersResponse = RetrofitClient.instance.getTaskLists()
                if (!foldersResponse.isSuccessful) {
                    Log.e(TAG, "syncFromServer folders: HTTP ${foldersResponse.code()}")
                    return@launch
                }
                val serverFolders = foldersResponse.body() ?: return@launch

                val allLocalFolders = dbHelper.getAllFolders()
                var foldersAdded = 0
                var tasksAdded   = 0

                serverFolders.forEach { serverFolder ->
                    // ── 1. Resolve local folder id ──────────────────────────
                    val existing = allLocalFolders.firstOrNull { it.serverId == serverFolder.id }
                    val localFolderId: Long = if (existing != null) {
                        // Update name if it changed on the server
                        if (existing.name != serverFolder.name) {
                            dbHelper.updateFolder(existing.copy(name = serverFolder.name))
                        }
                        existing.id
                    } else {
                        foldersAdded++
                        // Restore the color the user previously chose for this folder.
                        // getFolderColor returns 0 (green/low) if never saved.
                        val restoredColor = sessionManager.getFolderColor(serverFolder.id)
                        dbHelper.insertFolder(
                            Folder(
                                name       = serverFolder.name,
                                colorIndex = restoredColor,
                                serverId   = serverFolder.id
                            )
                        )
                    }

                    // ── 2. Sync tasks for this folder ────────────────────────
                    try {
                        val tasksResponse = RetrofitClient.instance.getTasksForFolder(serverFolder.id)
                        if (tasksResponse.isSuccessful) {
                            tasksResponse.body()?.forEach { serverTask ->
                                // Only insert if we don't already have this task locally
                                if (dbHelper.getTaskByServerId(serverTask.id) == null) {
                                    dbHelper.insertTask(
                                        Task(
                                            title    = serverTask.description,
                                            isDone   = serverTask.status == "COMPLETED",
                                            serverId = serverTask.id,
                                            folderId = localFolderId
                                        )
                                    )
                                    tasksAdded++
                                }
                            }
                        } else {
                            Log.w(TAG, "getTasksForFolder(${serverFolder.id}) HTTP ${tasksResponse.code()}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "syncFromServer tasks for folder ${serverFolder.id}: ${e.message}", e)
                    }
                }

                Log.d(TAG, "syncFromServer done — folders added: $foldersAdded, tasks added: $tasksAdded")
                withContext(Dispatchers.Main) { loadFolders() }

            } catch (e: Exception) {
                Log.e(TAG, "syncFromServer exception: ${e.message}", e)
            }
        }
    }

    /** Inserts a folder locally, then syncs to server to get a serverId. */
    fun insertFolder(folder: Folder): Long {
        val localId = dbHelper.insertFolder(folder)
        loadFolders()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request = TaskListRequest(
                    name       = folder.name,
                    targetDate = LocalDate.now().toString()
                )
                val response = RetrofitClient.instance.createTaskList(request)
                if (response.isSuccessful) {
                    val serverId = response.body()?.id ?: -1L
                    if (serverId > 0) {
                        dbHelper.updateFolderServerId(localId, serverId)
                        // Persist color so it survives logout/login cycle
                        sessionManager.saveFolderColor(serverId, folder.colorIndex)
                        Log.d(TAG, "Folder $localId synced → serverId=$serverId, color=${folder.colorIndex}")
                    }
                } else {
                    Log.e(TAG, "createTaskList failed: HTTP ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "insertFolder sync exception: ${e.message}", e)
            }
        }
        return localId
    }

    /** Updates folder locally and on server. */
    fun updateFolder(folder: Folder) {
        dbHelper.updateFolder(folder)
        loadFolders()
        // Persist color whenever the folder is updated (name or color may have changed)
        if (folder.serverId > 0) {
            sessionManager.saveFolderColor(folder.serverId, folder.colorIndex)
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val request = TaskListRequest(
                        name       = folder.name,
                        targetDate = LocalDate.now().toString()
                    )
                    val response = RetrofitClient.instance.updateTaskList(folder.serverId, request)
                    if (!response.isSuccessful) {
                        Log.e(TAG, "updateTaskList failed: HTTP ${response.code()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "updateFolder sync exception: ${e.message}", e)
                }
            }
        }
    }

    /**
     * Deletes folder locally (cascade removes its tasks + subtasks),
     * then deletes the corresponding task_list on the server.
     */
    fun deleteFolder(folderId: Long) {
        val folder = dbHelper.getFolder(folderId)
        val serverId = folder?.serverId ?: -1L

        dbHelper.deleteFolder(folderId)
        loadFolders()

        if (serverId > 0) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val response = RetrofitClient.instance.deleteTaskList(serverId)
                    if (response.isSuccessful) {
                        Log.d(TAG, "Deleted folder serverId=$serverId from server")
                    } else {
                        Log.e(TAG, "deleteTaskList failed: HTTP ${response.code()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "deleteFolder sync exception: ${e.message}", e)
                }
            }
        }
    }
}
