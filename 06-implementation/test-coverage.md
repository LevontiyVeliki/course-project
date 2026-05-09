# Модульное тестирование и покрытие кода

## Инструменты

| Инструмент | Назначение |
|---|---|
| JUnit 4 | Фреймворк модульного тестирования |
| Robolectric 4.13 | JVM-среда для запуска Android-кода без эмулятора |
| JaCoCo | Измерение покрытия кода тестами |
| Mockito 5 | Создание mock-объектов для изоляции слоёв |

---

## Результаты тестирования

**Дата запуска:** 09.05.2026  
**Команда:** `./gradlew testDebugUnitTest`

| Класс тестов | Тестов | Провалено | Время |
|---|---|---|---|
| `TaskDatabaseHelperTest` | 24 | 0 | 25.3 с |
| `DataClassTest` | 14 | 0 | 0.1 с |
| `ExampleUnitTest` | 1 | 0 | 0.0 с |
| **Итого** | **39** | **0** | ✅ |

---

## Сводка покрытия (JaCoCo)

**Команда генерации отчёта:** `./gradlew jacocoTestReport`

### По слоям приложения

| Слой / Пакет | Инструкции | Строки | Методы | Ветки |
|---|---|---|---|---|
| `data` (DB, модели) | **97.3%** | **94%** | **95%** | **72%** |
| `ui` (Fragments, VM) | 0%* | 0%* | 0%* | 0%* |
| `api` (Retrofit) | 0%* | 0%* | 0%* | 0%* |
| `auth` (SessionManager) | 0%* | 0%* | 0%* | 0%* |
| **Общий итог** | **15.6%** | **15.7%** | **15.1%** | **4.8%** |

*\* Слои `ui`, `api`, `auth` требуют запущенного Android-эмулятора (instrumented-тесты) и не покрываются unit-тестами*

### По классам слоя `data`

| Класс | Инструкции | Строки | Методы |
|---|---|---|---|
| `Task` | ✅ 100% (124/124) | ✅ 100% (11/11) | ✅ 100% (11/11) |
| `Subtask` | ✅ 100% (47/47) | ✅ 100% (6/6) | ✅ 100% (6/6) |
| `Folder` | ✅ 100% (102/102) | ✅ 100% (13/13) | ✅ 100% (7/7) |
| `TaskDatabaseHelper` | ✅ 97% (917/950) | 94% (151/160) | 95% (21/22) |

**Требование методички:** покрытие > 40% по основному слою бизнес-логики — **выполнено** ✅ (слой `data` — 97%)

---

## Что покрывают тесты

### `TaskDatabaseHelperTest` (24 теста, Robolectric)

| Группа | Тесты |
|---|---|
| Task CRUD | insert, getById, getNull, update title, update isDone, delete, delete cascades subtasks, updateServerId |
| Folder filter | getTasksForFolder returns only folder tasks, excludes other folders, empty folder |
| getAllTasks | returns tasks from all folders |
| Folder CRUD | insert, getById, updateName, delete, cascade delete tasks, countTasks, countZero |
| Subtask CRUD | insert, getMultiple, updateIsDone, deleteOne, getEmpty |

### `DataClassTest` (14 тестов, JUnit 4)

| Группа | Тесты |
|---|---|
| Task | defaultValues, copy.isDone, copy preserves fields, equality |
| Task + Subtask | withSubtasks storesSubtasks |
| Subtask | defaultValues, copy.isDone, equality |
| Folder | defaultValues, colorPalette size, firstColorIsBlue, copy.name, equality, taskCount |

---

## Примеры тестов

### TaskDatabaseHelperTest — фильтрация по папке

```kotlin
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TaskDatabaseHelperTest {

    private lateinit var db: TaskDatabaseHelper

    @Before
    fun setUp() {
        db = TaskDatabaseHelper(ApplicationProvider.getApplicationContext())
    }

    @After
    fun tearDown() { db.close() }

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
    fun deleteFolder_cascadesTaskDeletion() {
        val folderId = db.insertFolder(Folder(name = "Каскад", colorIndex = 0))
        db.insertTask(Task(title = "Задача 1", folderId = folderId))
        db.insertTask(Task(title = "Задача 2", folderId = folderId))

        db.deleteFolder(folderId)

        assertTrue(db.getTasksForFolder(folderId).isEmpty())
    }

    @Test
    fun deleteTask_alsoRemovesSubtasks() {
        val taskId = db.insertTask(Task(title = "Родитель", folderId = 1L))
        db.insertSubtask(Subtask(taskId = taskId, title = "Подзадача 1"))
        db.insertSubtask(Subtask(taskId = taskId, title = "Подзадача 2"))

        db.deleteTask(taskId)

        assertTrue(db.getSubtasksForTask(taskId).isEmpty())
    }
}
```

### DataClassTest — data-классы

```kotlin
class DataClassTest {

    @Test
    fun task_copy_changesIsDone() {
        val task = Task(title = "Задача", isDone = false)
        val done = task.copy(isDone = true)
        assertTrue(done.isDone)
        assertFalse(task.isDone) // оригинал не изменился
    }

    @Test
    fun folder_colorPalette_hasFourColors() {
        assertEquals(4, Folder.FOLDER_COLORS.size)
        assertEquals("#2196F3", Folder.FOLDER_COLORS[0])
    }

    @Test
    fun subtask_copy_changesIsDone() {
        val sub = Subtask(id = 1L, taskId = 5L, title = "Шаг", isDone = false)
        val done = sub.copy(isDone = true)
        assertTrue(done.isDone)
        assertEquals(5L, done.taskId) // taskId сохранён
    }
}
```

---

## Настройка JaCoCo в проекте

### `app/build.gradle.kts`

```kotlin
plugins {
    id("jacoco")
}

android {
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    testImplementation("org.robolectric:robolectric:4.13")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
}

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
    classDirectories.setFrom(
        fileTree("${layout.buildDirectory.get()}/tmp/kotlin-classes/debug") {
            exclude("**/R.class", "**/BuildConfig.*", "**/databinding/**")
        }
    )
    sourceDirectories.setFrom(files("src/main/java"))
    executionData.setFrom(
        fileTree(layout.buildDirectory.get()) {
            include("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec",
                    "jacoco/testDebugUnitTest.exec")
        }
    )
}
```

---

## Запуск тестов

```bash
# Запустить все unit-тесты
./gradlew testDebugUnitTest

# Сгенерировать отчёт JaCoCo
./gradlew jacocoTestReport

# Отчёт будет в:
# app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml  (XML)
# app/build/reports/jacoco/html/index.html                        (HTML)
```
