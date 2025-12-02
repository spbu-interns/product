# Система хранения аватарок пользователей

## Описание

Аватарки пользователей хранятся как **файлы на диске** в папке `avatars/`. В базе данных хранится только относительный путь к файлу.

## Структура

```
users-service/
├── avatars/                    # Папка с аватарками
│   ├── user_1_a1b2c3d4.jpg   # Формат: user_{id}_{hash}.{ext}
│   ├── user_2_e5f6g7h8.png
│   └── ...
├── app/
│   └── main.py                # API endpoints для работы с аватарками
└── sql/
    └── 001_schema.sql         # users.avatar — TEXT (путь к файлу)
```

## Поддерживаемые форматы

- **JPEG/JPG** — `.jpg`, `.jpeg`
- **PNG** — `.png`
- **WebP** — `.webp`
- **GIF** — `.gif`

## Ограничения

- **Максимальный размер файла:** 5 MB
- **Максимальное разрешение:** 2048×2048 пикселей
- **Автоматическая оптимизация:** изображения сжимаются при загрузке (quality=85)
- **Автоматическое изменение размера:** если изображение превышает 2048px по любой стороне

## API Endpoints

### 1. Загрузка аватарки

```http
POST /users/{user_id}/avatar
Content-Type: multipart/form-data

Form Data:
  file: <image_file>
```

**Пример (curl):**
```bash
curl -X POST "http://localhost:8001/users/1/avatar" \
  -F "file=@/path/to/avatar.jpg"
```

**Ответ:**
```json
{
  "message": "avatar uploaded successfully",
  "avatar_url": "/users/1/avatar",
  "filename": "user_1_a1b2c3d4.jpg"
}
```

**Особенности:**
- Автоматически удаляет старую аватарку
- Оптимизирует изображение (сжатие, изменение размера)
- Конвертирует RGBA → RGB для JPEG
- Генерирует уникальное имя файла

### 2. Получение аватарки

```http
GET /users/{user_id}/avatar
```

**Пример (curl):**
```bash
curl "http://localhost:8001/users/1/avatar" --output avatar.jpg
```

**Ответ:**
- Файл изображения с правильным MIME-типом
- `404` если аватарка не установлена

**Использование в HTML:**
```html
<img src="http://localhost:8001/users/1/avatar" alt="User Avatar">
```

### 3. Удаление аватарки

```http
DELETE /users/{user_id}/avatar
```

**Пример (curl):**
```bash
curl -X DELETE "http://localhost:8001/users/1/avatar"
```

**Ответ:**
- `204 No Content` — успешно удалено
- `404` — пользователь не найден

## Хранение в базе данных

В таблице `users` поле `avatar` хранит **относительный путь**:

```sql
-- Пример значения в БД
avatar = 'avatars/user_1_a1b2c3d4.jpg'
```

Это позволяет:
- Легко переносить файлы между серверами
- Менять расположение папки `avatars/`
- Минимизировать размер БД

## Docker

При запуске API в Docker нужно примонтировать volume для сохранения аватарок:

```yaml
# docker-compose.yml
services:
  api:
    build: .
    volumes:
      - ./avatars:/app/avatars  # Монтирование папки с аватарками
```

## Примеры использования

### Python (requests)

```python
import requests

# Загрузка аватарки
with open('avatar.jpg', 'rb') as f:
    response = requests.post(
        'http://localhost:8001/users/1/avatar',
        files={'file': f}
    )
print(response.json())

# Получение аватарки
response = requests.get('http://localhost:8001/users/1/avatar')
with open('downloaded_avatar.jpg', 'wb') as f:
    f.write(response.content)

# Удаление аватарки
requests.delete('http://localhost:8001/users/1/avatar')
```

### JavaScript (Fetch API)

```javascript
// Загрузка аватарки
const formData = new FormData();
formData.append('file', fileInput.files[0]);

fetch('http://localhost:8001/users/1/avatar', {
  method: 'POST',
  body: formData
})
.then(res => res.json())
.then(data => console.log(data));

// Отображение аватарки
const avatarUrl = 'http://localhost:8001/users/1/avatar';
document.getElementById('avatar').src = avatarUrl;

// Удаление аватарки
fetch('http://localhost:8001/users/1/avatar', {
  method: 'DELETE'
});
```

## Безопасность

### Реализованные меры:

1. **Валидация типа файла** — проверка расширения
2. **Валидация размера** — максимум 5 MB
3. **Проверка формата** — PIL открывает и проверяет изображение
4. **Уникальные имена** — UUID предотвращает конфликты
5. **Автоматическая очистка** — старая аватарка удаляется

### Рекомендуется добавить:

- [ ] Аутентификацию (только владелец может менять свою аватарку)
- [ ] Rate limiting (ограничение частоты загрузок)
- [ ] Антивирусную проверку файлов
- [ ] Проверку EXIF-данных (удаление метаданных)

## Миграция существующих данных

Если у вас уже есть аватарки в формате URL или base64, нужно их мигрировать:

```python
# Пример скрипта миграции
import requests
import base64
from pathlib import Path

def migrate_avatar(user_id, old_avatar_data):
    # Если это URL
    if old_avatar_data.startswith('http'):
        response = requests.get(old_avatar_data)
        image_data = response.content
    
    # Если это base64
    elif old_avatar_data.startswith('data:image'):
        header, encoded = old_avatar_data.split(',', 1)
        image_data = base64.b64decode(encoded)
    
    # Загрузка через API
    files = {'file': ('avatar.jpg', image_data, 'image/jpeg')}
    requests.post(f'http://localhost:8001/users/{user_id}/avatar', files=files)
```

## Troubleshooting

### Ошибка: "Invalid file type"
- Проверьте расширение файла
- Поддерживаются только: .jpg, .jpeg, .png, .webp, .gif

### Ошибка: "File too large"
- Максимальный размер: 5 MB
- Используйте сжатие перед загрузкой

### Ошибка: "Invalid image file"
- Файл поврежден или не является изображением
- Попробуйте пересохранить файл в графическом редакторе

### Аватарка не отображается (404)
- Проверьте существование файла в `avatars/`
- Проверьте значение поля `users.avatar` в БД
- Убедитесь, что путь начинается с `avatars/`

## Производительность

- **Оптимизация при загрузке:** ~50-200ms (зависит от размера файла)
- **Отдача через FastAPI:** ~5-20ms
- **Кэширование:** рекомендуется настроить nginx для отдачи статики

### Настройка nginx (опционально)

```nginx
location /users/ {
    # API endpoint
    proxy_pass http://localhost:8001;
}

location /avatars/ {
    # Прямая отдача статики
    alias /path/to/users-service/avatars/;
    expires 7d;
    add_header Cache-Control "public, immutable";
}
```

## Резервное копирование

Не забывайте делать backup папки `avatars/`:

```bash
# Создание архива
tar -czf avatars_backup_$(date +%Y%m%d).tar.gz avatars/

# Восстановление
tar -xzf avatars_backup_20251202.tar.gz
```
