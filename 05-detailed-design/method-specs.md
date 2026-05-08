# Спецификация методов

Сигнатуры и контракты ключевых методов по каждому слою архитектуры.

---

## Control — REST-контроллеры

### AuthController

```java
/**
 * POST /api/auth/login
 * Аутентификация пользователя. Возвращает JWT-токен.
 */
@PostMapping("/login")
public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginRequest request);

/**
 * POST /api/auth/register
 * Регистрация нового пользователя.
 */
@PostMapping("/register")
public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request);
```

### TaskListController

```java
/**
 * GET /api/task-lists
 * Получение всех папок текущего пользователя.
 * @param principal — извлекается из JWT
 */
@GetMapping
public ResponseEntity<List<TaskListResponse>> getAll(Principal principal);

/**
 * POST /api/task-lists
 * Создание новой папки.
 */
@PostMapping
public ResponseEntity<TaskListResponse> create(
    @Valid @RequestBody CreateTaskListRequest request,
    Principal principal
);

/**
 * PUT /api/task-lists/{id}
 * Обновление существующей папки.
 * Возвращает 403, если папка принадлежит другому пользователю.
 */
@PutMapping("/{id}")
public ResponseEntity<TaskListResponse> update(
    @PathVariable Long id,
    @Valid @RequestBody UpdateTaskListRequest request,
    Principal principal
);

/**
 * DELETE /api/task-lists/{id}
 * Удаление папки. Каскадно удаляет все задачи через БД.
 */
@DeleteMapping("/{id}")
public ResponseEntity<Void> delete(@PathVariable Long id, Principal principal);
```

### TaskController

```java
/**
 * GET /api/tasks?listId={id}
 * Получение задач для конкретной папки.
 */
@GetMapping
public ResponseEntity<List<TaskResponse>> getByList(
    @RequestParam Long listId,
    Principal principal
);

/**
 * POST /api/tasks
 * Создание задачи в указанной папке.
 */
@PostMapping
public ResponseEntity<TaskResponse> create(
    @Valid @RequestBody CreateTaskRequest request,
    Principal principal
);

/**
 * PUT /api/tasks/{id}
 * Обновление задачи (название, описание, статус, дата/время).
 */
@PutMapping("/{id}")
public ResponseEntity<TaskResponse> update(
    @PathVariable Long id,
    @Valid @RequestBody UpdateTaskRequest request,
    Principal principal
);

/**
 * DELETE /api/tasks/{id}
 * Удаление задачи.
 */
@DeleteMapping("/{id}")
public ResponseEntity<Void> delete(@PathVariable Long id, Principal principal);
```

---

## Mediator — сервисы бизнес-логики

### UserService

```java
public interface IUserService {

    /**
     * Регистрация нового пользователя.
     * @throws EmailAlreadyExistsException если email занят
     * @throws UsernameAlreadyExistsException если логин занят
     */
    UserResponse registerUser(RegisterRequest request);

    /**
     * Аутентификация пользователя по email и паролю.
     * @throws AuthenticationException если учётные данные неверны
     * @return JWT-токен
     */
    JwtResponse authenticate(LoginRequest request);

    /**
     * Получение профиля по ID.
     * @throws EntityNotFoundException если пользователь не найден
     */
    UserResponse getUserById(Long id);

    /**
     * Обновление профиля (fullName).
     */
    UserResponse updateProfile(Long userId, UpdateProfileRequest request);
}
```

### TaskListService

```java
public interface ITaskListService {

    /**
     * Создание новой папки.
     * @param userId — владелец
     */
    TaskListResponse createTaskList(Long userId, CreateTaskListRequest request);

    /**
     * Получение всех папок пользователя, отсортированных по дате создания.
     */
    List<TaskListResponse> getAllByUser(Long userId);

    /**
     * Получение папки по ID. Проверяет принадлежность пользователю.
     * @throws AccessDeniedException если папка чужая
     */
    TaskListResponse getById(Long id, Long userId);

    /**
     * Обновление папки.
     * @throws AccessDeniedException если папка чужая
     */
    TaskListResponse update(Long id, Long userId, UpdateTaskListRequest request);

    /**
     * Удаление папки со всеми задачами.
     * @throws AccessDeniedException если папка чужая
     */
    void delete(Long id, Long userId);
}
```

### TaskService

```java
public interface ITaskService {

    /**
     * Создание задачи в папке.
     * @throws AccessDeniedException если папка не принадлежит пользователю
     */
    TaskResponse createTask(Long userId, CreateTaskRequest request);

    /**
     * Получение всех задач папки.
     */
    List<TaskResponse> getByList(Long taskListId, Long userId);

    /**
     * Обновление задачи.
     * isDone=true → status = COMPLETED
     * isDone=false + targetDate прошла → status = OVERDUE
     */
    TaskResponse updateTask(Long id, Long userId, UpdateTaskRequest request);

    /**
     * Удаление задачи.
     */
    void deleteTask(Long id, Long userId);
}
```

---

## Foundation — репозитории

```java
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
}

public interface TaskListRepository extends JpaRepository<TaskList, Long> {
    List<TaskList> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<TaskList> findByIdAndUserId(Long id, Long userId);
}

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByTaskListIdOrderByCreatedAtAsc(Long taskListId);
    Optional<Task> findByIdAndTaskList_UserId(Long id, Long userId);
    long countByTaskList_UserId(Long userId);
}
```

---

## Клиентская часть — ViewModel

```kotlin
class FolderViewModel(application: Application) : AndroidViewModel(application) {

    /** Загружает все папки из локальной БД, обновляет LiveData */
    fun loadFolders()

    /**
     * Сохраняет папку локально и синхронизирует с сервером асинхронно.
     * @return localId созданной записи
     */
    fun insertFolder(folder: Folder): Long

    /**
     * Удаляет папку локально, затем каскадно удаляет все задачи с сервера.
     * Собирает serverIds ДО локального удаления.
     */
    fun deleteFolder(folderId: Long)
}

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    /** Загружает задачи для указанной папки */
    fun loadTasksForFolder(folderId: Long)

    /**
     * Сохраняет или обновляет задачу.
     * Если task.id == -1L — создаёт новую.
     */
    fun saveTask(task: Task)

    /** Обновляет статус isDone */
    fun updateTask(task: Task)

    /** Удаляет задачу локально и на сервере */
    fun deleteTask(taskId: Long)
}
```
