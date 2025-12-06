# Документация по аватаркам

## 📸 Загрузка тестовых аватарок для пользователей

После создания тестовых данных через `sql/016_test_data.sql`, аватарки нужно загрузить отдельно через API.

### Быстрый старт с аватарками

```bash
# 1. Запустить БД и API
./scripts/run-server.sh

# 2. В ДРУГОМ терминале загрузить аватарки
python3 scripts/upload_avatars_via_api.py
```

### Полная пересборка с нуля

```bash
# Остановить все и удалить данные
docker compose down -v

# Запустить с чистого листа  
./scripts/run-server.sh

# После запуска API, в другом терминале:
python3 scripts/upload_avatars_via_api.py
```

### Маппинг аватарок на пользователей

```
test-avatar-1.jpg -> user_id=1  (admin_ivanov)
test-avatar-2.jpg -> user_id=2  (dr_petrov)
test-avatar-3.jpg -> user_id=3  (dr_sidorova)
test-avatar-4.jpg -> user_id=4  (dr_kuznetsov)
test-avatar-5.jpg -> user_id=5  (dr_volkova)
test-avatar-6.jpg -> user_id=6  (dr_sokolov)
test-avatar-7.jpg -> user_id=7,9 (maria_ivanova, olga_popova)
test-avatar-8.jpg -> user_id=8,10 (alex_smirnov, dmitry_novikov)
test-avatar-9.jpg -> user_id=11,12 (elena_fedorova, sergey_kozlov)
```

### Проверка

```bash
# Через API
curl http://localhost:8001/users/1/avatar -o /tmp/avatar.jpg

# Через браузер
open http://localhost:8001/users/1/avatar
```

### Ручная загрузка

```bash
curl -X POST "http://localhost:8001/users/1/avatar" -F "file=@photo.jpg"
```

## API Endpoints

- **POST** `/users/{user_id}/avatar` - Загрузить аватарку (max 5MB, auto-optimize)
- **GET** `/users/{user_id}/avatar` - Получить аватарку
- **DELETE** `/users/{user_id}/avatar` - Удалить аватарку

Пароль для всех тестовых пользователей: `password123`
