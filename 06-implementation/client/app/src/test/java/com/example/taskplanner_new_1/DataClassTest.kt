package com.example.taskplanner_new_1

import com.example.taskplanner_new_1.data.Folder
import com.example.taskplanner_new_1.data.Subtask
import com.example.taskplanner_new_1.data.Task
import org.junit.Assert.*
import org.junit.Test

/**
 * Тесты для data-классов Task, Subtask, Folder.
 * Не требуют Android-окружения — запускаются на JVM напрямую.
 */
class DataClassTest {

    // ── Task ──────────────────────────────────────────────────────────────────

    @Test
    fun task_defaultValues_areCorrect() {
        val task = Task(title = "Тест")
        assertEquals(0L, task.id)
        assertEquals("", task.description)
        assertEquals("", task.date)
        assertEquals("", task.time)
        assertFalse(task.isDone)
        assertTrue(task.subtasks.isEmpty())
        assertEquals(-1L, task.serverId)
        assertEquals(-1L, task.folderId)
    }

    @Test
    fun task_copy_changesIsDone() {
        val task = Task(title = "Задача", isDone = false)
        val done = task.copy(isDone = true)
        assertTrue(done.isDone)
        assertFalse(task.isDone) // оригинал не изменился
    }

    @Test
    fun task_copy_preservesOtherFields() {
        val task = Task(id = 5L, title = "Заголовок", folderId = 3L, isDone = false)
        val updated = task.copy(isDone = true)
        assertEquals(5L, updated.id)
        assertEquals("Заголовок", updated.title)
        assertEquals(3L, updated.folderId)
    }

    @Test
    fun task_equality_basedOnAllFields() {
        val t1 = Task(id = 1L, title = "А", folderId = 2L)
        val t2 = Task(id = 1L, title = "А", folderId = 2L)
        val t3 = Task(id = 2L, title = "А", folderId = 2L)
        assertEquals(t1, t2)
        assertNotEquals(t1, t3)
    }

    @Test
    fun task_withSubtasks_storesSubtasks() {
        val subtasks = listOf(
            Subtask(id = 1L, taskId = 10L, title = "Шаг 1"),
            Subtask(id = 2L, taskId = 10L, title = "Шаг 2")
        )
        val task = Task(id = 10L, title = "Главная", subtasks = subtasks)
        assertEquals(2, task.subtasks.size)
        assertEquals("Шаг 1", task.subtasks[0].title)
    }

    // ── Subtask ───────────────────────────────────────────────────────────────

    @Test
    fun subtask_defaultValues_areCorrect() {
        val sub = Subtask(taskId = 1L, title = "Подзадача")
        assertEquals(0L, sub.id)
        assertFalse(sub.isDone)
    }

    @Test
    fun subtask_copy_changesIsDone() {
        val sub = Subtask(id = 1L, taskId = 5L, title = "Шаг", isDone = false)
        val done = sub.copy(isDone = true)
        assertTrue(done.isDone)
        assertEquals(1L, done.id)
        assertEquals(5L, done.taskId)
    }

    @Test
    fun subtask_equality() {
        val s1 = Subtask(id = 1L, taskId = 2L, title = "Т", isDone = false)
        val s2 = Subtask(id = 1L, taskId = 2L, title = "Т", isDone = false)
        assertEquals(s1, s2)
    }

    // ── Folder ────────────────────────────────────────────────────────────────

    @Test
    fun folder_defaultValues_areCorrect() {
        val folder = Folder(name = "Работа")
        assertEquals(0L, folder.id)
        assertEquals(0, folder.colorIndex)
        assertEquals(0, folder.taskCount)
    }

    @Test
    fun folder_colorPalette_hasFourColors() {
        assertEquals(4, Folder.FOLDER_COLORS.size)
        assertEquals(4, Folder.COLOR_NAMES.size)
    }

    @Test
    fun folder_colorPalette_firstColorIsGreen() {
        assertEquals("#4CAF50", Folder.FOLDER_COLORS[0])
        assertEquals("Низкий приоритет", Folder.COLOR_NAMES[0])
    }

    @Test
    fun folder_copy_changesName() {
        val folder = Folder(id = 1L, name = "Старое", colorIndex = 2)
        val renamed = folder.copy(name = "Новое")
        assertEquals("Новое", renamed.name)
        assertEquals(2, renamed.colorIndex)
        assertEquals(1L, renamed.id)
    }

    @Test
    fun folder_equality() {
        val f1 = Folder(id = 1L, name = "А", colorIndex = 0)
        val f2 = Folder(id = 1L, name = "А", colorIndex = 0)
        assertEquals(f1, f2)
    }

    @Test
    fun folder_taskCount_isNotStoredInDb_staysZeroByDefault() {
        // taskCount вычисляется при загрузке, не хранится в БД
        val folder = Folder(id = 1L, name = "Тест", colorIndex = 0, taskCount = 0)
        assertEquals(0, folder.taskCount)
    }
}
