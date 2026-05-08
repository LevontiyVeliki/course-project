# Реализация Entity-слоя

Entity-классы представляют бизнес-объекты, отображённые на таблицы базы данных. Аннотация `@Entity` подключает JPA/Hibernate. Классы не являются «анемичными» — содержат бизнес-методы.

---

## User

```java
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name", length = 100)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TaskList> taskLists = new ArrayList<>();

    // Бизнес-метод: проверка роли
    public boolean isAdmin() {
        return Role.ADMIN.equals(this.role);
    }

    // Бизнес-метод: полное имя или логин если имя не задано
    public String getDisplayName() {
        return (fullName != null && !fullName.isBlank()) ? fullName : username;
    }
}

public enum Role { USER, ADMIN }
```

---

## TaskList

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

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "taskList", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Task> tasks = new ArrayList<>();

    // Бизнес-метод: все задачи выполнены?
    public boolean isCompleted() {
        if (tasks.isEmpty()) return false;
        return tasks.stream().allMatch(t -> t.getStatus() == TaskStatus.COMPLETED);
    }

    // Бизнес-метод: количество выполненных задач
    public long countCompleted() {
        return tasks.stream().filter(t -> t.getStatus() == TaskStatus.COMPLETED).count();
    }
}

public enum ListStatus { ACTIVE, COMPLETED }
```

---

## Task

```java
@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_list_id", nullable = false)
    private TaskList taskList;

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status = TaskStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority = Priority.MEDIUM;

    @Column(name = "order_index", nullable = false)
    private int orderIndex = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // Бизнес-метод: отметить выполненной
    public void markCompleted() {
        this.status = TaskStatus.COMPLETED;
    }

    // Бизнес-метод: обновить статус просрочки
    public void refreshOverdueStatus() {
        if (this.status == TaskStatus.PENDING) {
            LocalDate listDate = this.taskList.getTargetDate();
            if (listDate != null && listDate.isBefore(LocalDate.now())) {
                this.status = TaskStatus.OVERDUE;
            }
        }
    }
}

public enum TaskStatus  { PENDING, COMPLETED, OVERDUE }
public enum Priority    { LOW, MEDIUM, HIGH }
```

---

## DTO (Data Transfer Objects)

### Запросы (Request)

```java
public record LoginRequest(
    @NotBlank String email,
    @NotBlank String password
) {}

public record RegisterRequest(
    @NotBlank @Size(min=3, max=50) String username,
    @NotBlank @Email String email,
    @NotBlank @Size(min=6) String password
) {}

public record CreateTaskListRequest(
    @NotBlank @Size(max=100) String name,
    @NotNull LocalDate targetDate
) {}

public record CreateTaskRequest(
    @NotNull Long taskListId,
    @NotBlank String description,
    Priority priority
) {}
```

### Ответы (Response)

```java
public record TaskListResponse(
    Long id,
    String name,
    LocalDate targetDate,
    String status,
    LocalDateTime createdAt
) {}

public record TaskResponse(
    Long id,
    Long taskListId,
    String description,
    String status,
    String priority,
    LocalDateTime createdAt
) {}

public record JwtResponse(
    String token,
    String type,      // "Bearer"
    Long userId,
    String username,
    String email,
    String role
) {}
```
