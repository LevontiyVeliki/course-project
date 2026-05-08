# Диаграмма классов проектирования

## Диаграмма

![Диаграмма классов проектирования](<images/class-diagram.jpg>)

---

## Клиентская часть (Android)

### Слой Presentation

```
+---------------------------+       +---------------------------+
|     FolderListFragment    |       |    FolderTasksFragment    |
+---------------------------+       +---------------------------+
| - viewModel: FolderVM     |       | - viewModel: TaskVM       |
| - adapter: FolderListAdap.|       | - adapter: TaskListAdap.  |
| - folderId: Long          |       | - folderId: Long          |
+---------------------------+       | - folderName: String      |
| + onViewCreated()         |       +---------------------------+
| + confirmDeleteFolder()   |       | + onViewCreated()         |
+---------------------------+       | + onResume()              |
                                    +---------------------------+

+---------------------------+       +---------------------------+
|     TaskEditFragment      |       |    CreateFolderActivity   |
+---------------------------+       +---------------------------+
| - taskId: Long            |       | - selectedColorIndex: Int |
| - folderId: Long          |       | - chipIds: List<Int>      |
+---------------------------+       +---------------------------+
| + saveTask()              |       | + saveFolder()            |
| + showDatePicker()        |       | + selectColor()           |
| + showTimePicker()        |       +---------------------------+
+---------------------------+
```

### Слой ViewModel

```
+---------------------------+       +---------------------------+
|       FolderViewModel     |       |       TaskViewModel        |
+---------------------------+       +---------------------------+
| - dbHelper: DBHelper      |       | - dbHelper: DBHelper      |
| - _folders: MutableLD     |       | - _tasks: MutableLD       |
+---------------------------+       +---------------------------+
| + loadFolders()           |       | + loadTasksForFolder()    |
| + insertFolder()          |       | + saveTask()              |
| + deleteFolder()          |       | + updateTask()            |
+---------------------------+       | + deleteTask()            |
                                    +---------------------------+
```

### Слой Data / Infrastructure

```
+---------------------------+       +---------------------------+
|    TaskDatabaseHelper     |       |       RetrofitClient      |
+---------------------------+       +---------------------------+
| - DB_NAME: String         |       | - token: String?          |
| - DB_VERSION: Int         |       | - instance: ApiService    |
+---------------------------+       +---------------------------+
| + insertFolder()          |       | + getInstance()           |
| + getAllFolders()          |       |   : ApiService            |
| + deleteFolder()          |       +---------------------------+
| + insertTask()            |
| + getTasksForFolder()     |       +---------------------------+
| + deleteTask()            |       |       SessionManager      |
+---------------------------+       +---------------------------+
                                    | - prefs: SharedPrefs      |
                                    +---------------------------+
                                    | + saveToken()             |
                                    | + getToken()              |
                                    | + saveFullName()          |
                                    | + getFullName()           |
                                    | + isLoggedIn()            |
                                    | + clearSession()          |
                                    +---------------------------+
```

---

## Серверная часть (Spring Boot)

### Слой Control

```
+---------------------------+       +---------------------------+
|      AuthController       |       |   TaskListController      |
+---------------------------+       +---------------------------+
| - userService: IUserSvc   |       | - service: ITaskListSvc   |
| - jwtService: JwtService  |       +---------------------------+
+---------------------------+       | + getAll()                |
| + login()                 |       | + getById()               |
| + register()              |       | + create()                |
+---------------------------+       | + update()                |
                                    | + delete()                |
+---------------------------+       +---------------------------+
|      TaskController       |
+---------------------------+       +---------------------------+
| - service: ITaskService   |       |      UserController       |
+---------------------------+       +---------------------------+
| + getByList()             |       | - service: IUserService   |
| + create()                |       +---------------------------+
| + update()                |       | + getProfile()            |
| + delete()                |       | + updateProfile()         |
+---------------------------+       +---------------------------+
```

### Слой Mediator (Service)

```
+---------------------------+       +---------------------------+
|       UserService         |       |     TaskListService       |
+---------------------------+       +---------------------------+
| - repo: UserRepository    |       | - repo: TaskListRepo      |
| - jwtService: JwtService  |       | - userRepo: UserRepo      |
| - encoder: PwdEncoder     |       +---------------------------+
+---------------------------+       | + create()                |
| + registerUser()          |       | + getAll()                |
| + authenticate()          |       | + getById()               |
| + getUserById()           |       | + update()                |
+---------------------------+       | + delete()                |
                                    +---------------------------+

+---------------------------+       +---------------------------+
|       TaskService         |       |       JwtService          |
+---------------------------+       +---------------------------+
| - repo: TaskRepository    |       | - secretKey: String       |
| - listRepo: TaskListRepo  |       | - expiration: Long        |
+---------------------------+       +---------------------------+
| + createTask()            |       | + generateToken()         |
| + updateTask()            |       | + validateToken()         |
| + deleteTask()            |       | + extractUserId()         |
| + markCompleted()         |       +---------------------------+
+---------------------------+
```

### Слой Entity

```
+---------------------------+
|           User            |
+---------------------------+
| - id: Long                |
| - username: String        |
| - email: String           |
| - passwordHash: String    |
| - fullName: String        |
| - role: Role (enum)       |
| - createdAt: LocalDateTime|
| - taskLists: List<TaskList>|
+---------------------------+

+---------------------------+       +---------------------------+
|         TaskList          |       |           Task            |
+---------------------------+       +---------------------------+
| - id: Long                |       | - id: Long                |
| - user: User              |       | - taskList: TaskList      |
| - name: String            |       | - description: String     |
| - targetDate: LocalDate   |       | - status: TaskStatus      |
| - status: ListStatus      |       | - priority: Priority      |
| - tasks: List<Task>       |       | - orderIndex: Int         |
| - createdAt: LDT          |       | - createdAt: LDT          |
+---------------------------+       +---------------------------+
```

### Слой Foundation (Repository)

```
+---------------------------+       +---------------------------+
|      UserRepository       |       |   TaskListRepository      |
+---------------------------+       +---------------------------+
| extends JpaRepository     |       | extends JpaRepository     |
+---------------------------+       +---------------------------+
| + findByEmail()           |       | + findByUserIdOrderBy...  |
| + existsByEmail()         |       | + findByIdAndUserId()     |
| + existsByUsername()      |       +---------------------------+
+---------------------------+
                                    +---------------------------+
                                    |     TaskRepository        |
                                    +---------------------------+
                                    | extends JpaRepository     |
                                    +---------------------------+
                                    | + findByTaskListId...     |
                                    | + findByIdAndUserId()     |
                                    | + countByUserId()         |
                                    +---------------------------+
```
