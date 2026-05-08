# Мобильный интерфейс (Android)

## Требования траектории В: 5+ экранов ✅ (реализовано 7)

---

## Экраны приложения

### Экран 1: Вход в систему (LoginActivity)

**Функции:**
- Ввод email и пароля
- Кнопка «Войти»
- Переход на регистрацию
- Валидация полей
- Показ сообщения об ошибке при неверных данных

**Компоненты UI:**
- `TextInputLayout` + `TextInputEditText` для email и пароля
- `MaterialButton` — Войти
- `TextView` — ссылка на регистрацию
- `CircularProgressIndicator` во время запроса

![Экран входа](<images/screen-login.png>)

---

### Экран 2: Регистрация (RegisterActivity)

**Функции:**
- Ввод логина, email, пароля
- Валидация: логин ≥ 3 символа, email корректный, пароль ≥ 6 символов
- Переход на главный экран после успешной регистрации

![Экран регистрации](<images/screen-register.png>)

---

### Экран 3: Список папок (FolderListFragment)

**Функции:**
- Список папок с цветовой маркировкой
- FAB «+» — создать папку
- Кнопка удаления на каждой карточке
- Подтверждение удаления через AlertDialog
- Пустое состояние (иконка 📂 + «Папок пока нет»)
- Переход в папку по нажатию

**Компоненты UI:**
- `RecyclerView` + `MaterialCardView`
- `FloatingActionButton`
- `ImageButton` (корзина)

![Список папок](<images/screen-folders.png>)

---

### Экран 4: Создание папки (CreateFolderActivity)

**Функции:**
- Ввод названия папки
- Выбор цвета (4 варианта: синий, зелёный, оранжевый, фиолетовый)
- Кнопки «Сохранить» / «Отмена»
- Визуальная индикация выбранного цвета (белая обводка)

**Компоненты UI:**
- `TextInputLayout` + `TextInputEditText`
- `LinearLayout` с цветными чипами (`View` + `GradientDrawable`)
- Два `MaterialButton`

![Создание папки](<images/screen-create-folder.png>)

---

### Экран 5: Список задач в папке (FolderTasksFragment)

**Функции:**
- Заголовок = название папки
- Кнопка «Назад» в ActionBar
- Список задач с чекбоксами (отметить выполненной)
- FAB «+» — добавить задачу
- Кнопка удаления задачи
- Пустое состояние (✅ + «Задач пока нет»)

**Компоненты UI:**
- `RecyclerView` + `MaterialCardView`
- `CheckBox` для статуса
- `FloatingActionButton`

![Список задач](<images/screen-tasks.png>)

---

### Экран 6: Редактирование задачи (TaskEditFragment)

**Функции:**
- Поля: название, описание, дата, время
- Встроенный тулбар с кнопкой «Назад»
- Список подзадач с возможностью добавить новую
- Кнопка «Сохранить» — сохранить и закрыть
- Кнопка «Закрыть» — закрыть без сохранения
- `DatePickerDialog` и `TimePickerDialog` для выбора даты/времени

**Компоненты UI:**
- `MaterialToolbar`
- `ScrollView` → `LinearLayout`
- Три `TextInputLayout`
- `RecyclerView` для подзадач
- Два `MaterialButton`

![Редактор задачи](<images/screen-task-edit.png>)

---

### Экран 7: Профиль пользователя (ProfileActivity)

**Функции:**
- Отображение: полное имя, логин, email, дата регистрации, количество задач
- Редактирование полного имени
- Выход из аккаунта
- Данные берутся из кэша (`SharedPreferences`) и обновляются с сервера

![Профиль](<images/screen-profile.png>)

---

## Навигация между экранами

```
LoginActivity ──────────────────────────────────────────> RegisterActivity
      │
      ▼ (JWT получен)
MainActivity
      │
      ├── FolderListFragment (по умолчанию)
      │         │
      │         ├── [FAB] ──────────────────────────────> CreateFolderActivity
      │         │                                               │
      │         │                                               └── finish() → FolderListFragment
      │         │
      │         └── [нажатие на папку] ─────────────────> FolderTasksFragment
      │                   │
      │                   ├── [FAB] ─────────────────────> TaskEditFragment (новая задача)
      │                   │
      │                   └── [нажатие на задачу] ───────> TaskEditFragment (редактирование)
      │
      └── [меню] ─────────────────────────────────────────> ProfileActivity
```

---

## Соответствие требованиям Material Design 3

| Требование | Реализация |
|---|---|
| Цветовая схема | `colorPrimary`, `colorOnPrimary`, `colorSurface` |
| Компоненты | `MaterialButton`, `MaterialCardView`, `MaterialToolbar` |
| FAB | `FloatingActionButton` с иконкой `ic_add_24dp` |
| Поля ввода | `TextInputLayout.OutlinedBox` |
| Диалоги | `AlertDialog.Builder` из Material |
| Навигация | Back stack через `FragmentManager` |
