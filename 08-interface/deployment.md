# Руководство по развёртыванию

## Системные требования

| Компонент | Требование |
|---|---|
| JDK | 17 или выше |
| PostgreSQL | 14 или выше |
| Android | API 26+ (Android 8.0) |
| Android Studio | Hedgehog 2023.1+ |
| Maven | 3.8+ |
| RAM (сервер) | 512 MB минимум |

---

## 1. Настройка базы данных

```sql
-- Создать базу данных
CREATE DATABASE taskplanner;

-- Создать пользователя (опционально)
CREATE USER taskplanner_user WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE taskplanner TO taskplanner_user;
```

Выполнить DDL-скрипт:

```bash
psql -U postgres -d taskplanner -f 04-database/ddl.sql
```

---

## 2. Настройка серверной части

### application.properties

```properties
# База данных
spring.datasource.url=jdbc:postgresql://localhost:5432/taskplanner
spring.datasource.username=postgres
spring.datasource.password=your_password
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA / Hibernate
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# JWT
jwt.secret=YourBase64EncodedSecretKeyAtLeast32CharactersLong
jwt.expiration=86400000

# Swagger UI
springdoc.swagger-ui.path=/swagger-ui/index.html
springdoc.api-docs.path=/v3/api-docs

# Сервер
server.port=8080
```

### Сборка и запуск

```bash
# Перейти в директорию сервера
cd taskplanner-server

# Собрать JAR
mvn clean package -DskipTests

# Запустить
java -jar target/taskplanner-server-1.0.0.jar
```

### Проверка работоспособности

```bash
# Сервер запущен
curl http://localhost:8080/actuator/health
# {"status":"UP"}

# Swagger UI
# Открыть в браузере: http://localhost:8080/swagger-ui/index.html
```

---

## 3. Настройка мобильного приложения

### Изменить базовый URL

```kotlin
// RetrofitClient.kt
// Текущий продакшн-URL (Railway):
const val BASE_URL = "https://taskplanner-server.up.railway.app/"

// Для локальной разработки:
// const val BASE_URL = "http://10.0.2.2:8080/"   // эмулятор
// const val BASE_URL = "http://192.168.1.X:8080/" // реальное устройство
```

### Сборка APK

1. Открыть проект в Android Studio
2. `Build → Build Bundle(s)/APK(s) → Build APK(s)`
3. APK находится по пути: `app/build/outputs/apk/debug/app-debug.apk`

### Установка на устройство

```bash
# Через ADB
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## 4. Проверка полной интеграции

| Шаг | Действие | Ожидаемый результат |
|---|---|---|
| 1 | Запустить PostgreSQL | БД принимает подключения |
| 2 | Запустить сервер | `{"status":"UP"}` на /actuator/health |
| 3 | Открыть приложение | Экран входа |
| 4 | Зарегистрироваться | Переход на главный экран |
| 5 | Создать папку | Папка появляется в списке |
| 6 | Создать задачу | Задача появляется в папке |
| 7 | Отключить интернет | Данные доступны из кэша |
| 8 | Включить интернет | Синхронизация происходит автоматически |

---

## 5. Конфигурация переменных окружения (для продакшена)

```bash
export DB_URL=jdbc:postgresql://db-host:5432/taskplanner
export DB_USERNAME=taskplanner_user
export DB_PASSWORD=secure_password
export JWT_SECRET=YourProductionSecretKey

java -jar taskplanner-server.jar \
  --spring.datasource.url=$DB_URL \
  --spring.datasource.username=$DB_USERNAME \
  --spring.datasource.password=$DB_PASSWORD \
  --jwt.secret=$JWT_SECRET
```
