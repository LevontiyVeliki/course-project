package com.example.taskplanner_new_1.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.content.ContentValues

class TaskDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME    = "taskplanner.db"
        const val DATABASE_VERSION = 4          // v4: added server_id to folders

        // tasks
        const val TABLE_TASKS    = "tasks"
        const val COL_ID         = "id"
        const val COL_TITLE      = "title"
        const val COL_DESCRIPTION= "description"
        const val COL_DATE       = "date"
        const val COL_TIME       = "time"
        const val COL_IS_DONE    = "is_done"
        const val COL_SERVER_ID  = "server_id"
        const val COL_FOLDER_ID  = "folder_id"   // added in v3

        // subtasks
        const val TABLE_SUBTASKS = "subtasks"
        const val COL_TASK_ID    = "task_id"

        // folders
        const val TABLE_FOLDERS        = "folders"
        const val COL_FOLDER_NAME      = "name"
        const val COL_FOLDER_COLOR     = "color_index"
        const val COL_FOLDER_SERVER_ID = "server_id"   // added in v4
    }

    override fun onCreate(db: SQLiteDatabase) {
        // tasks table (includes folder_id from the start)
        db.execSQL("""
            CREATE TABLE $TABLE_TASKS (
                $COL_ID          INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TITLE       TEXT NOT NULL,
                $COL_DESCRIPTION TEXT DEFAULT '',
                $COL_DATE        TEXT DEFAULT '',
                $COL_TIME        TEXT DEFAULT '',
                $COL_IS_DONE     INTEGER DEFAULT 0,
                $COL_SERVER_ID   INTEGER DEFAULT -1,
                $COL_FOLDER_ID   INTEGER DEFAULT -1
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE $TABLE_SUBTASKS (
                $COL_ID      INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TASK_ID INTEGER NOT NULL,
                $COL_TITLE   TEXT NOT NULL,
                $COL_IS_DONE INTEGER DEFAULT 0,
                FOREIGN KEY ($COL_TASK_ID) REFERENCES $TABLE_TASKS($COL_ID) ON DELETE CASCADE
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE $TABLE_FOLDERS (
                $COL_ID                 INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_FOLDER_NAME        TEXT NOT NULL,
                $COL_FOLDER_COLOR       INTEGER DEFAULT 0,
                $COL_FOLDER_SERVER_ID   INTEGER DEFAULT -1
            )
        """.trimIndent())

        // Default folder for fresh installs
        db.execSQL("INSERT INTO $TABLE_FOLDERS ($COL_FOLDER_NAME, $COL_FOLDER_COLOR, $COL_FOLDER_SERVER_ID) VALUES ('Мои задачи', 1, -1)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TABLE_TASKS ADD COLUMN $COL_SERVER_ID INTEGER DEFAULT -1")
        }
        if (oldVersion < 3) {
            // Create folders table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS $TABLE_FOLDERS (
                    $COL_ID           INTEGER PRIMARY KEY AUTOINCREMENT,
                    $COL_FOLDER_NAME  TEXT NOT NULL,
                    $COL_FOLDER_COLOR INTEGER DEFAULT 0
                )
            """.trimIndent())
            // Add folder_id column to tasks
            db.execSQL("ALTER TABLE $TABLE_TASKS ADD COLUMN $COL_FOLDER_ID INTEGER DEFAULT -1")
            // Insert default folder and assign all existing tasks to it
            db.execSQL("INSERT INTO $TABLE_FOLDERS ($COL_FOLDER_NAME, $COL_FOLDER_COLOR) VALUES ('Мои задачи', 1)")
            db.execSQL("UPDATE $TABLE_TASKS SET $COL_FOLDER_ID = (SELECT $COL_ID FROM $TABLE_FOLDERS LIMIT 1)")
        }
        if (oldVersion < 4) {
            // Add server_id column to folders table
            db.execSQL("ALTER TABLE $TABLE_FOLDERS ADD COLUMN $COL_FOLDER_SERVER_ID INTEGER DEFAULT -1")
        }
    }

    // ── Folder CRUD ──────────────────────────────────────────────────────────

    fun insertFolder(folder: Folder): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_FOLDER_NAME,        folder.name)
            put(COL_FOLDER_COLOR,       folder.colorIndex)
            put(COL_FOLDER_SERVER_ID,   folder.serverId)
        }
        return db.insert(TABLE_FOLDERS, null, values)
    }

    fun updateFolder(folder: Folder): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_FOLDER_NAME,        folder.name)
            put(COL_FOLDER_COLOR,       folder.colorIndex)
            put(COL_FOLDER_SERVER_ID,   folder.serverId)
        }
        return db.update(TABLE_FOLDERS, values, "$COL_ID = ?", arrayOf(folder.id.toString()))
    }

    fun updateFolderServerId(localId: Long, serverId: Long) {
        val db = writableDatabase
        val values = ContentValues().apply { put(COL_FOLDER_SERVER_ID, serverId) }
        db.update(TABLE_FOLDERS, values, "$COL_ID = ?", arrayOf(localId.toString()))
    }

    fun deleteFolder(folderId: Long) {
        val db = writableDatabase
        // Delete all tasks in this folder first (and their subtasks via cascade)
        val taskCursor = db.query(TABLE_TASKS, arrayOf(COL_ID),
            "$COL_FOLDER_ID = ?", arrayOf(folderId.toString()), null, null, null)
        while (taskCursor.moveToNext()) {
            val tid = taskCursor.getLong(taskCursor.getColumnIndexOrThrow(COL_ID))
            db.delete(TABLE_SUBTASKS, "$COL_TASK_ID = ?", arrayOf(tid.toString()))
        }
        taskCursor.close()
        db.delete(TABLE_TASKS,   "$COL_FOLDER_ID = ?", arrayOf(folderId.toString()))
        db.delete(TABLE_FOLDERS, "$COL_ID = ?",        arrayOf(folderId.toString()))
    }

    fun getFolder(folderId: Long): Folder? {
        val db = readableDatabase
        val cursor = db.query(TABLE_FOLDERS, null, "$COL_ID = ?",
            arrayOf(folderId.toString()), null, null, null)
        return if (cursor.moveToFirst()) {
            val f = cursorToFolder(cursor)
            cursor.close()
            f
        } else {
            cursor.close()
            null
        }
    }

    fun getAllFolders(): List<Folder> {
        val list = mutableListOf<Folder>()
        val db = readableDatabase
        val cursor = db.query(TABLE_FOLDERS, null, null, null, null, null, "$COL_ID ASC")
        while (cursor.moveToNext()) {
            list.add(cursorToFolder(cursor))
        }
        cursor.close()
        return list
    }

    private fun cursorToFolder(cursor: android.database.Cursor): Folder {
        val id       = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID))
        val name     = cursor.getString(cursor.getColumnIndexOrThrow(COL_FOLDER_NAME))
        val color    = cursor.getInt(cursor.getColumnIndexOrThrow(COL_FOLDER_COLOR))
        val count    = countTasksInFolder(id)
        val sidIdx   = cursor.getColumnIndex(COL_FOLDER_SERVER_ID)
        val serverId = if (sidIdx >= 0) cursor.getLong(sidIdx) else -1L
        return Folder(id, name, color, count, serverId)
    }

    fun countTasksInFolder(folderId: Long): Int {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_TASKS WHERE $COL_FOLDER_ID = ?",
            arrayOf(folderId.toString())
        )
        val count = if (cursor.moveToFirst()) cursor.getInt(0) else 0
        cursor.close()
        return count
    }

    // ── Task CRUD ────────────────────────────────────────────────────────────

    fun insertTask(task: Task): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_TITLE,       task.title)
            put(COL_DESCRIPTION, task.description)
            put(COL_DATE,        task.date)
            put(COL_TIME,        task.time)
            put(COL_IS_DONE,     if (task.isDone) 1 else 0)
            put(COL_SERVER_ID,   task.serverId)
            put(COL_FOLDER_ID,   task.folderId)
        }
        return db.insert(TABLE_TASKS, null, values)
    }

    fun updateTask(task: Task): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_TITLE,       task.title)
            put(COL_DESCRIPTION, task.description)
            put(COL_DATE,        task.date)
            put(COL_TIME,        task.time)
            put(COL_IS_DONE,     if (task.isDone) 1 else 0)
            put(COL_SERVER_ID,   task.serverId)
            put(COL_FOLDER_ID,   task.folderId)
        }
        return db.update(TABLE_TASKS, values, "$COL_ID = ?", arrayOf(task.id.toString()))
    }

    fun updateTaskServerId(localId: Long, serverId: Long) {
        val db = writableDatabase
        val values = ContentValues().apply { put(COL_SERVER_ID, serverId) }
        db.update(TABLE_TASKS, values, "$COL_ID = ?", arrayOf(localId.toString()))
    }

    fun deleteTask(taskId: Long): Int {
        val db = writableDatabase
        db.delete(TABLE_SUBTASKS, "$COL_TASK_ID = ?", arrayOf(taskId.toString()))
        return db.delete(TABLE_TASKS, "$COL_ID = ?", arrayOf(taskId.toString()))
    }

    fun getTask(taskId: Long): Task? {
        val db = readableDatabase
        val cursor = db.query(TABLE_TASKS, null, "$COL_ID = ?",
            arrayOf(taskId.toString()), null, null, null)
        return if (cursor.moveToFirst()) {
            val task = cursorToTask(cursor)
            cursor.close()
            task
        } else {
            cursor.close()
            null
        }
    }

    /** Returns ALL tasks (used by TaskViewModel when no folder filter needed). */
    fun getAllTasks(): List<Task> {
        val tasks = mutableListOf<Task>()
        val db = readableDatabase
        val cursor = db.query(TABLE_TASKS, null, null, null, null, null,
            "$COL_DATE ASC, $COL_TIME ASC")
        while (cursor.moveToNext()) tasks.add(cursorToTask(cursor))
        cursor.close()
        return tasks
    }

    /** Returns a task by its server-side id, or null if not found. */
    fun getTaskByServerId(serverId: Long): Task? {
        if (serverId <= 0) return null
        val db = readableDatabase
        val cursor = db.query(TABLE_TASKS, null, "$COL_SERVER_ID = ?",
            arrayOf(serverId.toString()), null, null, null)
        return if (cursor.moveToFirst()) {
            val task = cursorToTask(cursor)
            cursor.close()
            task
        } else {
            cursor.close()
            null
        }
    }

    /**
     * Wipes all user data from SQLite and re-creates the single default folder.
     * Call this on logout so the next user starts with a clean slate.
     */
    fun clearAllData() {
        val db = writableDatabase
        db.delete(TABLE_SUBTASKS, null, null)
        db.delete(TABLE_TASKS,    null, null)
        db.delete(TABLE_FOLDERS,  null, null)
        db.execSQL("INSERT INTO $TABLE_FOLDERS ($COL_FOLDER_NAME, $COL_FOLDER_COLOR) VALUES ('Мои задачи', 1)")
    }

    /**
     * Wipes all user data from SQLite WITHOUT creating a default folder.
     * Use before syncing from the server so fresh server data fills the DB cleanly.
     */
    fun clearForUserSwitch() {
        val db = writableDatabase
        db.delete(TABLE_SUBTASKS, null, null)
        db.delete(TABLE_TASKS,    null, null)
        db.delete(TABLE_FOLDERS,  null, null)
    }

    /** Returns tasks that belong to a specific folder. */
    fun getTasksForFolder(folderId: Long): List<Task> {
        val tasks = mutableListOf<Task>()
        val db = readableDatabase
        val cursor = db.query(TABLE_TASKS, null, "$COL_FOLDER_ID = ?",
            arrayOf(folderId.toString()), null, null, "$COL_DATE ASC, $COL_TIME ASC")
        while (cursor.moveToNext()) tasks.add(cursorToTask(cursor))
        cursor.close()
        return tasks
    }

    private fun cursorToTask(cursor: android.database.Cursor): Task {
        val id    = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID))
        val title = cursor.getString(cursor.getColumnIndexOrThrow(COL_TITLE))
        val desc  = cursor.getString(cursor.getColumnIndexOrThrow(COL_DESCRIPTION))
        val date  = cursor.getString(cursor.getColumnIndexOrThrow(COL_DATE))
        val time  = cursor.getString(cursor.getColumnIndexOrThrow(COL_TIME))
        val done  = cursor.getInt(cursor.getColumnIndexOrThrow(COL_IS_DONE)) == 1
        val sidIdx = cursor.getColumnIndex(COL_SERVER_ID)
        val serverId = if (sidIdx >= 0) cursor.getLong(sidIdx) else -1L
        val fidIdx = cursor.getColumnIndex(COL_FOLDER_ID)
        val folderId = if (fidIdx >= 0) cursor.getLong(fidIdx) else -1L
        val subtasks = getSubtasksForTask(id)
        return Task(id, title, desc, date, time, done, subtasks, serverId, folderId)
    }

    // ── Subtask CRUD ─────────────────────────────────────────────────────────

    fun insertSubtask(subtask: Subtask): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_TASK_ID, subtask.taskId)
            put(COL_TITLE,   subtask.title)
            put(COL_IS_DONE, if (subtask.isDone) 1 else 0)
        }
        return db.insert(TABLE_SUBTASKS, null, values)
    }

    fun updateSubtask(subtask: Subtask): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_TITLE,   subtask.title)
            put(COL_IS_DONE, if (subtask.isDone) 1 else 0)
        }
        return db.update(TABLE_SUBTASKS, values, "$COL_ID = ?", arrayOf(subtask.id.toString()))
    }

    fun deleteSubtask(subtaskId: Long): Int {
        val db = writableDatabase
        return db.delete(TABLE_SUBTASKS, "$COL_ID = ?", arrayOf(subtaskId.toString()))
    }

    fun getSubtasksForTask(taskId: Long): List<Subtask> {
        val subtasks = mutableListOf<Subtask>()
        val db = readableDatabase
        val cursor = db.query(TABLE_SUBTASKS, null, "$COL_TASK_ID = ?",
            arrayOf(taskId.toString()), null, null, "$COL_ID ASC")
        while (cursor.moveToNext()) {
            val id    = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID))
            val tId   = cursor.getLong(cursor.getColumnIndexOrThrow(COL_TASK_ID))
            val title = cursor.getString(cursor.getColumnIndexOrThrow(COL_TITLE))
            val done  = cursor.getInt(cursor.getColumnIndexOrThrow(COL_IS_DONE)) == 1
            subtasks.add(Subtask(id, tId, title, done))
        }
        cursor.close()
        return subtasks
    }
}
