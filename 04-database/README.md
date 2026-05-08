# Этап 3: Проектирование базы данных

## Цель этапа

Разработать логическую и физическую модель данных для серверной части мобильного приложения «Plan Day», создать DDL-скрипты для PostgreSQL и определить стратегию объектно-реляционного отображения (ORM) с использованием JPA/Hibernate.

## Результаты

- [ER-диаграмма логической модели данных](#er-диаграмма)
- [DDL-скрипты для PostgreSQL](ddl.sql)
- [Описание маппинга JPA-сущностей](#маппинг-jpa-сущностей)

---

## ER-диаграмма

![ER-диаграмма](<images/er-diagram.png>)

---

## Описание таблиц

### `users` — Пользователи

| Столбец | Тип | Ограничения | Описание |
|---|---|---|---|
| `id` | BIGSERIAL | PRIMARY KEY | Уникальный идентификатор |
| `username` | VARCHAR(50) | NOT NULL, UNIQUE | Логин |
| `email` | VARCHAR(100) | NOT NULL, UNIQUE | Email (используется для входа) |
| `password_hash` | VARCHAR(255) | NOT NULL | BCrypt-хеш пароля |
| `full_name` | VARCHAR(100) | — | Полное имя пользователя |
| `role` | VARCHAR(20) | NOT NULL, DEFAULT 'USER' | Роль: `USER` или `ADMIN` |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() | Дата регистрации |
| `updated_at` | TIMESTAMP | — | Дата последнего обновления профиля |

---

### `task_lists` — Списки задач (Папки)

| Столбец | Тип | Ограничения | Описание |
|---|---|---|---|
| `id` | BIGSERIAL | PRIMARY KEY | Уникальный идентификатор |
| `user_id` | BIGINT | NOT NULL, FK → users.id | Владелец списка |
| `name` | VARCHAR(100) | NOT NULL | Название папки |
| `target_date` | DATE | NOT NULL | Дата исполнения |
| `status` | VARCHAR(20) | NOT NULL, DEFAULT 'ACTIVE' | Статус: `ACTIVE`, `COMPLETED` |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() | Дата создания |
| `updated_at` | TIMESTAMP | — | Дата последнего изменения |

**Индексы:** `idx_task_lists_user_date (user_id, target_date)` — быстрый поиск списков пользователя по дате.

---

### `tasks` — Задачи

| Столбец | Тип | Ограничения | Описание |
|---|---|---|---|
| `id` | BIGSERIAL | PRIMARY KEY | Уникальный идентификатор |
| `task_list_id` | BIGINT | NOT NULL, FK → task_lists.id | Родительский список |
| `description` | TEXT | NOT NULL | Текст задачи |
| `status` | VARCHAR(20) | NOT NULL, DEFAULT 'PENDING' | Статус: `PENDING`, `COMPLETED`, `OVERDUE` |
| `priority` | VARCHAR(20) | NOT NULL, DEFAULT 'MEDIUM' | Приоритет: `LOW`, `MEDIUM`, `HIGH` |
| `order_index` | INTEGER | NOT NULL, DEFAULT 0 | Порядок сортировки в UI |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() | Дата создания |
| `updated_at` | TIMESTAMP | — | Дата последнего изменения |

**Индексы:** `idx_tasks_list_order (task_list_id, order_index)` — сортировка задач внутри списка.

---

### `reminders` — Напоминания

| Столбец | Тип | Ограничения | Описание |
|---|---|---|---|
| `id` | BIGSERIAL | PRIMARY KEY | Уникальный идентификатор |
| `task_list_id` | BIGINT | FK → task_lists.id | Список, для которого задано напоминание |
| `task_id` | BIGINT | FK → tasks.id | Задача, для которой задано напоминание |
| `trigger_time` | TIMESTAMP | NOT NULL | Время срабатывания |
| `message` | TEXT | — | Текст уведомления (NULL = авто) |
| `is_sent` | BOOLEAN | NOT NULL, DEFAULT FALSE | Флаг: было ли отправлено |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() | Дата создания |

**Ограничение:** ровно одно из полей `task_list_id` / `task_id` должно быть NOT NULL (CHECK constraint).

**Индексы:** `idx_reminders_unsent (is_sent, trigger_time) WHERE is_sent = FALSE` — быстрый поиск неотправленных напоминаний планировщиком.

---

## Маппинг JPA-сущностей

### Основные аннотации

```java
@Entity
@Table(name = "task_lists")
public class TaskList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ListStatus status = ListStatus.ACTIVE;

    @OneToMany(mappedBy = "taskList", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Task> tasks = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
```

### Стратегия каскадного удаления

При удалении `User` → каскадно удаляются все его `TaskList`.  
При удалении `TaskList` → каскадно удаляются все `Task` и `Reminder`.  
При удалении `Task` → каскадно удаляются все связанные `Reminder`.

Реализовано через: `@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)` + `ON DELETE CASCADE` в DDL.

---

## Стратегия индексирования

| Индекс | Таблица | Столбцы | Цель |
|---|---|---|---|
| `idx_users_email` | users | email | Быстрая аутентификация по email |
| `idx_task_lists_user_date` | task_lists | user_id, target_date | Загрузка списков пользователя по дате |
| `idx_tasks_list_order` | tasks | task_list_id, order_index | Сортировка задач в списке |
| `idx_reminders_unsent` | reminders | is_sent, trigger_time | Опрос неотправленных напоминаний |
