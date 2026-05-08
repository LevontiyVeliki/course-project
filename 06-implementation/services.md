# Реализация сервисного слоя (Mediator)

Сервисы содержат бизнес-логику и транзакционные границы. Управляют правами доступа (кто владелец данных) и орчестрируют операции с репозиториями.

---

## UserService

```java
@Service
@Transactional
public class UserService implements IUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    public UserResponse registerUser(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException("Email уже занят: " + request.email());
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new UsernameAlreadyExistsException("Логин занят: " + request.username());
        }
        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.USER);
        User saved = userRepository.save(user);
        return toResponse(saved);
    }

    @Override
    public JwtResponse authenticate(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
            .orElseThrow(() -> new AuthenticationException("Неверный логин или пароль"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new AuthenticationException("Неверный логин или пароль");
        }
        String token = jwtService.generateToken(user);
        return new JwtResponse(token, "Bearer",
            user.getId(), user.getUsername(), user.getEmail(), user.getRole().name());
    }
}
```

---

## TaskListService

```java
@Service
@Transactional
public class TaskListService implements ITaskListService {

    private final TaskListRepository taskListRepository;
    private final UserRepository userRepository;

    @Override
    public TaskListResponse createTaskList(Long userId, CreateTaskListRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
        TaskList list = new TaskList();
        list.setUser(user);
        list.setName(request.name());
        list.setTargetDate(request.targetDate());
        list.setStatus(ListStatus.ACTIVE);
        return toResponse(taskListRepository.save(list));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskListResponse> getAllByUser(Long userId) {
        return taskListRepository
            .findByUserIdOrderByCreatedAtDesc(userId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Override
    public void delete(Long id, Long userId) {
        TaskList list = taskListRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new AccessDeniedException("Папка не найдена или нет доступа"));
        taskListRepository.delete(list);  // каскадно удаляет все Tasks
    }
}
```

---

## TaskService

```java
@Service
@Transactional
public class TaskService implements ITaskService {

    private final TaskRepository taskRepository;
    private final TaskListRepository taskListRepository;

    @Override
    public TaskResponse createTask(Long userId, CreateTaskRequest request) {
        TaskList list = taskListRepository
            .findByIdAndUserId(request.taskListId(), userId)
            .orElseThrow(() -> new AccessDeniedException("Нет доступа к папке"));

        Task task = new Task();
        task.setTaskList(list);
        task.setDescription(request.description());
        task.setPriority(request.priority() != null ? request.priority() : Priority.MEDIUM);
        task.setStatus(TaskStatus.PENDING);
        return toResponse(taskRepository.save(task));
    }

    @Override
    public TaskResponse updateTask(Long id, Long userId, UpdateTaskRequest request) {
        Task task = taskRepository.findByIdAndTaskList_UserId(id, userId)
            .orElseThrow(() -> new AccessDeniedException("Задача не найдена или нет доступа"));

        if (request.description() != null) task.setDescription(request.description());
        if (request.isDone() != null) {
            if (request.isDone()) {
                task.markCompleted();
            } else {
                task.setStatus(TaskStatus.PENDING);
                task.refreshOverdueStatus();
            }
        }
        return toResponse(taskRepository.save(task));
    }
}
```

---

## JwtService

```java
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long expirationMs;

    public String generateToken(User user) {
        return Jwts.builder()
            .setSubject(user.getId().toString())
            .claim("email", user.getEmail())
            .claim("role", user.getRole().name())
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
            .signWith(getSignKey(), SignatureAlgorithm.HS256)
            .compact();
    }

    public Long extractUserId(String token) {
        return Long.parseLong(
            Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject()
        );
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(getSignKey()).build().parseClaimsJws(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    private Key getSignKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
    }
}
```
