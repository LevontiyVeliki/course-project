# Диаграммы последовательности

Описывают динамику взаимодействия компонентов системы при выполнении ключевых сценариев.

---

## SD-01: Аутентификация пользователя (UC2)

![SD-01: Аутентификация пользователя](<images/SD-01_Аутентификация_пользователя.png>)

**Участники:** `LoginFragment` → `AuthViewModel` → `RetrofitClient` → `AuthController` → `UserService` → `UserRepository`

```plantuml
@startuml SD-01-Authentication
title SD-01: Аутентификация пользователя (UC2)

skinparam sequenceMessageAlign center
skinparam responseMessageBelowArrow true

actor "Пользователь" as User
participant "LoginFragment\n(Android)" as LF
participant "AuthViewModel" as AVM
participant "RetrofitClient\n(Retrofit2)" as RC
participant "AuthController\n(Spring)" as AC
participant "UserService" as US
database "UserRepository\n(PostgreSQL)" as UR

User -> LF : Вводит email + пароль,\nнажимает «Войти»
activate LF

LF -> AVM : login(email, password)
activate AVM

AVM -> RC : POST /api/auth/login\n{email, password}
activate RC

RC -> AC : LoginRequest(email, password)
activate AC

AC -> US : authenticate(request)
activate US

US -> UR : findByEmail(email)
activate UR
UR --> US : Optional<User>
deactivate UR

alt Пользователь не найден
    US --> AC : throw AuthenticationException
    AC --> RC : HTTP 401 Unauthorized
    RC --> AVM : onFailure(401)
    AVM --> LF : authError = "Неверный логин или пароль"
    LF --> User : Показывает сообщение об ошибке
else Пользователь найден
    US -> US : BCrypt.matches(rawPwd, hash)
    alt Пароль неверный
        US --> AC : throw AuthenticationException
        AC --> RC : HTTP 401 Unauthorized
        RC --> AVM : onFailure(401)
        AVM --> LF : authError = "Неверный логин или пароль"
        LF --> User : Показывает сообщение об ошибке
    else Пароль верный
        US -> US : generateJwt(userId, role)
        US --> AC : JwtResponse(token, userId, role)
        deactivate US

        AC --> RC : HTTP 200 OK + JwtResponse
        deactivate AC

        RC --> AVM : onSuccess(JwtResponse)
        deactivate RC

        AVM -> AVM : SessionManager.saveToken(token)
        AVM --> LF : navigateToMain()
        deactivate AVM

        LF --> User : Переход на главный экран
        deactivate LF
    end
end

@enduml
```

---

## SD-02: Создание папки (UC3)

![SD-02: Создание папки](<images/SD-02_Создание_папки.png>)

**Участники:** `CreateFolderActivity` → `FolderViewModel` → `TaskDatabaseHelper` / `RetrofitClient` → `TaskListController` → `TaskListService` → `TaskListRepository`

```plantuml
@startuml SD-02-CreateFolder
title SD-02: Создание папки (UC3)

skinparam sequenceMessageAlign center
skinparam responseMessageBelowArrow true

actor "Пользователь" as User
participant "CreateFolderActivity\n(Android)" as CFA
participant "FolderViewModel" as FVM
database "TaskDatabaseHelper\n(SQLite)" as DB
participant "RetrofitClient\n(Retrofit2)" as RC
participant "TaskListController\n(Spring)" as TLC
participant "TaskListService" as TLS
database "TaskListRepository\n(PostgreSQL)" as TLR

User -> CFA : Вводит название, выбирает цвет,\nнажимает «Создать папку»
activate CFA

CFA -> FVM : insertFolder(folder)
activate FVM

FVM -> DB : insertFolder(folder)
activate DB
DB --> FVM : localId
deactivate DB

FVM -> RC : POST /api/task-lists\n{name, targetDate}\nAuthorization: Bearer <JWT>
activate RC

RC -> TLC : CreateTaskListRequest(name, date)
activate TLC

TLC -> TLC : extractUserId(JWT)

TLC -> TLS : createTaskList(userId, request)
activate TLS

TLS -> TLR : save(taskList)
activate TLR
TLR --> TLS : TaskList(id=serverId)
deactivate TLR

TLS --> TLC : TaskListResponse(serverId, name, ...)
deactivate TLS

TLC --> RC : HTTP 201 Created + TaskListResponse
deactivate TLC

RC --> FVM : onSuccess(TaskListResponse)
deactivate RC

FVM -> DB : updateServerId(localId, serverId)
activate DB
DB --> FVM : ok
deactivate DB

FVM -> FVM : loadFolders() — обновить LiveData
FVM --> CFA : setResult(RESULT_OK), finish()
deactivate FVM

CFA --> User : Возврат на список папок
deactivate CFA

@enduml
```

---

## SD-03: Сохранение задачи (UC6)

![SD-03: Сохранение задачи](<images/SD-03_Сохранение_задачи.png>)

**Участники:** `TaskEditFragment` → `TaskViewModel` → `TaskDatabaseHelper` / `RetrofitClient` → `TaskController` → `TaskService` → `TaskRepository`

