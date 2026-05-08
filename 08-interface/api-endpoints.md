# REST API — Документация эндпоинтов

## Базовый URL

```
http://localhost:8080/api
```

**Swagger UI:** `http://localhost:8080/swagger-ui/index.html`

**OpenAPI JSON:** `http://localhost:8080/v3/api-docs`

---

## Аутентификация

Все эндпоинты (кроме `/auth/**`) требуют заголовок:

```
Authorization: Bearer <JWT-TOKEN>
```

---

## Эндпоинты (12 штук — требование: 8+)

### Аутентификация (`/api/auth`)

#### POST /api/auth/register — Регистрация

```json
// Запрос
{
  "username": "john",
  "email": "john@example.com",
  "password": "secret123"
}

// Ответ 201 Created
{
  "id": 1,
  "username": "john",
  "email": "john@example.com",
  "role": "USER",
  "createdAt": "2026-03-01T10:00:00"
}

// Ошибки: 400 (невалидные данные), 409 (email/логин уже занят)
```

#### POST /api/auth/login — Аутентификация

```json
// Запрос
{
  "email": "john@example.com",
  "password": "secret123"
}

// Ответ 200 OK
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer",
  "userId": 1,
  "username": "john",
  "email": "john@example.com",
  "role": "USER"
}

// Ошибки: 401 (неверные данные)
```

---

### Пользователь (`/api/users`)

#### GET /api/users/me — Профиль текущего пользователя

```json
// Ответ 200 OK
{
  "id": 1,
  "username": "john",
  "email": "john@example.com",
  "fullName": "Иванов Иван",
  "role": "USER",
  "createdAt": "2026-03-01T10:00:00",
  "taskCount": 15
}
```

#### PUT /api/users/me — Обновление профиля

```json
// Запрос
{
  "fullName": "Иванов Иван Иванович"
}

// Ответ 200 OK — обновлённый профиль
```

---

### Папки / Списки задач (`/api/task-lists`)

#### GET /api/task-lists — Список папок пользователя

```json
// Ответ 200 OK
[
  {
    "id": 5,
    "name": "Работа",
    "targetDate": "2026-05-08",
    "status": "ACTIVE",
    "createdAt": "2026-04-01T09:00:00"
  },
  ...
]
```

#### POST /api/task-lists — Создать папку

```json
// Запрос
{
  "name": "Учёба",
  "targetDate": "2026-05-15"
}

// Ответ 201 Created
{
  "id": 6,
  "name": "Учёба",
  "targetDate": "2026-05-15",
  "status": "ACTIVE",
  "createdAt": "2026-05-08T12:00:00"
}
```

#### GET /api/task-lists/{id} — Получить папку по ID

```
// Ответ 200 OK — объект папки
// Ошибки: 404 (не найдена), 403 (чужая папка)
```

#### PUT /api/task-lists/{id} — Обновить папку

```json
// Запрос
{
  "name": "Новое название",
  "targetDate": "2026-06-01"
}
// Ответ 200 OK
```

#### DELETE /api/task-lists/{id} — Удалить папку

```
// Ответ 204 No Content
// Каскадно удаляет все задачи через ON DELETE CASCADE
```

---

### Задачи (`/api/tasks`)

#### GET /api/tasks?listId={id} — Задачи в папке

```json
// Ответ 200 OK
[
  {
    "id": 10,
    "taskListId": 5,
    "description": "Написать отчёт",
    "status": "PENDING",
    "priority": "HIGH",
    "createdAt": "2026-05-01T08:00:00"
  },
  ...
]
```

#### POST /api/tasks — Создать задачу

```json
// Запрос
{
  "taskListId": 5,
  "description": "Сдать курсовую",
  "priority": "HIGH"
}

// Ответ 201 Created
```

#### PUT /api/tasks/{id} — Обновить задачу

```json
// Запрос
{
  "description": "Обновлённое название",
  "isDone": true,
  "priority": "MEDIUM"
}

// Ответ 200 OK
```

#### DELETE /api/tasks/{id} — Удалить задачу

```
// Ответ 204 No Content
```

---

## Таблица эндпоинтов

| # | Метод | URL | Описание | Авторизация |
|---|---|---|---|---|
| 1 | POST | `/api/auth/register` | Регистрация | Нет |
| 2 | POST | `/api/auth/login` | Вход, получение токена | Нет |
| 3 | GET | `/api/users/me` | Профиль пользователя | JWT |
| 4 | PUT | `/api/users/me` | Обновление профиля | JWT |
| 5 | GET | `/api/task-lists` | Все папки пользователя | JWT |
| 6 | POST | `/api/task-lists` | Создать папку | JWT |
| 7 | GET | `/api/task-lists/{id}` | Получить папку | JWT |
| 8 | PUT | `/api/task-lists/{id}` | Обновить папку | JWT |
| 9 | DELETE | `/api/task-lists/{id}` | Удалить папку | JWT |
| 10 | GET | `/api/tasks?listId={id}` | Задачи папки | JWT |
| 11 | POST | `/api/tasks` | Создать задачу | JWT |
| 12 | PUT | `/api/tasks/{id}` | Обновить задачу | JWT |
| 13 | DELETE | `/api/tasks/{id}` | Удалить задачу | JWT |

**Итого: 13 эндпоинтов** — требование 8+ выполнено ✅

---

## Swagger UI

![Swagger UI](<images/swagger-ui.png>)
