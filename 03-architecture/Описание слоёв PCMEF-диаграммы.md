# Описание слоёв PCMEF-диаграммы

## Таблица слоёв

| Слой | Расположение | Ответственность | Компоненты |
|---|---|---|---|
| **Presentation** | Мобильное устройство | Отображение данных, обработка пользовательского ввода, локальное кэширование | Activity, Fragment, ViewModel, RecyclerView Adapter, SQLite (TaskDatabaseHelper) |
| **Control** | Сервер | Обработка HTTP-запросов, валидация входных данных, маршрутизация к сервисам | `@RestController` классы: AuthController, TaskListController, TaskController, UserController |
| **Mediator** | Сервер | Бизнес-логика: проверка прав, бизнес-правила, оркестрация операций | `@Service` классы: UserService, TaskListService, TaskService, JwtTokenProvider |
| **Entity** | Сервер | Представление бизнес-сущностей с поведением, отображение на таблицы БД | `@Entity` классы: User, TaskList, Task, Reminder |
| **Foundation** | Сервер | Доступ к базе данных, маппинг объектов через ORM | `@Repository` интерфейсы: UserRepository, TaskListRepository, TaskRepository, ReminderRepository |

---

## Правила зависимостей

Зависимости направлены строго **сверху вниз**:

- **Control** зависит от **Mediator** (через интерфейсы сервисов `IXxxService`).
- **Mediator** зависит от **Entity** и **Foundation** (через интерфейсы репозиториев).
- **Foundation** зависит от **Entity** и базы данных (PostgreSQL).

**Обратных зависимостей нет.** Presentation взаимодействует с сервером только через REST API — нет прямых зависимостей от серверных классов.

---

## Распределение компонентов реального проекта

### Клиентская часть (Presentation)

| Компонент | Класс | Роль |
|---|---|---|
| Экран списка папок | `FolderListFragment` | Отображение папок пользователя |
| Экран задач в папке | `FolderTasksFragment` | Список задач выбранной папки |
| Экран редактирования задачи | `TaskEditFragment` | Создание и редактирование задачи |
| ViewModel задач | `TaskViewModel` | Управление состоянием списка задач |
| ViewModel папок | `FolderViewModel` | Управление состоянием списка папок |
| HTTP-клиент | `RetrofitClient` | Синглтон Retrofit для API-запросов |
| Локальная БД | `TaskDatabaseHelper` | SQLite: кэширование папок и задач |

### Серверная часть (Control → Foundation)

| Слой | Класс | Роль |
|---|---|---|
| Control | `AuthController` | POST /api/auth/login, /register |
| Control | `TaskListController` | CRUD /api/tasklists |
| Control | `TaskController` | CRUD /api/tasks |
| Mediator | `UserService` | Регистрация, аутентификация пользователей |
| Mediator | `TaskListService` | Бизнес-логика работы со списками |
| Mediator | `TaskService` | Бизнес-логика работы с задачами |
| Mediator | `JwtTokenProvider` | Генерация и валидация JWT-токенов |
| Entity | `User`, `TaskList`, `Task` | JPA-сущности |
| Foundation | `UserRepository` | Запросы к таблице `users` |
| Foundation | `TaskListRepository` | Запросы к таблице `task_lists` |
| Foundation | `TaskRepository` | Запросы к таблице `tasks` |
