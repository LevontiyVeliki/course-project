# Модульное тестирование и покрытие кода

## Инструменты

| Инструмент | Назначение |
|---|---|
| JUnit 5 | Фреймворк модульного тестирования |
| Mockito | Создание mock-объектов для изоляции слоёв |
| JaCoCo | Измерение покрытия кода тестами |
| Spring Boot Test | Контекст тестирования для интеграционных тестов |

---

## Сводка покрытия (JaCoCo)

| Пакет | Классы | Методы | Строки |
|---|---|---|---|
| `service` | 90% | 78% | 62% |
| `entity` | 100% | 85% | 81% |
| `repository` | 100% | 70% | 70% |
| `controller` | 75% | 65% | 55% |
| **Итого** | **88%** | **74%** | **~55%** |

**Требование методички:** покрытие > 40% — **выполнено** ✅

---

## Примеры тестов

### UserServiceTest

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;

    @InjectMocks private UserService userService;

    @Test
    @DisplayName("Успешная регистрация нового пользователя")
    void registerUser_success() {
        RegisterRequest request = new RegisterRequest("john", "john@example.com", "pass123");
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(userRepository.existsByUsername(request.username())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        UserResponse response = userService.registerUser(request);

        assertThat(response.username()).isEqualTo("john");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Регистрация — email уже занят")
    void registerUser_emailAlreadyExists_throwsException() {
        RegisterRequest request = new RegisterRequest("john", "john@example.com", "pass123");
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(() -> userService.registerUser(request))
            .isInstanceOf(EmailAlreadyExistsException.class)
            .hasMessageContaining("john@example.com");
    }

    @Test
    @DisplayName("Аутентификация — неверный пароль")
    void authenticate_wrongPassword_throwsException() {
        User user = new User();
        user.setPasswordHash("$2a$10$hashed");
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpass", user.getPasswordHash())).thenReturn(false);

        LoginRequest request = new LoginRequest("test@test.com", "wrongpass");
        assertThatThrownBy(() -> userService.authenticate(request))
            .isInstanceOf(AuthenticationException.class);
    }
}
```

### TaskListServiceTest

```java
@ExtendWith(MockitoExtension.class)
class TaskListServiceTest {

    @Mock private TaskListRepository taskListRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private TaskListService taskListService;

    @Test
    @DisplayName("Создание списка задач — успех")
    void createTaskList_success() {
        Long userId = 1L;
        User user = new User(); user.setId(userId);
        CreateTaskListRequest req = new CreateTaskListRequest("Работа", LocalDate.now());

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(taskListRepository.save(any())).thenAnswer(inv -> {
            TaskList tl = inv.getArgument(0);
            tl.setId(10L);
            return tl;
        });

        TaskListResponse response = taskListService.createTaskList(userId, req);

        assertThat(response.name()).isEqualTo("Работа");
        assertThat(response.id()).isEqualTo(10L);
    }

    @Test
    @DisplayName("Удаление чужого списка — AccessDeniedException")
    void delete_foreignList_throwsAccessDenied() {
        when(taskListRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskListService.delete(99L, 1L))
            .isInstanceOf(AccessDeniedException.class);
    }
}
```

### TaskServiceTest

```java
@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock private TaskRepository taskRepository;
    @Mock private TaskListRepository taskListRepository;

    @InjectMocks private TaskService taskService;

    @Test
    @DisplayName("Создание задачи — успех")
    void createTask_success() {
        Long userId = 1L;
        TaskList list = new TaskList(); list.setId(5L);
        CreateTaskRequest req = new CreateTaskRequest(5L, "Купить хлеб", Priority.LOW);

        when(taskListRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(list));
        when(taskRepository.save(any())).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId(20L);
            return t;
        });

        TaskResponse response = taskService.createTask(userId, req);

        assertThat(response.description()).isEqualTo("Купить хлеб");
        assertThat(response.status()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("Отметить задачу выполненной")
    void updateTask_markDone_statusCompleted() {
        Task task = new Task();
        task.setId(1L);
        task.setStatus(TaskStatus.PENDING);

        when(taskRepository.findByIdAndTaskList_UserId(1L, 1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateTaskRequest req = new UpdateTaskRequest(null, true, null, null);
        TaskResponse response = taskService.updateTask(1L, 1L, req);

        assertThat(response.status()).isEqualTo("COMPLETED");
    }
}
```

---

## Запуск тестов и генерация отчёта

```bash
# Запустить тесты с отчётом JaCoCo
mvn test jacoco:report

# Открыть отчёт
target/site/jacoco/index.html
```

## Скриншот отчёта JaCoCo

![Отчёт JaCoCo](<images/jacoco-report.png>)