```plantuml
@startuml SD-03-SaveTask
title SD-03: Сохранение задачи (UC6)

skinparam sequenceMessageAlign center
skinparam responseMessageBelowArrow true

actor "Пользователь" as User
participant "TaskEditFragment\n(Android)" as TEF
participant "TaskViewModel" as TVM
database "TaskDatabaseHelper\n(SQLite)" as DB
participant "RetrofitClient\n(Retrofit2)" as RC
participant "TaskController\n(Spring)" as TC
participant "TaskService" as TS
database "TaskRepository\n(PostgreSQL)" as TR

User -> TEF : Заполняет название, описание,\nдату, время, нажимает «Сохранить»
activate TEF

TEF -> TVM : saveTask(task)
activate TVM

alt Новая задача (taskId == -1)
    TVM -> DB : insertTask(task, folderId)
    activate DB
    DB --> TVM : localTaskId
    deactivate DB

    TVM -> RC : POST /api/tasks\n{taskListId, description, priority}
    activate RC
    RC -> TC : CreateTaskRequest
    activate TC
    TC -> TC : extractUserId(JWT)
    TC -> TS : createTask(userId, request)
    activate TS
    TS -> TR : save(task)
    activate TR
    TR --> TS : Task(id=serverId)
    deactivate TR
    TS --> TC : TaskResponse(serverId, ...)
    deactivate TS
    TC --> RC : HTTP 201 Created
    deactivate TC
    RC --> TVM : onSuccess(TaskResponse)
    deactivate RC

    TVM -> DB : updateServerId(localTaskId, serverId)

else Редактирование существующей задачи
    TVM -> DB : updateTask(task)
    activate DB
    DB --> TVM : ok
    deactivate DB

    TVM -> RC : PUT /api/tasks/{serverId}\n{description, isDone, ...}
    activate RC
    RC -> TC : UpdateTaskRequest
    activate TC
    TC -> TS : updateTask(id, userId, request)
    activate TS
    TS -> TR : save(updatedTask)
    activate TR
    TR --> TS : Task
    deactivate TR
    TS --> TC : TaskResponse
    deactivate TS
    TC --> RC : HTTP 200 OK
    deactivate TC
    RC --> TVM : onSuccess(TaskResponse)
    deactivate RC
end

TVM -> TVM : loadTasksForFolder(folderId)
TVM --> TEF : popBackStack()
deactivate TVM

TEF --> User : Возврат к списку задач
deactivate TEF

@enduml
```

---

## SD-04: Удаление папки с каскадным удалением (UC5)

![SD-04: Удаление папки с каскадным удалением](<images/SD-04_Удаление_папки_с_каскадным_удалением.png>)

**Участники:** `FolderListFragment` → `FolderViewModel` → `TaskDatabaseHelper` / `RetrofitClient` → `TaskListController`

```plantuml
@startuml SD-04-DeleteFolder
title SD-04: Удаление папки с каскадным удалением (UC5)

skinparam sequenceMessageAlign center
skinparam responseMessageBelowArrow true

actor "Пользователь" as User
participant "FolderListFragment\n(Android)" as FLF
participant "FolderViewModel" as FVM
database "TaskDatabaseHelper\n(SQLite)" as DB
participant "RetrofitClient\n(Retrofit2)" as RC
participant "TaskListController\n(Spring)" as TLC
database "PostgreSQL\n(ON DELETE CASCADE)" as PG

User -> FLF : Нажимает кнопку удаления папки
activate FLF

FLF -> FLF : AlertDialog «Удалить папку?»
FLF -> FLF : Пользователь подтверждает

FLF -> FVM : deleteFolder(folderId)
activate FVM

note over FVM, DB : Сначала читаем serverIds ДО удаления!
FVM -> DB : getTasksForFolder(folderId)
activate DB
DB --> FVM : List<Task> (с serverIds)
deactivate DB

FVM -> DB : deleteFolder(folderId)
activate DB
note right of DB : Удаляет папку +\nвсе задачи каскадом\n(SQLite)
DB --> FVM : ok
deactivate DB

FVM -> FVM : loadFolders() — LiveData обновляется
FVM --> FLF : UI обновлён (папка исчезла)
deactivate FLF

note over FVM, RC : Асинхронно (IO dispatcher)
loop Для каждого serverId > 0
    FVM -> RC : DELETE /api/task-lists/{serverId}\nAuthorization: Bearer <JWT>
    activate RC
    RC -> TLC : deleteTaskList(serverId, userId)
    activate TLC
    TLC -> PG : DELETE FROM task_lists WHERE id=serverId
    note right of PG : ON DELETE CASCADE\nудаляет все tasks автоматически
    PG --> TLC : ok
    TLC --> RC : HTTP 204 No Content
    deactivate TLC
    RC --> FVM : onSuccess
    deactivate RC
end

deactivate FVM

@enduml
```

---

## Как сгенерировать диаграммы

1. Скопируй код между `@startuml` и `@enduml`
2. Вставь на сайт **[plantuml.com/plantuml](https://www.plantuml.com/plantuml/uml/)** или используй плагин PlantUML в IntelliJ / VS Code
3. Сохрани результат как `.jpg` в папку `05-detailed-design/images/`

| Файл | Имя для сохранения |
|---|---|
| SD-01 | `seq-login.jpg` |
| SD-02 | `seq-create-folder.jpg` |
| SD-03 | `seq-save-task.jpg` |
| SD-04 | `seq-delete-folder.jpg` |
