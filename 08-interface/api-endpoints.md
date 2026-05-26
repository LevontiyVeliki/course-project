# REST API — Документация эндпоинтов

## Базовый URL

```
https://taskplanner-server.up.railway.app/api
```

> Для локального запуска: `http://localhost:8080/api`

**Swagger UI:** `http://localhost:8080/swagger-ui/index.html`

**OpenAPI JSON:** `http://localhost:8080/v3/api-docs`

---

## Аутентификация

Все эндпоинты (кроме `/auth/**`) требуют заголовок:

```
Authorization: Bearer <JWT-TOKEN>
```

---

## Эндпоинты (15 штук — требование: 8+)

### Аутентификация (`/api/auth`)

#### POST /api/auth/register — Регистрация

```json
// Запрос
{
  "username": "john",
  "email": "john@example.com",
  "password": "secret123"
}

// Ответ 200 OK
"User registered successfully"

// Ошибки: 400 (невалидные данные), 409 (email/логин уже занят)
```

#### POST /api/auth/login — Аутентификация

```json
// Запрос
{
  "username": "john",
  "password": "secret123"
}

// Ответ 200 OK
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer",
  "id": 1,
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
  "createdAt": "01.03.2026 10:00",
  "taskListsCount": 5
}
```

#### PATCH /api/users/me — Обновление профиля (полное имя)

```json
// Запрос
{
  "fullName": "Иванов Иван Иванович"
}

// Ответ 200 OK
"updated"
```

#### PATCH /api/users/me/password — Смена пароля

```json
// Запрос
{
  "newPassword": "newSecret456"
}

// Ответ 200 OK
"password updated"

// Ошибки: 400 (пароль менее 6 символов)
```

---

### Папки / Списки задач (`/api/tasklists`)

#### GET /api/tasklists — Список папок пользователя

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

#### POST /api/tasklists — Создать папку

```json
// Запрос
{
  "name": "Учёба",
  "targetDate": "2026-05-15",
  "status": "ACTIVE"
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

#### GET /api/tasklists/{id} — Получить папку по ID

```
// Ответ 200 OK — объект папки
// Ошибки: 404 (не найдена), 403 (чужая папка)
```

#### PUT /api/tasklists/{id} — Обновить папку

```json
// Запрос
{
  "name": "Новое название",
  "targetDate": "2026-06-01"
}
// Ответ 200 OK
```

#### DELETE /api/tasklists/{id} — Удалить папку

```
// Ответ 200 OK
// Каскадно удаляет все задачи через ON DELETE CASCADE
```

---

### Задачи (`/api/tasks`)

#### GET /api/tasks/tasklist/{taskListId} — Задачи в папке

```json
// Ответ 200 OK
[
  {
    "id": 10,
    "taskListId": 5,
    "description": "Написать отчёт",
    "status": "PENDING",
    "priority": "HIGH",
    "orderIndex": 0
  },
  ...
]
```

#### POST /api/tasks/tasklist/{taskListId} — Создать задачу в папке

```json
// Запрос
{
  "description": "Сдать курсовую",
  "status": "PENDING",
  "priority": "HIGH",
  "orderIndex": 0
}

// Ответ 201 Created — объект созданной задачи
```

#### PUT /api/tasks/{id} — Обновить задачу

```json
// Запрос
{
  "description": "Обновлённое название",
  "status": "COMPLETED",
  "priority": "MEDIUM",
  "orderIndex": 0
}

// Ответ 200 OK
```

#### DELETE /api/tasks/{id} — Удалить задачу

```
// Ответ 200 OK
```

---

## Таблица эндпоинтов

| # | Метод | URL | Описание | Авторизация |
|---|---|---|---|---|
| 1 | POST | `/api/auth/register` | Регистрация | Нет |
| 2 | POST | `/api/auth/login` | Вход, получение токена | Нет |
| 3 | GET | `/api/users/me` | Профиль пользователя | JWT |
| 4 | PATCH | `/api/users/me` | Обновление полного имени | JWT |
| 5 | PATCH | `/api/users/me/password` | Смена пароля | JWT |
| 6 | GET | `/api/tasklists` | Все папки пользователя | JWT |
| 7 | POST | `/api/tasklists` | Создать папку | JWT |
| 8 | GET | `/api/tasklists/{id}` | Получить папку | JWT |
| 9 | PUT | `/api/tasklists/{id}` | Обновить папку | JWT |
| 10 | DELETE | `/api/tasklists/{id}` | Удалить папку | JWT |
| 11 | GET | `/api/tasks/tasklist/{id}` | Задачи папки | JWT |
| 12 | POST | `/api/tasks/tasklist/{id}` | Создать задачу | JWT |
| 13 | GET | `/api/tasks/{id}` | Получить задачу по ID | JWT |
| 14 | PUT | `/api/tasks/{id}` | Обновить задачу | JWT |
| 15 | DELETE | `/api/tasks/{id}` | Удалить задачу | JWT |

**Итого: 15 эндпоинтов** — требование 8+ выполнено ✅

---

## Приоритеты задач

| Значение | Описание | Цвет папки |
|---|---|---|
| `LOW` | Низкий приоритет | 🟢 Зелёный |
| `MEDIUM` | Средний приоритет | 🔵 Синий |
| `HIGH` | Высокий приоритет | 🔴 Красный |
| `URGENT` | Наивысший приоритет | 🟣 Фиолетовый |

Приоритет задачи определяется автоматически по цвету папки, в которой она находится.
