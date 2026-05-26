# Этап 5: Реализация ядра (Недели 11–12)

## Цель этапа

Полная реализация слоёв Entity и Foundation серверной части, реализация ключевых Use Cases в Mediator (Service), модульное тестирование с покрытием > 40%.

## Результаты

| Артефакт | Описание | Документ |
|---|---|---|
| Entity-классы | JPA-сущности с бизнес-методами | [core-entities.md](core-entities.md) |
| Сервисный слой | Бизнес-логика, транзакции | [services.md](services.md) |
| Слой доступа к данным | Репозитории, запросы | [repositories.md](repositories.md) |
| Модульные тесты | JUnit-тесты, отчёт JaCoCo | [test-coverage.md](test-coverage.md) |

---

## Структура серверного проекта

```
taskplanner-server/
├── src/main/java/com/levon/taskplanner/
│   ├── config/
│   │   ├── SecurityConfig.java         ← Spring Security + JWT-фильтр
│   │   └── OpenApiConfig.java          ← Swagger UI конфигурация
│   ├── controller/
│   │   ├── AuthController.java
│   │   ├── TaskListController.java
│   │   ├── TaskController.java
│   │   └── UserController.java
│   ├── service/
│   │   ├── UserService.java
│   │   ├── TaskListService.java
│   │   ├── TaskService.java
│   │   ├── AuthService.java
│   │   └── ReminderService.java
│   ├── security/
│   │   ├── JwtTokenProvider.java       ← генерация и валидация JWT
│   │   ├── JwtAuthenticationFilter.java
│   │   ├── UserDetailsImpl.java
│   │   └── UserDetailsServiceImpl.java
│   ├── repository/
│   │   ├── UserRepository.java
│   │   ├── TaskListRepository.java
│   │   ├── TaskRepository.java
│   │   └── ReminderRepository.java
│   ├── entity/
│   │   ├── User.java
│   │   ├── TaskList.java
│   │   ├── Task.java
│   │   ├── Reminder.java
│   │   └── enums/
│   │       ├── Priority.java           ← LOW, MEDIUM, HIGH, URGENT
│   │       ├── Role.java
│   │       ├── TaskStatus.java
│   │       └── TaskListStatus.java
│   ├── dto/
│   │   ├── LoginRequest.java
│   │   ├── RegisterRequest.java
│   │   ├── JwtResponse.java
│   │   └── UserProfileDto.java
│   └── config/
│       ├── SecurityConfig.java
│       ├── GlobalExceptionHandler.java
│       └── OpenAPIConfig.java
├── src/main/resources/
│   └── application.properties          ← БД, JWT-секрет, Swagger
└── src/test/java/com/levon/taskplanner/
    ├── service/
    │   ├── UserServiceTest.java
    │   ├── TaskListServiceTest.java
    │   └── TaskServiceTest.java
    └── repository/
        └── TaskListRepositoryTest.java
```

## Выполненные требования траектории В

| Требование | Статус |
|---|---|
| Мобильное приложение с 5+ экранами | ✅ 7 экранов |
| Серверная часть на Java (Spring Boot) | ✅ |
| REST API (8+ эндпоинтов) | ✅ 12 эндпоинтов |
| Документация OpenAPI (Swagger UI) | ✅ |
| Аутентификация через JWT | ✅ |
| Локальное кэширование (оффлайн-режим) | ✅ SQLite |
| Модульное тестирование (покрытие >40%) | ✅ ~55% |
