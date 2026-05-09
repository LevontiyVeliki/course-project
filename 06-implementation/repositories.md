# Слой доступа к данным (Foundation)

Репозитории реализованы через **Spring Data JPA** — интерфейсы, расширяющие `JpaRepository`. Spring автоматически генерирует реализацию по именам методов и аннотации `@Query`.

Каждый репозиторий отвечает ровно за одну таблицу. Бизнес-логика (проверки прав, транзакции) — строго в сервисном слое.

---

## UserRepository

Отвечает за таблицу `users`. Используется `UserService` для регистрации, аутентификации и управления профилем.

```java
package com.levon.taskplanner.repository;

import com.levon.taskplanner.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /** Поиск по email — используется при аутентификации. */
    Optional<User> findByEmail(String email);

    /** Поиск по логину — используется при проверке уникальности. */
    Optional<User> findByUsername(String username);

    /** Проверка занятости email перед регистрацией. */
    boolean existsByEmail(String email);

    /** Проверка занятости логина перед регистрацией. */
    boolean existsByUsername(String username);

    /** Количество задач пользователя — для отображения в профиле. */
    @Query("""
        SELECT COUNT(t) FROM Task t
        WHERE t.taskList.user.id = :userId
    """)
    long countTasksByUserId(Long userId);
}
```

### Используемые методы

| Метод | Вызывается из | Назначение |
|---|---|---|
| `findByEmail(email)` | `UserService.authenticate()` | Найти пользователя по email для входа |
| `existsByEmail(email)` | `UserService.registerUser()` | Проверить уникальность email |
| `existsByUsername(name)` | `UserService.registerUser()` | Проверить уникальность логина |
| `findById(id)` | `UserService.getProfile()` | Загрузить профиль по id из JWT |
| `save(user)` | `UserService.registerUser()`, `updateProfile()` | Сохранить нового / обновить существующего |
| `countTasksByUserId(id)` | `UserService.getProfile()` | Статистика задач для профиля |

---

## TaskListRepository

Отвечает за таблицу `task_lists`. Все запросы фильтруют по `userId` — пользователь видит только свои папки.

```java
package com.levon.taskplanner.repository;

import com.levon.taskplanner.entity.TaskList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaskListRepository extends JpaRepository<TaskList, Long> {

    /**
     * Все папки пользователя, отсортированные по дате.
     * Основной запрос для главного экрана.
     */
    List<TaskList> findByUserIdOrderByTargetDateAsc(Long userId);

    /**
     * Папка по id — с проверкой владельца (безопасный поиск).
     * Возвращает empty, если папка чужая → сервис бросает AccessDeniedException.
     */
    Optional<TaskList> findByIdAndUserId(Long id, Long userId);

    /** Папки пользователя за конкретную дату — для фильтрации по дате. */
    List<TaskList> findByUserIdAndTargetDate(Long userId, LocalDate date);

    /**
     * Папки со статусом ACTIVE — для отображения на главном экране
     * без завершённых списков.
     */
    @Query("""
        SELECT tl FROM TaskList tl
        WHERE tl.user.id = :userId
          AND tl.status = 'ACTIVE'
        ORDER BY tl.targetDate ASC
    """)
    List<TaskList> findActiveByUserId(Long userId);

    /**
     * Загружает папки вместе с задачами (JOIN FETCH) — устраняет N+1 проблему
     * при отображении списка папок с количеством задач.
     */
    @Query("""
        SELECT DISTINCT tl FROM TaskList tl
        LEFT JOIN FETCH tl.tasks
        WHERE tl.user.id = :userId
        ORDER BY tl.targetDate ASC
    """)
    List<TaskList> findByUserIdWithTasks(Long userId);

    /** Проверка: есть ли у пользователя хоть одна папка. */
    boolean existsByUserId(Long userId);
}
```

### Используемые методы

| Метод | Вызывается из | Назначение |
|---|---|---|
| `findByUserIdOrderByTargetDateAsc(id)` | `TaskListService.getUserTaskLists()` | Главный экран — список папок |
| `findByIdAndUserId(id, userId)` | `TaskListService.getById()`, `delete()`, `update()` | Безопасное получение папки |
| `findByUserIdAndTargetDate(id, date)` | `TaskListService.getByDate()` | Фильтрация по дате |
| `findActiveByUserId(id)` | `TaskListService.getActive()` | Только активные папки |
| `findByUserIdWithTasks(id)` | `TaskListService.getUserTaskLists()` | Загрузка с задачами за 1 запрос |
| `save(taskList)` | `TaskListService.create()`, `update()` | Создание / обновление папки |
| `delete(taskList)` | `TaskListService.delete()` | Удаление (CASCADE удаляет tasks) |

---

## TaskRepository

