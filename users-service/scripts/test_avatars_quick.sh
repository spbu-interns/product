#!/bin/bash
# Быстрый тест аватарок с реальными пользователями из БД

API_BASE="http://localhost:8001"

echo "🧪 Быстрый тест системы аватарок"
echo "======================================"

# Проверка API
echo ""
echo "Проверка доступности API..."
if ! curl -s "$API_BASE/health" > /dev/null 2>&1; then
    echo "❌ API недоступен. Запустите: ./scripts/run-server.sh"
    exit 1
fi
echo "✅ API доступен"

# Получение списка пользователей
echo ""
echo "Получение списка пользователей..."
USERS=$(curl -s "$API_BASE/users")
echo "Найдено пользователей: $(echo "$USERS" | python3 -c "import sys, json; print(len(json.load(sys.stdin)))")"

# Показываем первых 3 пользователей
echo ""
echo "Первые 3 пользователя:"
echo "$USERS" | python3 -c "
import sys, json
users = json.load(sys.stdin)[:3]
for u in users:
    print(f\"  ID: {u['id']}, Login: {u['login']}, Role: {u['role']}, Email: {u['email']}\")
"

# Создаем тестовое изображение (1x1 красный пиксель PNG)
echo ""
echo "Создание тестового изображения..."
# Используем base64 для создания минимального PNG
echo "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8DwHwAFBQIAX8jx0gAAAABJRU5ErkJggg==" | base64 -d > /tmp/test_avatar.png
echo "✅ test_avatar.png создан"

# Тестируем с первым пользователем (обычно alice, ID=1)
USER_ID=1
echo ""
echo "======================================"
echo "Тест с пользователем ID=$USER_ID"
echo "======================================"

# 1. Загрузка аватарки
echo ""
echo "1️⃣  Загрузка аватарки..."
UPLOAD_RESPONSE=$(curl -s -X POST "$API_BASE/users/$USER_ID/avatar" \
    -F "file=@/tmp/test_avatar.png")

if echo "$UPLOAD_RESPONSE" | grep -q "avatar uploaded successfully"; then
    echo "✅ Аватарка загружена"
    echo "$UPLOAD_RESPONSE" | python3 -c "import sys, json; print('   Файл:', json.load(sys.stdin)['filename'])"
else
    echo "❌ Ошибка загрузки:"
    echo "$UPLOAD_RESPONSE" | python3 -m json.tool
fi

# 2. Проверка в БД
echo ""
echo "2️⃣  Проверка в БД..."
USER_INFO=$(curl -s "$API_BASE/users/$USER_ID/profile")
AVATAR_PATH=$(echo "$USER_INFO" | python3 -c "import sys, json; u=json.load(sys.stdin); print(u.get('avatar', 'null'))")
echo "   Avatar path в БД: $AVATAR_PATH"

# 3. Получение аватарки
echo ""
echo "3️⃣  Получение аватарки через API..."
HTTP_CODE=$(curl -s -o /tmp/downloaded_avatar.png -w "%{http_code}" "$API_BASE/users/$USER_ID/avatar")

if [ "$HTTP_CODE" = "200" ]; then
    echo "✅ Аватарка получена (HTTP $HTTP_CODE)"
    ls -lh /tmp/downloaded_avatar.png
else
    echo "❌ Ошибка получения (HTTP $HTTP_CODE)"
fi

# 4. Проверка файла на диске
echo ""
echo "4️⃣  Проверка файла в папке avatars/..."
AVATAR_COUNT=$(ls -1 avatars/ 2>/dev/null | grep -v ".gitkeep" | wc -l | tr -d ' ')
if [ "$AVATAR_COUNT" -gt 0 ]; then
    echo "✅ Найдено файлов: $AVATAR_COUNT"
    echo "   Файлы:"
    ls -lh avatars/ | grep -v ".gitkeep" | grep -v "^total"
else
    echo "⚠️  Файлов не найдено"
fi

# 5. Удаление аватарки
echo ""
echo "5️⃣  Удаление аватарки..."
DELETE_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE "$API_BASE/users/$USER_ID/avatar")

if [ "$DELETE_CODE" = "204" ]; then
    echo "✅ Аватарка удалена (HTTP $DELETE_CODE)"
else
    echo "❌ Ошибка удаления (HTTP $DELETE_CODE)"
fi

# 6. Проверка, что файл удален
echo ""
echo "6️⃣  Проверка удаления..."
AVATAR_COUNT_AFTER=$(ls -1 avatars/ 2>/dev/null | grep -v ".gitkeep" | wc -l | tr -d ' ')
if [ "$AVATAR_COUNT_AFTER" -eq 0 ]; then
    echo "✅ Файл удален из папки avatars/"
else
    echo "⚠️  Осталось файлов: $AVATAR_COUNT_AFTER"
fi

# Очистка
echo ""
echo "Очистка временных файлов..."
rm -f /tmp/test_avatar.png /tmp/downloaded_avatar.png

echo ""
echo "======================================"
echo "✅ Тест завершен!"
echo ""
echo "📖 Используйте реальные изображения:"
echo "   curl -X POST '$API_BASE/users/1/avatar' -F 'file=@photo.jpg'"
echo ""
echo "🌐 Или откройте Swagger UI:"
echo "   http://localhost:8001/docs"
