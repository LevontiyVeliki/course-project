# Применённые паттерны рефакторинга

## Data Mapper

### Назначение (из методички)
Отделение бизнес-логики (Entity) от логики доступа к данным. Entity-классы не должны содержать SQL-код или знать о структуре БД.

### Проблема (до рефакторинга)
Entity-классы напрямую использовались как DTO в ответах REST API — клиент получал внутренние поля (`passwordHash`, `updatedAt`, `@ManyToOne`-ссылки с циклическими зависимостями).

### Решение
Введены отдельные классы-маппер (`TaskListMapper`, `TaskMapper`, `UserMapper`) и DTO-records для запросов/ответов.

```java
// Маппер разделяет знания Entity и контракт REST API
@Component
public class TaskMapper {

    public TaskResponse toResponse(Task task) {
        return new TaskResponse(
            task.getId(),
            task.getTaskList().getId(),
            task.getDescription(),
            task.getStatus().name(),
            task.getPriority().name(),
            task.getCreatedAt()
        );
    }

    public Task toEntity(CreateTaskRequest request, TaskList list) {
        Task task = new Task();
        task.setTaskList(list);
        task.setDescription(request.description());
        task.setPriority(request.priority() != null ? request.priority() : Priority.MEDIUM);
        task.setStatus(TaskStatus.PENDING);
        return task;
    }
}
```

**Результат:** Entity-классы не зависят от REST-контракта. Изменение структуры ответа API не требует изменения Entity.

---

## Identity Map

### Назначение (из методички)
Обеспечение уникальности объектов в сессии: одна и та же строка БД всегда представлена одним объектом в памяти. Предотвращает дублирование объектов и несогласованность данных.

### Реализация
В проекте Identity Map реализован через **JPA First-Level Cache** (кэш первого уровня Hibernate). В рамках одной транзакции (`@Transactional`) каждый объект Entity загружается из БД ровно один раз и кэшируется в `EntityManager`.

```java
@Transactional
public void demonstrateIdentityMap() {
    // Оба вызова возвращают ОДИН и тот же объект из кэша EntityManager
    TaskList list1 = taskListRepository.findById(1L).get();
    TaskList list2 = taskListRepository.findById(1L).get();

    System.out.println(list1 == list2); // true — Identity Map!
}
```

### Дополнительная реализация: кэш на Android
На клиенте Identity Map реализован через `TaskDatabaseHelper` — локальная SQLite-база выступает как единый источник истины для UI.

---

## Lazy Load

### Назначение
Отложенная загрузка связанных объектов: связанные коллекции загружаются только при первом обращении, а не при загрузке родительской сущности.

### Реализация

```java
// TaskList НЕ загружает все Task при запросе списка папок
@OneToMany(mappedBy = "taskList", fetch = FetchType.LAZY)
private List<Task> tasks;

// User НЕ загружает все TaskList при аутентификации
@OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
private List<TaskList> taskLists;
```

**Проблема N+1:** при неаккуратном использовании Lazy Load может вызвать N дополнительных SQL-запросов. Решение — использование `@EntityGraph` или JPQL JOIN FETCH для сценариев, требующих данных коллекции.

```java
// Используем JOIN FETCH при необходимости
@Query("SELECT tl FROM TaskList tl LEFT JOIN FETCH tl.tasks WHERE tl.user.id = :userId")
List<TaskList> findByUserIdWithTasks(@Param("userId") Long userId);
```
