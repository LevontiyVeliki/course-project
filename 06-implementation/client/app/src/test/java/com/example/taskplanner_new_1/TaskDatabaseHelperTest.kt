package com.example.taskplanner_new_1

import androidx.test.core.app.ApplicationProvider
import com.example.taskplanner_new_1.data.Folder
import com.example.taskplanner_new_1.data.Subtask
import com.example.taskplanner_new_1.data.Task
import com.example.taskplanner_new_1.data.TaskDatabaseHelper
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TaskDatabaseHelperTest {

    private lateinit var db: TaskDatabaseHelper

    @Before
    fun setUp() {
        db = TaskDatabaseHelper(ApplicationProvider.getApplicationContext())
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ── Task CRUD ─────────────────────────────────────────────────────────────

    @Test
    fun insertTask_returnsPositiveId() {
        val id = db.insertTask(Task(title = "Тест задачи", folderId = 1L))
        assertTrue("insertTask должен вернуть id > 0", id > 0)
    }

    @Test
    fun getTask_returnsInsertedTask() {
        val id = db.insertTask(Task(title = "Моя задача", description = "Описание", folderId = 1L))
        val task = db.getTask(id)
        assertNotNull(task)
        assertEquals("Моя задача", task!!.title)
        assertEquals("Описание", task.description)
        assertEquals(1L, task.folderId)
    }

    @Test
    fun getTask_returnsNullForMissingId() {
        assertNull(db.getTask(99999L))
    }

    @Test
    fun updateTask_changesIsDoneFlag() {
        val id = db.insertTask(Task(title = "Задача", folderId = 1L, isDone = false))
        val task = db.getTask(id)!!
        db.updateTask(task.copy(isDone = true))

        val updated = db.getTask(id)!!
        assertTrue("isDone должен быть true после обновления", updated.isDone)
    }

    @Test
    fun updateTask_changesTitle() {
        val id = db.insertTask(Task(title = "Старое название", folderId = 1L))
        val task = db.getTask(id)!!
        db.updateTask(task.copy(title = "Новое название"))

        assertEquals("Новое название", db.getTask(id)!!.title)
    }

    @Test
    fun deleteTask_removesTaskFromDb() {
        val id = db.insertTask(Task(title = "На удаление", folderId = 1L))
        db.deleteTask(id)
        assertNull(db.getTask(id))
    }

    @Test
    fun deleteTask_alsoRemovesSubtasks() {
        val taskId = db.insertTask(Task(title = "Родитель", folderId = 1L))
        db.insertSubtask(Subtask(taskId = taskId, title = "Подзадача 1"))
        db.insertSubtask(Subtask(taskId = taskId, title = "Подзадача 2"))

        db.deleteTask(taskId)

        val subtasks = db.getSubtasksForTask(taskId)
        assertTrue("Подзадачи должны быть удалены вместе с задачей", subtasks.isEmpty())
    }

    @Test
    fun updateTaskServerId_updatesCorrectly() {
        val localId = db.insertTask(Task(title = "Синхронизация", folderId = 1L))
        db.updateTaskServerId(localId, 777L)

        assertEquals(777L, db.getTask(localId)!!.serverId)
    }

    // ── Folder filter ─────────────────────────────────────────────────────────

    @Test
    fun getTasksForFolder_returnsOnlyFolderTasks() {
        val folderA = db.insertFolder(Folder(name = "Папка А", colorIndex = 0))
        val folderB = db.insertFolder(Folder(name = "Папка Б", colorIndex = 1))

        db.insertTask(Task(title = "Задача А1", folderId = folderA))
        db.insertTask(Task(title = "Задача А2", folderId = folderA))
        db.insertTask(Task(title = "Задача Б1", folderId = folderB))

        val tasksA = db.getTasksForFolder(folderA)
        assertEquals(2, tasksA.size)
        assertTrue(tasksA.all { it.folderId == folderA })
    }

    @Test
    fun getTasksForFolder_doesNotReturnOtherFolderTasks() {
        val folderA = db.insertFolder(Folder(name = "Папка А", colorIndex = 0))
        val folderB = db.insertFolder(Folder(name = "Папка Б", colorIndex = 1))

        db.insertTask(Task(title = "Задача А", folderId = folderA))
        db.insertTask(Task(title = "Задача Б", folderId = folderB))

        val tasksA = db.getTasksForFolder(folderA)
        assertTrue("В списке папки А не должно быть задач из папки Б",
            tasksA.none { it.folderId == folderB })
    }

    @Test
    fun getTasksForFolder_returnsEmptyListForEmptyFolder() {
        val folderId = db.insertFolder(Folder(name = "Пустая папка", colorIndex = 0))
        val tasks = db.getTasksForFolder(folderId)
        assertTrue(tasks.isEmpty())
    }

    @Test
    fun getAllTasks_returnsTasksFromAllFolders() {
        val folderA = db.insertFolder(Folder(name = "А", colorIndex = 0))
        val folderB = db.insertFolder(Folder(name = "Б", colorIndex = 1))

        db.insertTask(Task(title = "Задача 1", folderId = folderA))
        db.insertTask(Task(title = "Задача 2", folderId = folderB))

        // getAllTasks includes the default "Мои задачи" folder tasks too,
        // so just check both inserted tasks are present
        val all = db.getAllTasks()
        assertTrue(all.any { it.title == "Задача 1" })
        assertTrue(all.any { it.title == "Задача 2" })
    }

    // ── Folder CRUD ───────────────────────────────────────────────────────────

    @Test
    fun insertFolder_addsToAllFolders() {
        val before = db.getAllFolders().size
        db.insertFolder(Folder(name = "Новая папка", colorIndex = 2))
        assertEquals(before + 1, db.getAllFolders().size)
    }

    @Test
    fun getFolder_returnsCorrectFolder() {
        val id = db.insertFolder(Folder(name = "Работа", colorIndex = 1))
        val folder = db.getFolder(id)
        assertNotNull(folder)
        assertEquals("Работа", folder!!.name)
        assertEquals(1, folder.colorIndex)
    }

    @Test
    fun updateFolder_changesName() {
        val id = db.insertFolder(Folder(name = "Старое имя", colorIndex = 0))
        db.updateFolder(Folder(id = id, name = "Новое имя", colorIndex = 0))
        assertEquals("Новое имя", db.getFolder(id)!!.name)
    }

    @Test
    fun deleteFolder_removesFolderFromList() {
        val id = db.insertFolder(Folder(name = "На удаление", colorIndex = 0))
        db.deleteFolder(id)
        assertNull(db.getFolder(id))
    }

    @Test
    fun deleteFolder_cascadesTaskDeletion() {
        val folderId = db.insertFolder(Folder(name = "Каскад", colorIndex = 0))
        db.insertTask(Task(title = "Задача 1", folderId = folderId))
        db.insertTask(Task(title = "Задача 2", folderId = folderId))

        db.deleteFolder(folderId)

        assertTrue("Все задачи папки должны быть удалены",
            db.getTasksForFolder(folderId).isEmpty())
    }

    @Test
    fun countTasksInFolder_returnsCorrectCount() {
        val folderId = db.insertFolder(Folder(name = "Счётчик", colorIndex = 0))
        db.insertTask(Task(title = "Т1", folderId = folderId))
        db.insertTask(Task(title = "Т2", folderId = folderId))
        db.insertTask(Task(title = "Т3", folderId = folderId))

        assertEquals(3, db.countTasksInFolder(folderId))
    }

    @Test
    fun countTasksInFolder_returnsZeroForEmptyFolder() {
        val folderId = db.insertFolder(Folder(name = "Пустая", colorIndex = 0))
        assertEquals(0, db.countTasksInFolder(folderId))
    }

    // ── Subtask CRUD ──────────────────────────────────────────────────────────

    @Test
    fun insertSubtask_appearsInGetSubtasks() {
        val taskId = db.insertTask(Task(title = "Главная", folderId = 1L))
        db.insertSubtask(Subtask(taskId = taskId, title = "Подзадача"))

        val subtasks = db.getSubtasksForTask(taskId)
        assertEquals(1, subtasks.size)
        assertEquals("Подзадача", subtasks[0].title)
        assertFalse(subtasks[0].isDone)
    }

    @Test
    fun getSubtasksForTask_returnsMultipleSubtasks() {
        val taskId = db.insertTask(Task(title = "Главная", folderId = 1L))
        db.insertSubtask(Subtask(taskId = taskId, title = "Шаг 1"))
        db.insertSubtask(Subtask(taskId = taskId, title = "Шаг 2"))
        db.insertSubtask(Subtask(taskId = taskId, title = "Шаг 3"))

        assertEquals(3, db.getSubtasksForTask(taskId).size)
    }

    @Test
    fun updateSubtask_changesIsDone() {
        val taskId = db.insertTask(Task(title = "Главная", folderId = 1L))
        db.insertSubtask(Subtask(taskId = taskId, title = "Шаг", isDone = false))
        val sub = db.getSubtasksForTask(taskId).first()

        db.updateSubtask(sub.copy(isDone = true))

        assertTrue(db.getSubtasksForTask(taskId).first().isDone)
    }

    @Test
    fun deleteSubtask_removesOnlyTargetSubtask() {
        val taskId = db.insertTask(Task(title = "Главная", folderId = 1L))
        val subId = db.insertSubtask(Subtask(taskId = taskId, title = "Шаг 1"))
        db.insertSubtask(Subtask(taskId = taskId, title = "Шаг 2"))

        db.deleteSubtask(subId)

        val remaining = db.getSubtasksForTask(taskId)
        assertEquals(1, remaining.size)
        assertEquals("Шаг 2", remaining[0].title)
    }

    @Test
    fun getSubtasksForTask_returnsEmptyForTaskWithNoSubtasks() {
        val taskId = db.insertTask(Task(title = "Без подзадач", folderId = 1L))
        assertTrue(db.getSubtasksForTask(taskId).isEmpty())
    }
}
