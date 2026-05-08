# Журнал рефакторинга

Хронологическая запись изменений, внесённых в ходе этапа рефакторинга.

---

## RF-001: Введение Data Mapper — разделение Entity и DTO

**Изменённые файлы:**
- Добавлены: `TaskListMapper.java`, `TaskMapper.java`, `UserMapper.java`
- Обновлены: `TaskListService.java`, `TaskService.java`, `UserService.java`

**Суть:** Убрано прямое использование Entity-классов в ответах контроллеров. Введены DTO-records для входящих запросов и исходящих ответов.

**До:**
```java
return taskListRepository.save(list); // возвращает Entity напрямую
```
**После:**
```java
return taskListMapper.toResponse(taskListRepository.save(list));
```

---

## RF-002: Замена `.get()` на `.orElseThrow()`

**Изменённые файлы:** `TaskListService.java`, `TaskService.java`, `UserService.java`

**Суть:** Все вызовы `Optional.get()` заменены на `.orElseThrow()` с информативным сообщением об ошибке.

**До:**
```java
User user = userRepository.findById(userId).get();
```
**После:**
```java
User user = userRepository.findById(userId)
    .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: " + userId));
```

---

## RF-003: Вынесение JWT-секрета в конфигурацию

**Изменённые файлы:** `JwtService.java`, `application.properties`

**Суть:** Секретный ключ JWT перенесён из исходного кода в файл конфигурации (и далее в переменную окружения для продакшена).

```properties
# application.properties
jwt.secret=${JWT_SECRET:defaultDevSecretKey32CharactersLong}
jwt.expiration=86400000
```

---

## RF-004: Единый GlobalExceptionHandler

**Добавлено:** `GlobalExceptionHandler.java`

**Суть:** Все исключения обрабатываются централизованно через `@RestControllerAdvice`. Клиент всегда получает JSON с полем `message`.

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(EntityNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuth(AuthenticationException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
            .map(err -> err.getField() + ": " + err.getDefaultMessage())
            .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(new ErrorResponse(msg));
    }
}
```

---

## RF-005: Оптимизация N+1 запросов в TaskListRepository

**Изменённые файлы:** `TaskListRepository.java`

**Суть:** При загрузке папок с задачами добавлен JOIN FETCH для предотвращения N+1.

```java
@Query("SELECT tl FROM TaskList tl LEFT JOIN FETCH tl.tasks " +
       "WHERE tl.user.id = :userId ORDER BY tl.createdAt DESC")
List<TaskList> findByUserIdWithTasks(@Param("userId") Long userId);
```

---

## RF-006: Рефакторинг Android ViewModel

**Изменённые файлы:** `FolderViewModel.kt`, `TaskViewModel.kt`

**Суть:** Убрано дублирование логики синхронизации. Выделен общий механизм обработки ошибок сети в базовый класс.

```kotlin
// BaseViewModel.kt — общий обработчик ошибок сети
abstract class BaseViewModel(application: Application) : AndroidViewModel(application) {
    protected fun launchWithRetry(block: suspend () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                block()
            } catch (e: IOException) {
                // работаем в оффлайн-режиме, данные уже в SQLite
            } catch (e: HttpException) {
                // логируем серверную ошибку
            }
        }
    }
}
```
