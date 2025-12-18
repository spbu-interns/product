#!/bin/bash
# Скрипт для быстрого запуска всего сервиса
# Использование:
#   ./scripts/run-server.sh          # Обычный запуск
#   ./scripts/run-server.sh --reset  # Полный сброс БД

set -e  # Остановить при ошибке

echo "🚀 Запуск Users Service"
echo "======================================"

# Перейти в корень users-service (родительская папка от scripts/)
cd "$(dirname "$0")/.."

# Проверка флага --reset
RESET_DB=false
if [ "$1" == "--reset" ]; then
    RESET_DB=true
    echo ""
    echo "⚠️  РЕЖИМ: Полный сброс базы данных"
    echo "======================================"
fi

# 1. Остановка и очистка (если --reset)
if [ "$RESET_DB" = true ]; then
    echo ""
    echo "1️⃣  Остановка и удаление старой БД..."
    docker compose down -v
    echo "   ✅ Старые данные удалены"
fi

# 2. Запуск PostgreSQL
echo ""
if [ "$RESET_DB" = true ]; then
    echo "2️⃣  Запуск PostgreSQL в Docker..."
else
    echo "1️⃣  Запуск PostgreSQL в Docker..."
fi
docker compose up -d

# Ожидание готовности БД с улучшенной проверкой
echo "   Ожидание готовности PostgreSQL..."
MAX_ATTEMPTS=30
ATTEMPT=1

while ! docker exec users_pg pg_isready -U userdb -d userdb > /dev/null 2>&1; do
    if [ $ATTEMPT -ge $MAX_ATTEMPTS ]; then
        echo "   ❌ База данных не запустилась за $MAX_ATTEMPTS секунд"
        docker compose logs
        exit 1
    fi
    echo "   Попытка $ATTEMPT/$MAX_ATTEMPTS..."
    sleep 1
    ATTEMPT=$((ATTEMPT + 1))
done

echo "   ✅ PostgreSQL запущен и готов"

# Проверка, первый ли это запуск (пустая БД) или режим --reset
USER_COUNT=$(docker exec users_pg psql -U userdb -d userdb -t -c "SELECT COUNT(*) FROM users WHERE email LIKE '%@test.com';" 2>/dev/null | tr -d ' ' || echo "0")

if [ "$USER_COUNT" = "0" ] || [ "$RESET_DB" = true ]; then
    echo ""
    if [ "$RESET_DB" = true ]; then
        echo "3️⃣  Применение SQL миграций..."
    else
        echo "📊 Обнаружена пустая БД, применяю все миграции..."
    fi
    
    for sql_file in $(ls sql/*.sql | sort); do
        filename=$(basename "$sql_file")
        echo "   Применяю $filename..."
        docker exec -i users_pg psql -U userdb -d userdb < "$sql_file" > /dev/null 2>&1 || true
    done
    
    echo "   ✅ Миграции применены"
    
    # Статистика созданных данных
    if [ "$RESET_DB" = true ]; then
        echo ""
        echo "4️⃣  Статистика созданных данных:"
        
        CLINICS_COUNT=$(docker exec users_pg psql -U userdb -d userdb -t -c "SELECT COUNT(*) FROM clinics;" 2>/dev/null | tr -d ' ' || echo "0")
        USERS_COUNT=$(docker exec users_pg psql -U userdb -d userdb -t -c "SELECT COUNT(*) FROM users;" 2>/dev/null | tr -d ' ' || echo "0")
        DOCTORS_COUNT=$(docker exec users_pg psql -U userdb -d userdb -t -c "SELECT COUNT(*) FROM doctors;" 2>/dev/null | tr -d ' ' || echo "0")
        CLIENTS_COUNT=$(docker exec users_pg psql -U userdb -d userdb -t -c "SELECT COUNT(*) FROM clients;" 2>/dev/null | tr -d ' ' || echo "0")
        
        echo "   📊 Клиники: $CLINICS_COUNT | Доктора: $DOCTORS_COUNT | Клиенты: $CLIENTS_COUNT | Всего пользователей: $USERS_COUNT"
    fi
    
    # Подсказка о загрузке аватарок
    if [ -d "test_avatars" ]; then
        echo ""
        echo "💡 Для загрузки тестовых аватарок запустите после старта API:"
        echo "   python scripts/upload_avatars_via_api.py"
    fi
else
    echo "   ℹ️  БД уже содержит данные ($USER_COUNT тестовых пользователей)"

    echo ""
    echo "📊 Обновляю схему (без повторной загрузки тестовых данных)..."

    for sql_file in $(ls sql/*.sql | sort); do
        filename=$(basename "$sql_file")

        # Пропускаем тестовые данные, чтобы не дублировать записи
        if [ "$filename" = "016_test_data.sql" ]; then
            continue
        fi

        echo "   Применяю $filename..."
        docker exec -i users_pg psql -U userdb -d userdb < "$sql_file" > /dev/null 2>&1 || true
    done

    echo "   ✅ Схема актуализирована"
fi

# 2. Активация виртуального окружения
echo ""
echo "2️⃣  Активация виртуального окружения..."
if [ ! -d ".venv" ]; then
    echo "   ⚠️  Виртуальное окружение не найдено. Создаю..."
    python3 -m venv .venv
fi

source .venv/bin/activate
echo "   ✅ Виртуальное окружение активировано"

# 3. Установка/обновление зависимостей
echo ""
echo "3️⃣  Проверка зависимостей..."
pip install -q -r requirements.txt
echo "   ✅ Зависимости установлены"

# 4. Создание папки avatars (если нет)
if [ ! -d "avatars" ]; then
    mkdir -p avatars
    echo "   ✅ Создана папка avatars/"
fi

# 5. Запуск API
echo ""
echo "4️⃣  Запуск API сервера..."
echo "======================================"
echo ""
echo "📡 API будет доступен на:"
echo "   http://localhost:8001"
echo "   http://localhost:8001/docs (Swagger UI)"
echo ""
echo "🛑 Для остановки нажмите Ctrl+C"
echo ""

uvicorn app.main:app --reload --port 8001
