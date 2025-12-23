# 🚀 Команды для запуска Users Service

## Полная последовательность (пошагово)

### macOS / Linux:

```bash
# 1. Перейти в папку users-service
cd /Users/tsagoll/StudioProjects/product/users-service

# 2. Запустить PostgreSQL в Docker
docker compose up -d

# 3. Проверить статус БД (опционально)
docker compose ps

# 4. Активировать виртуальное окружение
source .venv/bin/activate

# 5. Установить зависимости (если не установлены)
pip install -r requirements.txt

# 6. Запустить API
uvicorn app.main:app --reload --port 8001
```

### Windows (PowerShell):

```powershell
# 1. Перейти в папку users-service
cd C:\path\to\product\users-service

# 2. Запустить PostgreSQL в Docker
docker compose up -d

# 3. Проверить статус БД (опционально)
docker compose ps

# 4. Активировать виртуальное окружение
.\.venv\Scripts\Activate.ps1

# 5. Установить зависимости (если не установлены)
pip install -r requirements.txt

# 6. Запустить API
uvicorn app.main:app --reload --port 8001
```

---

## 🎯 Быстрый запуск (один скрипт)

### macOS / Linux:
```bash
# Из корня users-service
./scripts/run-server.sh --reset
# Из другого терминале из папки users-service
python3 scripts/upload_avatars_via_api.py
# Или из любой папки
cd /Users/tsagoll/StudioProjects/product/users-service
./scripts/run-server.sh
```

### Windows:
```powershell
# Из корня users-service
.\scripts\run-server.ps1

# Или из любой папки
cd C:\path\to\product\users-service
.\scripts\run-server.ps1
```

---

## 🛑 Остановка

### macOS / Linux:
```bash
# Остановить API: Ctrl+C в терминале

# Остановить PostgreSQL (из корня users-service)
./scripts/stop-server.sh

# Или вручную
docker compose stop          # остановить, данные сохранятся
docker compose down          # остановить и удалить контейнеры
docker compose down -v       # удалить контейнеры + данные

# Деактивировать окружение
deactivate
```

### Windows:
```powershell
# Остановить API: Ctrl+C в терминале

# Остановить PostgreSQL
docker compose stop          # остановить, данные сохранятся
docker compose down          # остановить и удалить контейнеры
docker compose down -v       # удалить контейнеры + данные

# Деактивировать окружение
deactivate
```

---

## 🧪 Тестирование аватарок

После запуска API:

### macOS / Linux:
```bash
# Bash-скрипт (из корня users-service)
./scripts/test_avatars.sh

# Или Python-тесты
python scripts/test_avatars.py
```

### Windows:
```powershell
# Python-тесты
python scripts/test_avatars.py
```

### Ручное тестирование (curl):

```bash
# Загрузить аватарку
curl -X POST "http://localhost:8001/users/1/avatar" \
  -F "file=@avatar.jpg"

# Получить аватарку
curl "http://localhost:8001/users/1/avatar" --output avatar.jpg

# Удалить аватарку
curl -X DELETE "http://localhost:8001/users/1/avatar"
```

---

## 📡 API будет доступен на:

- **Основной URL:** http://localhost:8001
- **Swagger UI (документация):** http://localhost:8001/docs
- **ReDoc:** http://localhost:8001/redoc
- **Health check:** http://localhost:8001/health

---

## 🔧 Полезные команды Docker

```bash
# Посмотреть логи PostgreSQL
docker compose logs db

# Посмотреть логи в реальном времени
docker compose logs -f db

# Перезапустить PostgreSQL
docker compose restart db

# Подключиться к PostgreSQL через psql
docker exec -it users_pg psql -U app -d usersdb

# Сделать бэкап БД
docker exec users_pg pg_dump -U app usersdb > backup.sql

# Восстановить БД из бэкапа
cat backup.sql | docker exec -i users_pg psql -U app usersdb
```

---

## 📁 Структура после запуска

```
users-service/
├── .venv/                  # Виртуальное окружение Python (создается автоматически)
├── avatars/                # Загруженные аватарки пользователей
│   ├── .gitkeep
│   └── user_*.{jpg,png}   # Файлы аватарок (создаются при загрузке)
├── app/                    # Код приложения
├── scripts/                # 🆕 Скрипты и документация
│   ├── run-server.sh      # 🚀 Быстрый запуск (macOS/Linux)
│   ├── run-server.ps1     # 🚀 Быстрый запуск (Windows)
│   ├── stop-server.sh     # 🛑 Остановка
│   ├── test_avatars.sh    # 🧪 Тесты (bash)
│   ├── test_avatars.py    # 🧪 Тесты (Python)
│   ├── QUICK_START.md     # 📖 Это руководство
│   └── AVATARS_README.md  # 📖 Документация по аватаркам
└── sql/                    # SQL миграции
```

---

## ⚠️ Troubleshooting

### Ошибка: "docker compose" не найдена
```bash
# Попробуйте с дефисом (старая версия Docker)
docker-compose up -d
```

### Ошибка: Порт 8001 уже занят
```bash
# Найти процесс на порту 8001
lsof -ti:8001              # macOS/Linux
netstat -ano | findstr 8001  # Windows

# Убить процесс
kill -9 <PID>              # macOS/Linux
taskkill /PID <PID> /F     # Windows

# Или запустить на другом порту
uvicorn app.main:app --reload --port 8002
```

### Ошибка: Порт 5432 уже занят (PostgreSQL)
```bash
# Остановить локальный PostgreSQL
sudo service postgresql stop    # Linux
brew services stop postgresql   # macOS

# Или изменить порт в docker-compose.yml
ports:
  - "5433:5432"  # Вместо 5432:5432
```

### Ошибка: "command not found: uvicorn"
```bash
# Убедитесь, что виртуальное окружение активировано
source .venv/bin/activate       # macOS/Linux
.\.venv\Scripts\Activate.ps1    # Windows

# Переустановите зависимости
pip install -r requirements.txt
```

### БД не инициализируется (таблицы не создаются)
```bash
# Удалить и пересоздать БД
docker compose down -v
docker compose up -d

# Проверить логи
docker compose logs db
```

---

## 📚 Дополнительная документация

- [README.md](../README.md) — Основная документация
- [AVATARS_README.md](AVATARS_README.md) — Подробно про систему аватарок
- [sql/](../sql/) — SQL миграции и схема БД
