Документация API
API аутентификации
Базовый URL
http://localhost:8000

Конечные точки (Endpoints)

1. Вход пользователя
**Конечная точка:** POST /api/auth/login

**Описание:** Аутентифицирует пользователя и возвращает токен сессии.

**Тело запроса:**
```json
{
  "email": "user@example.com",
  "password": "userPassword123",
  "account_type": "Пациент" | "Врач" | "Администратор"
}
```

**Ответ (Успех - 200 OK):**
```json
{
  "success": true,
  "data": {
    "token": "base64EncodedToken",
    "user_id": 123,
    "email": "user@example.com",
    "account_type": "CLIENT" | "DOCTOR" | "ADMIN",
    "first_name": "John",
    "last_name": "Doe"
  }
}
```

**Ответ (Ошибка - 401 Unauthorized):**
```json
{
  "success": false,
  "error": "Неверный email или пароль"
}
```

2. Регистрация пользователя
**Конечная точка:** POST /api/auth/register

**Описание:** Регистрирует нового пользователя и запускает процесс подтверждения email.

**Тело запроса:**
```json
{
  "email": "newuser@example.com",
  "password": "securePassword123",
  "account_type": "Пациент" | "Врач" | "Администратор"
}
```

**Ответ (Успех - 201 Created):**
```json
{
  "success": true,
  "data": {
    "success": true,
    "message": "Пользователь успешно зарегистрирован. Пожалуйста, проверьте вашу электронную почту для подтверждения.",
    "user_id": 123
  }
}
```

**Ответ (Ошибка - 500 Internal Server Error):**
```json
{
  "success": false,
  "error": "Не удалось зарегистрировать пользователя: [сообщение об ошибке]"
}
```

3. Запуск подтверждения Email
**Конечная точка:** POST /api/auth/email/start

**Описание:** Инициирует или повторно отправляет процесс подтверждения email.

**Тело запроса:**
```json
{
  "email": "user@example.com"
}
```

**Ответ (Успех - 200 OK):**
```json
{
  "success": true,
  "data": {
    "success": true,
    "message": "Письмо с подтверждением отправлено"
  }
}
```

**Ответ (Ошибка - 404 Not Found):**
```json
{
  "success": false,
  "error": "Пользователь не найден или уже подтвержден"
}
```

4. Подтверждение Email
**Конечная точка:** POST /api/auth/email/verify

**Описание:** Подтверждает email пользователя с использованием токена, полученного по email.

**Тело запроса:**
```json
{
  "token": "verification_token"
}
```

**Ответ (Успех - 200 OK):**
```json
{
  "success": true,
  "data": {
    "success": true,
    "message": "Email успешно подтвержден"
  }
}
```

**Ответ (Ошибка - 400 Bad Request):**
```json
{
  "success": false,
  "error": "Неверный или просроченный токен"
}
```

5. Запрос сброса пароля
**Конечная точка:** POST /api/auth/password/forgot

**Описание:** Инициирует процесс сброса пароля, отправляя ссылку для сброса по email.

**Тело запроса:**
```json
{
  "email": "user@example.com"
}
```

**Ответ (Всегда 200 OK в целях безопасности):**
```json
{
  "success": true,
  "data": {
    "success": true,
    "message": "Если электронная почта существует, ссылка для сброса пароля была отправлена"
  }
}
```

6. Сброс пароля
**Конечная точка:** POST /api/auth/password/reset

**Описание:** Сбрасывает пароль пользователя с использованием токена, полученного по email.

**Тело запроса:**
```json
{
  "token": "reset_token",
  "new_password": "newSecurePassword123"
}
```

**Ответ (Успех - 200 OK):**
```json
{
  "success": true,
  "data": {
    "success": true,
    "message": "Пароль успешно изменен"
  }
}
```

**Ответ (Ошибка - 400 Bad Request):**
```json
{
  "success": false,
  "error": "Неверный или просроченный токен"
}
```