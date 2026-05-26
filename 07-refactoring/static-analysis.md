# Отчёт статического анализа

## Инструменты анализа

| Инструмент | Тип анализа | Применение |
|---|---|---|
| SonarQube | Качество кода, уязвимости, запахи кода | Серверная часть (Java) |
| Checkstyle | Соответствие стандарту оформления | Серверная часть (Java) |
| Android Lint | Анализ Android-кода | Мобильное приложение (Kotlin) |

---

## Результаты SonarQube (серверная часть)

### До рефакторинга

| Категория | Проблем | Критичность |
|---|---|---|
| Bugs (ошибки) | 3 | MAJOR, MINOR |
| Vulnerabilities (уязвимости) | 1 | MAJOR |
| Code Smells (запахи) | 12 | INFO, MINOR |
| Дублирование кода | 8% | — |
| Технический долг | 2h 30min | — |

### После рефакторинга

| Категория | Проблем | Изменение |
|---|---|---|
| Bugs | 0 | ↓ −3 |
| Vulnerabilities | 0 | ↓ −1 |
| Code Smells | 4 | ↓ −8 |
| Дублирование кода | 3% | ↓ −5% |
| Технический долг | 40min | ↓ −1h 50min |

---

## Устранённые критические проблемы

### Bug: Null Pointer в TaskService

**До:**
```java
public TaskResponse updateTask(Long id, Long userId, UpdateTaskRequest request) {
    Task task = taskRepository.findByIdAndTaskList_UserId(id, userId).get(); // NPE!
    task.setDescription(request.description());
    ...
}
```

**После:**
```java
public TaskResponse updateTask(Long id, Long userId, UpdateTaskRequest request) {
    Task task = taskRepository.findByIdAndTaskList_UserId(id, userId)
        .orElseThrow(() -> new EntityNotFoundException("Задача не найдена: " + id));
    if (request.description() != null) task.setDescription(request.description());
    ...
}
```

---

### Vulnerability: JWT-секрет в исходном коде

**До:**
```java
private final String secretKey = "mySecretKey12345"; // в коде!
```

**После:**
```java
@Value("${jwt.secret}")
private String secretKey; // читается из application.properties / переменной окружения
```

---

### Code Smell: дублирование маппинга Entity → Response

**До:** в каждом сервисе свой дублирующийся код преобразования.

**После:** выделены mapper-методы (паттерн Data Mapper):

```java
@Component
public class TaskListMapper {
    public TaskListResponse toResponse(TaskList entity) {
        return new TaskListResponse(
            entity.getId(),
            entity.getName(),
            entity.getTargetDate(),
            entity.getStatus().name(),
            entity.getCreatedAt()
        );
    }

    public TaskList toEntity(CreateTaskListRequest request, User owner) {
        TaskList list = new TaskList();
        list.setUser(owner);
        list.setName(request.name());
        list.setTargetDate(request.targetDate());
        list.setStatus(ListStatus.ACTIVE);
        return list;
    }
}
```

---

## Результаты Android Lint (мобильное приложение)

| Категория | До | После |
|---|---|---|
| Warnings | 11 | 4 |
| Errors | 0 | 0 |
| Unused resources | 3 | 0 |
| Deprecated API calls | 2 | 0 |

<!-- TODO: добавить скриншот отчёта SonarQube — images/sonar-report.png -->