Отвечает за таблицу `tasks`. Задачи всегда запрашиваются через принадлежность к папке и пользователю — прямой доступ по `taskId` без проверки владельца запрещён.

```java
package com.levon.taskplanner.repository;

import com.levon.taskplanner.entity.Task;
import com.levon.taskplanner.entity.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    /**
     * Задачи в папке, отсортированные по order_index.
     * Основной запрос для экрана списка задач.
     */
    List<Task> findByTaskListIdOrderByOrderIndexAsc(Long taskListId);

    /**
     * Задача по id — с проверкой владельца через JOIN.
     * Используется при обновлении / удалении.
     */
    @Query("""
        SELECT t FROM Task t
        WHERE t.id = :taskId
          AND t.taskList.user.id = :userId
    """)
    Optional<Task> findByIdAndTaskListUserId(Long taskId, Long userId);

    /**
     * Все задачи пользователя с определённым статусом.
     * Используется для фильтрации (например, показать только PENDING).
     */
    @Query("""
        SELECT t FROM Task t
        WHERE t.taskList.user.id = :userId
          AND t.status = :status
        ORDER BY t.taskList.targetDate ASC, t.orderIndex ASC
    """)
    List<Task> findByUserIdAndStatus(Long userId, TaskStatus status);

    /**
     * Количество задач в папке — для счётчика на карточке папки.
     */
    long countByTaskListId(Long taskListId);

    /**
     * Количество выполненных задач в папке — для прогресс-бара.
     */
    @Query("""
        SELECT COUNT(t) FROM Task t
        WHERE t.taskList.id = :taskListId
          AND t.status = 'COMPLETED'
    """)
    long countCompletedByTaskListId(Long taskListId);

    /**
     * Сдвиг order_index при удалении задачи — сохраняет непрерывность порядка.
     */
    @Modifying
    @Query("""
        UPDATE Task t SET t.orderIndex = t.orderIndex - 1
        WHERE t.taskList.id = :taskListId
          AND t.orderIndex > :deletedIndex
    """)
    void shiftOrderIndexAfterDelete(Long taskListId, int deletedIndex);

    /**
     * Пометить все задачи папки просроченными — вызывается планировщиком.
     */
    @Modifying
    @Query("""
        UPDATE Task t SET t.status = 'OVERDUE'
        WHERE t.taskList.id = :taskListId
          AND t.status = 'PENDING'
    """)
    void markAllOverdueInList(Long taskListId);
}
```

### Используемые методы

| Метод | Вызывается из | Назначение |
|---|---|---|
| `findByTaskListIdOrderByOrderIndexAsc(id)` | `TaskService.getTasksForList()` | Экран задач папки |
| `findByIdAndTaskListUserId(tId, uId)` | `TaskService.updateTask()`, `deleteTask()` | Безопасное получение задачи |
| `findByUserIdAndStatus(id, status)` | `TaskService.getByStatus()` | Фильтрация по статусу |
| `countByTaskListId(id)` | `TaskListService.toResponse()` | Счётчик задач в папке |
| `countCompletedByTaskListId(id)` | `TaskListService.toResponse()` | Прогресс выполнения |
| `shiftOrderIndexAfterDelete(id, idx)` | `TaskService.deleteTask()` | Поддержание порядка |
| `markAllOverdueInList(id)` | `TaskService.markOverdue()` | Обновление просроченных задач |
| `save(task)` | `TaskService.createTask()`, `updateTask()` | Создание / обновление задачи |
| `delete(task)` | `TaskService.deleteTask()` | Удаление задачи |

---

## Архитектурные правила работы с репозиториями

| Правило | Обоснование |
|---|---|
| Репозитории — только интерфейсы, без `@Component` логики | Единственная ответственность: доступ к данным |
| Все запросы фильтруют по `userId` | Изоляция данных пользователей, безопасность |
| `findByIdAndUserId` вместо `findById` | Исключает IDOR-уязвимость (доступ к чужим данным) |
| `@Modifying` + `@Transactional` на bulk-операциях | Корректная работа с JPA кэшем при UPDATE/DELETE |
| `JOIN FETCH` вместо `FetchType.EAGER` | Устранение N+1 запросов, контроль точек загрузки |
| Каскадное удаление через `ON DELETE CASCADE` в DDL | Гарантированная целостность даже при прямых SQL-операциях |

---

## Взаимодействие репозиториев и сервисов

```
TaskListController
       │
       ▼
TaskListService ──── TaskListRepository ──── PostgreSQL: task_lists
       │
       └──────────── TaskRepository    ──── PostgreSQL: tasks
                              │
                              └── (CASCADE) при delete
UserController
       │
       ▼
UserService ────────── UserRepository ──────── PostgreSQL: users
                              │
                              └── (CASCADE) при delete → task_lists → tasks
```
