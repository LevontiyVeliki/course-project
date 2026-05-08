# Руководство администратора

## Инструкция по развёртыванию серверной части

---

## 1. Системные требования

| Компонент | Минимальные требования |
|---|---|
| ОС | Ubuntu 20.04+ / Windows 10+ / macOS 12+ |
| JDK | OpenJDK 17 или Oracle JDK 17 |
| PostgreSQL | 14 или выше |
| RAM | 512 МБ свободной памяти |
| Диск | 200 МБ для приложения + место для БД |
| Сеть | Порт 8080 (или настраиваемый) |

---

## 2. Установка зависимостей

### JDK 17

```bash
# Ubuntu/Debian
sudo apt update
sudo apt install openjdk-17-jdk

# Проверка
java -version
# openjdk version "17.0.X"
```

### PostgreSQL

```bash
# Ubuntu/Debian
sudo apt install postgresql postgresql-contrib

# Запуск сервиса
sudo systemctl start postgresql
sudo systemctl enable postgresql
```

---

## 3. Настройка базы данных

```bash
# Войти в PostgreSQL
sudo -u postgres psql

# Создать базу данных и пользователя
CREATE DATABASE taskplanner;
CREATE USER tp_user WITH ENCRYPTED PASSWORD 'strong_password_here';
GRANT ALL PRIVILEGES ON DATABASE taskplanner TO tp_user;
\q
```

```bash
# Применить DDL-схему
psql -U tp_user -d taskplanner -h localhost -f ddl.sql
```

---

## 4. Конфигурация приложения

Создайте файл `application.properties` (или используйте переменные окружения):

```properties
# Подключение к БД
spring.datasource.url=jdbc:postgresql://localhost:5432/taskplanner
spring.datasource.username=tp_user
spring.datasource.password=strong_password_here

# JPA
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false

# JWT (ОБЯЗАТЕЛЬНО замените на случайную строку длиной 32+ символа)
jwt.secret=ReplaceWithYourRandomSecretKeyAtLeast32Chars
jwt.expiration=86400000

# Сервер
server.port=8080

# Swagger (в продакшене рекомендуется отключить)
springdoc.swagger-ui.enabled=true
springdoc.api-docs.enabled=true
```

---

## 5. Запуск приложения

```bash
# Запуск из JAR
java -jar taskplanner-server-1.0.0.jar

# Или с явными параметрами (переопределяют application.properties)
java -jar taskplanner-server-1.0.0.jar \
  --spring.datasource.password=your_password \
  --jwt.secret=your_secret_key
```

### Проверка запуска

```bash
curl http://localhost:8080/actuator/health
# {"status":"UP"}
```

---

## 6. Управление пользователями

### Просмотр пользователей (через psql)

```sql
SELECT id, username, email, role, created_at FROM users ORDER BY created_at DESC;
```

### Назначить роль ADMIN

```sql
UPDATE users SET role = 'ADMIN' WHERE email = 'admin@example.com';
```

### Удалить пользователя

```sql
DELETE FROM users WHERE email = 'user@example.com';
-- Автоматически удаляет все папки и задачи (ON DELETE CASCADE)
```

---

## 7. Мониторинг и логи

### Просмотр логов

```bash
# При запуске через JAR — вывод в консоль
java -jar taskplanner-server.jar > server.log 2>&1 &

# Просмотр хвоста лога
tail -f server.log
```

### Ключевые записи в логах

| Сообщение | Значение |
|---|---|
| `Started TaskPlannerApplication` | Сервер успешно запущен |
| `HikariPool-1 - Start completed` | Пул БД-соединений инициализирован |
| `JWT token validated for user ID: X` | Успешная аутентификация |
| `AccessDeniedException` | Попытка обратиться к чужим данным |

---

## 8. Резервное копирование БД

```bash
# Создать дамп
pg_dump -U tp_user -d taskplanner -f backup_$(date +%Y%m%d).sql

# Восстановить из дампа
psql -U tp_user -d taskplanner -f backup_20260508.sql
```

---

## 9. Обновление приложения

1. Остановить текущий процесс: `kill $(lsof -t -i:8080)`
2. Заменить JAR-файл новой версией
3. При изменении схемы БД — применить миграционный SQL-скрипт
4. Запустить новую версию

---

## 10. Типичные проблемы

| Проблема | Причина | Решение |
|---|---|---|
| `Connection refused` на порту 5432 | PostgreSQL не запущен | `sudo systemctl start postgresql` |
| `401 Unauthorized` на все запросы | Неверный JWT-секрет | Проверить `jwt.secret` в конфиге |
| `Schema-validation: missing table` | DDL не применён | Запустить `ddl.sql` |
| Приложение не запускается | Порт 8080 занят | Изменить `server.port` или освободить порт |
