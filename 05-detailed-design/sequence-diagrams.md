# Диаграммы последовательности

Описывают динамику взаимодействия компонентов системы при выполнении ключевых сценариев.

---

## SD-01: Аутентификация пользователя (UC2)

**Участники:** `LoginFragment` → `AuthViewModel` → `RetrofitClient` → `AuthController` → `UserService` → `UserRepository`

### Последовательность

```
LoginFragment        AuthViewModel          RetrofitClient       AuthController     UserService         UserRepository
     |                    |                      |                    |                  |                    |
     | login(email,pwd)   |                      |                    |                  |                    |
     |------------------->|                      |                    |                  |                    |
     |                    | POST /api/auth/login  |                    |                  |                    |
     |                    |---------------------->|                    |                  |                    |
     |                    |                      | login(req)         |                  |                    |
     |                    |                      |------------------->|                  |                    |
     |                    |                      |                    | authenticate()   |                    |
     |                    |                      |                    |----------------->|                    |
     |                    |                      |                    |                  | findByEmail()      |
     |                    |                      |                    |                  |------------------->|
     |                    |                      |                    |                  |<-- User            |
     |                    |                      |                    |                  | checkPassword()    |
     |                    |                      |                    |                  | generateJwt()      |
     |                    |                      |                    |<-- JwtResponse   |                    |
     |                    |                      |<-- 200 + token     |                  |                    |
     |                    |<-- token             |                    |                  |                    |
     |                    | saveToken()          |                    |                  |                    |
     |<-- navigateToMain()|                      |                    |                  |                    |
```

### Описание шагов

| № | Участник | Действие |
|---|---|---|
| 1 | `LoginFragment` | Пользователь вводит email и пароль, нажимает «Войти» |
| 2 | `AuthViewModel` | Вызывает `login(email, password)`, запускает корутину |
| 3 | `RetrofitClient` | Формирует POST-запрос к `/api/auth/login` с телом `{email, password}` |
| 4 | `AuthController` | Принимает запрос, создаёт `LoginRequest`, вызывает `userService.authenticate()` |
| 5 | `UserService` | Ищет пользователя по email через `UserRepository.findByEmail()` |
| 6 | `UserRepository` | Выполняет SELECT по email в PostgreSQL, возвращает `User` |
| 7 | `UserService` | Сравнивает BCrypt-хеш пароля, генерирует JWT-токен |
| 8 | `AuthController` | Возвращает `JwtResponse{token}` с HTTP 200 |
| 9 | `AuthViewModel` | Сохраняет токен в `SharedPreferences` через `SessionManager` |
| 10 | `LoginFragment` | Переходит на главный экран (`FolderListFragment`) |

### Альтернативный поток: неверный пароль

- Шаг 7: BCrypt-сравнение возвращает `false`
- `UserService` выбрасывает `AuthenticationException`
- `AuthController` возвращает HTTP 401
- `AuthViewModel` устанавливает `authError.value = "Неверный логин или пароль"`
- `LoginFragment` отображает сообщение об ошибке

---

## SD-02: Создание списка задач (UC3)

**Участники:** `CreateFolderActivity` → `FolderViewModel` → `TaskDatabaseHelper` / `RetrofitClient` → `TaskListController` → `TaskListService` → `TaskListRepository`

### Последовательность

```
CreateFolderActivity  FolderViewModel    TaskDatabaseHelper   RetrofitClient     TaskListController  TaskListService
        |                   |                   |                  |                    |                  |
        | saveFolder(data)  |                   |                  |                    |                  |
        |------------------>|                   |                  |                    |                  |
        |                   | insertFolder()    |                  |                    |                  |
        |                   |------------------>|                  |                    |                  |
        |                   |<-- localId        |                  |                    |                  |
        |                   | POST /api/task-lists                 |                    |                  |
        |                   |---------------------------------->   |                    |                  |
        |                   |                                      | create(req)        |                  |
        |                   |                                      |------------------->|                  |
        |                   |                                      |                    | createTaskList() |
        |                   |                                      |                    |----------------->|
        |                   |                                      |                    |<-- TaskList(id)  |
        |                   |                                      |<-- 201 + TaskList  |                  |
        |                   | updateServerId(localId, serverId)    |                    |                  |
        |                   |------------------>|                  |                    |                  |
        |<-- finish()       |                   |                  |                    |                  |
```

### Описание шагов

| № | Участник | Действие |
|---|---|---|
| 1 | `CreateFolderActivity` | Пользователь вводит название, выбирает цвет, нажимает «Сохранить» |
| 2 | `FolderViewModel` | Вызывает `insertFolder(folder)` |
| 3 | `TaskDatabaseHelper` | Записывает папку в локальный SQLite, возвращает `localId` |
| 4 | `RetrofitClient` | Отправляет POST `/api/task-lists` с данными папки и JWT-токеном |
| 5 | `TaskListController` | Извлекает `userId` из токена, вызывает `taskListService.create()` |
| 6 | `TaskListService` | Создаёт объект `TaskList`, сохраняет через репозиторий |
| 7 | `TaskListController` | Возвращает HTTP 201 с `TaskListResponse{id, name, ...}` |
| 8 | `FolderViewModel` | Обновляет `serverId` у локальной записи |
| 9 | `CreateFolderActivity` | Завершает активность, возвращается к списку папок |

---

## SD-03: Сохранение задачи (UC6)

**Участники:** `TaskEditFragment` → `TaskViewModel` → `TaskDatabaseHelper` / `RetrofitClient` → `TaskController` → `TaskService` → `TaskRepository`

### Последовательность

```
TaskEditFragment   TaskViewModel    TaskDatabaseHelper    RetrofitClient      TaskController     TaskService
       |                |                  |                   |                   |                 |
       | saveTask(data) |                  |                   |                   |                 |
       |--------------->|                  |                   |                   |                 |
       |                | insertOrUpdate() |                   |                   |                 |
       |                |----------------->|                   |                   |                 |
       |                |<-- taskId        |                   |                   |                 |
       |                | POST/PUT /api/tasks                  |                   |                 |
       |                |--------------------------------->    |                   |                 |
       |                |                                      | save(req)         |                 |
       |                |                                      |------------------>|                 |
       |                |                                      |                   | createTask()    |
       |                |                                      |                   |---------------->|
       |                |                                      |                   |<-- Task         |
       |                |                                      |<-- 201 + Task     |                 |
       |                | updateServerId(taskId, serverId)      |                   |                 |
       |                |----------------->|                   |                   |                 |
       |<-- popBackStack|                  |                   |                   |                 |
```

---

## SD-04: Удаление папки с каскадным удалением на сервере

**Участники:** `FolderListFragment` → `FolderViewModel` → `TaskDatabaseHelper` / `RetrofitClient` → `TaskListController`

### Последовательность

| № | Действие |
|---|---|
| 1 | Пользователь нажимает кнопку удаления, подтверждает в `AlertDialog` |
| 2 | `FolderViewModel.deleteFolder(folderId)` |
| 3 | `TaskDatabaseHelper.getTasksForFolder(folderId)` — собираем `serverIds` ДО удаления |
| 4 | `TaskDatabaseHelper.deleteFolder(folderId)` — удаляем локально (папка + задачи каскадом) |
| 5 | `FolderViewModel` запускает `viewModelScope.launch(IO)` |
| 6 | Для каждого `serverId`: `DELETE /api/task-lists/{serverId}` |
| 7 | `TaskListController` удаляет список и все задачи каскадом (ON DELETE CASCADE в БД) |
| 8 | Список папок обновляется автоматически через `LiveData` |
