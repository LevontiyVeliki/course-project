# Диаграмма пакетов PCMEF

## Диаграмма

![PCMEF пакеты](<images/pcmef-packages.jpg>)

---

## Общее описание

Архитектурный паттерн **PCMEF** (Presentation-Control-Mediator-Entity-Foundation) адаптирован для клиент-серверной архитектуры мобильного приложения «Plan Day».

Система разделена на два физических уровня:
- **Клиент** — мобильное Android-приложение (Kotlin). Реализует слой Presentation.
- **Сервер** — Spring Boot-приложение (Java). Реализует слои Control, Mediator, Entity, Foundation.

Взаимодействие между клиентом и сервером осуществляется исключительно через **REST API** с использованием JWT-аутентификации.

---

## Слои архитектуры

### P — Presentation (Мобильное приложение)

Отвечает за отображение данных пользователю и обработку его действий.

- `Activity` / `Fragment` — экраны приложения
- `ViewModel` — хранение UI-состояния, реагирует на изменения через `LiveData`
- `Adapter` — отображение списков (RecyclerView)
- `RetrofitClient` — HTTP-клиент для обращения к серверному API
- `TaskDatabaseHelper` (SQLite) — локальное кэширование данных

### C — Control (REST-контроллеры)

Обрабатывает входящие HTTP-запросы, валидирует входные данные, маршрутизирует к соответствующим сервисам.

- `AuthController` — `/api/auth/**` (login, register)
- `TaskListController` — `/api/task-lists/**` (CRUD папок)
- `TaskController` — `/api/tasks/**` (CRUD задач)
- `UserController` — `/api/users/**` (профиль пользователя)

### M — Mediator (Сервисы бизнес-логики)

Содержит бизнес-правила и оркестрирует взаимодействие между слоями Control и Foundation.

- `UserService` / `IUserService`
- `TaskListService` / `ITaskListService`
- `TaskService` / `ITaskService`
- `JwtService` — генерация и валидация JWT-токенов

### E — Entity (JPA-сущности)

Представляют доменные объекты, отображённые на таблицы базы данных.

- `User`
- `TaskList`
- `Task`
- `Reminder`

### F — Foundation (Репозитории)

Обеспечивают доступ к данным через абстракцию JPA Repository.

- `UserRepository`
- `TaskListRepository`
- `TaskRepository`
- `ReminderRepository`

---

## Правила зависимостей

Зависимости направлены строго **сверху вниз**:

```
Presentation  →  (REST API)  →  Control
                                   ↓
                               Mediator
                                   ↓
                     Entity ← Foundation → БД (PostgreSQL)
```

- Control зависит от Mediator (через интерфейсы сервисов `IXxxService`).
- Mediator зависит от Foundation (через интерфейсы репозиториев `XxxRepository`).
- Foundation зависит от Entity и БД.
- Обратных зависимостей нет.
- Presentation взаимодействует с сервером только через REST API.
