# Безопасность системы

## JWT-аутентификация

### Схема работы

```
Клиент (Android)                    Сервер (Spring Boot)
       |                                    |
       | POST /api/auth/login               |
       | {email, password}                  |
       |----------------------------------->|
       |                                    | 1. findByEmail()
       |                                    | 2. BCrypt.matches(raw, hash)
       |                                    | 3. generateJwt(userId, role)
       |<-----------------------------------|
       | {token: "eyJ..."}                  |
       |                                    |
       | GET /api/task-lists                |
       | Authorization: Bearer eyJ...       |
       |----------------------------------->|
       |                                    | 4. JwtFilter.doFilter()
       |                                    | 5. validateToken()
       |                                    | 6. extractUserId()
       |                                    | 7. setAuthentication()
       |<-----------------------------------|
       | 200 OK + данные                    |
```

### Структура JWT-токена

```json
// Header
{ "alg": "HS256", "typ": "JWT" }

// Payload
{
  "sub": "1",           // userId
  "email": "j@j.com",
  "role": "USER",
  "iat": 1715000000,    // issued at
  "exp": 1715086400     // expires (24h)
}
```

### Spring Security конфигурация

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();  // сила хеширования: 10 раундов
    }
}
```

---

## Хранение паролей (BCrypt)

- Алгоритм: BCrypt с cost factor = 10
- Каждый пароль хешируется с уникальной солью
- При аутентификации: `BCryptPasswordEncoder.matches(rawPassword, storedHash)`
- Исходный пароль НИКОГДА не хранится и не передаётся по сети после регистрации

---

## Разграничение доступа (роли)

| Роль | Возможности |
|---|---|
| `USER` | CRUD своих папок и задач, просмотр своего профиля |
| `ADMIN` | Все права USER + управление пользователями |

Проверка владельца:

```java
// Пример: папка принадлежит только владельцу
Optional<TaskList> findByIdAndUserId(Long id, Long userId);
// Если Optional.empty() — AccessDeniedException → HTTP 403
```

---

## Хранение токена на клиенте (Android)

```kotlin
// SessionManager.kt — токен в SharedPreferences
class SessionManager(context: Context) {
    private val prefs = context.getSharedPreferences("task_planner_prefs", Context.MODE_PRIVATE)

    fun saveToken(token: String) = prefs.edit().putString("jwt_token", token).apply()
    fun getToken(): String? = prefs.getString("jwt_token", null)
    fun isLoggedIn(): Boolean = getToken() != null
    fun clearSession() = prefs.edit().clear().apply()
}
```

**Рекомендация для продакшена:** использовать `EncryptedSharedPreferences` для хранения токена в зашифрованном виде.

---

## CORS-конфигурация

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOriginPatterns(List.of("*"));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", config);
    return source;
}
```
