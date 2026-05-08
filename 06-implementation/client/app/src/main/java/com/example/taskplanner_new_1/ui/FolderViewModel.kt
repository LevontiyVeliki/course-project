package com.example.taskplanner_new_1.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.taskplanner_new_1.api.RetrofitClient
import com.example.taskplanner_new_1.data.Folder
import com.example.taskplanner_new_1.data.TaskDatabaseHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "FolderViewModel"

class FolderViewModel(application: Application) : AndroidViewModel(application) {

    private val dbHelper = TaskDatabaseHelper(application)
    private val _folders = MutableLiveData<List<Folder>>()
    val folders: LiveData<List<Folder>> = _folders

    init { loadFolders() }

    fun loadFolders() {
        _folders.value = dbHelper.getAllFolders()
    }

    fun insertFolder(folder: Folder): Long {
        val id = dbHelper.insertFolder(folder)
        loadFolders()
        return id
    }

    fun updateFolder(folder: Folder) {
        dbHelper.updateFolder(folder)
        loadFolders()
    }

    /**
     * Удаляет папку локально и синхронно удаляет все её задачи с сервера.
     * Сначала собираем serverIds, потом удаляем локально, потом чистим сервер.
     */
    fun deleteFolder(folderId: Long) {
        // 1. Читаем задачи ДО удаления, чтобы знать serverIds
        val tasksInFolder = dbHelper.getTasksForFolder(folderId)

        // 2. Удаляем локально (папку + задачи + подзадачи)
        dbHelper.deleteFolder(folderId)
        loadFolders()

        // 3. Удаляем каждую задачу с сервера в фоне
        val serverIds = tasksInFolder.map { it.serverId }.filter { it > 0 }
        if (serverIds.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            serverIds.forEach { serverId ->
                try {
                    val response = RetrofitClient.instance.deleteTaskList(serverId)
                    if (response.isSuccessful) {
                        Log.d(TAG, "Deleted task serverId=$serverId from server")
                    } else {
                        Log.e(TAG, "Failed to delete serverId=$serverId: HTTP ${response.code()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Server delete exception for serverId=$serverId: ${e.message}")
                }
            }
        }
    }
}
