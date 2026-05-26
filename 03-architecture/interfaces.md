# Спецификация интерфейсов между слоями

Для обеспечения слабой связности и тестируемости слои взаимодействуют через интерфейсы. Это позволяет подменять реализации (например, для тестирования с использованием mock-объектов).

---

## 1. Интерфейсы уровня Mediator (бизнес-логика)

### IUserService

```java
public interface IUserService {
    User registerUser(RegistrationRequest request);
    User authenticate(String email, String password);
    User getUserById(Long id);
    List<User> getAllUsers();
    void updateUser(Long id, UpdateUserRequest request);
    void deleteUser(Long id);
}
```

### ITaskListService

```java
public interface ITaskListService {
    TaskList createTaskList(Long userId, CreateTaskListRequest request);
    TaskList getTaskListById(Long id, Long userId);
    List<TaskList> getAllTaskListsByUser(Long userId);
    TaskList updateTaskList(Long id, Long userId, UpdateTaskListRequest request);
    void deleteTaskList(Long id, Long userId);
}
```

### ITaskService

```java
public interface ITaskService {
    Task createTask(Long taskListId, Long userId, CreateTaskRequest request);
    Task getTaskById(Long id, Long userId);
    List<Task> getTasksByList(Long taskListId, Long userId);
    Task updateTask(Long id, Long userId, UpdateTaskRequest request);
    void deleteTask(Long id, Long userId);
    Task markCompleted(Long id, Long userId);
}
```

### IReminderService

```java
public interface IReminderService {
    Reminder createReminder(Long taskId, Long userId, CreateReminderRequest request);
    Reminder getReminderByTask(Long taskId, Long userId);
    void deleteReminder(Long reminderId, Long userId);
    List<Reminder> getUnsentReminders();
    void markAsSent(Long reminderId);
}
```

---

## 2. Интерфейсы уровня Foundation (репозитории)

Spring Data JPA автоматически генерирует реализации по сигнатуре методов.

### UserRepository

```java
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
}
```

### TaskListRepository

```java
public interface TaskListRepository extends JpaRepository<TaskList, Long> {
    List<TaskList> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<TaskList> findByIdAndUserId(Long id, Long userId);
    void deleteByIdAndUserId(Long id, Long userId);
}
```

### TaskRepository

```java
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByTaskListIdOrderByCreatedAtDesc(Long taskListId);
    Optional<Task> findByIdAndTaskList_UserId(Long id, Long userId);
    long countByTaskList_UserId(Long userId);
}
```

### ReminderRepository

```java
public interface ReminderRepository extends JpaRepository<Reminder, Long> {
    List<Reminder> findByIsSentFalseAndTriggerTimeBefore(LocalDateTime time);
    Optional<Reminder> findByTaskId(Long taskId);
}
```

---

## 3. Интерфейсы REST API (контракт клиент-сервер)

| Метод | Endpoint | Описание | Авторизация |
|---|---|---|---|
| POST | `/api/auth/register` | Регистрация пользователя | Нет |
| POST | `/api/auth/login` | Вход, возврат JWT | Нет |
| GET | `/api/users/me` | Профиль текущего пользователя | JWT |
| PUT | `/api/users/me` | Обновление профиля | JWT |
| GET | `/api/tasklists` | Список папок пользователя | JWT |
| POST | `/api/tasklists` | Создать папку | JWT |
| PUT | `/api/tasklists/{id}` | Обновить папку | JWT |
| DELETE | `/api/tasklists/{id}` | Удалить папку | JWT |
| GET | `/api/tasks/tasklist/{id}` | Задачи в папке | JWT |
| POST | `/api/tasks` | Создать задачу | JWT |
| PUT | `/api/tasks/{id}` | Обновить задачу | JWT |
| DELETE | `/api/tasks/{id}` | Удалить задачу | JWT |
