#!/usr/bin/env python3
"""
Тестовый скрипт для проверки работы с аватарками.
Запускать после старта API: uvicorn app.main:app --reload --port 8001
"""

import requests
import io
from PIL import Image

API_BASE = "http://localhost:8001"

def create_test_image(filename="test_avatar.jpg", size=(500, 500), color=(100, 150, 200)):
    """Создать тестовое изображение"""
    img = Image.new('RGB', size, color=color)
    img.save(filename)
    print(f"✅ Создано тестовое изображение: {filename}")
    return filename

def test_upload_avatar(user_id=1):
    """Тест загрузки аватарки"""
    print(f"\n📤 Тест загрузки аватарки для user_id={user_id}")
    
    # Создаем тестовое изображение
    test_file = create_test_image()
    
    # Загружаем
    with open(test_file, 'rb') as f:
        response = requests.post(
            f"{API_BASE}/users/{user_id}/avatar",
            files={'file': f}
        )
    
    if response.status_code == 201:
        print(f"✅ Аватарка загружена: {response.json()}")
        return True
    else:
        print(f"❌ Ошибка загрузки: {response.status_code} - {response.text}")
        return False

def test_get_avatar(user_id=1):
    """Тест получения аватарки"""
    print(f"\n📥 Тест получения аватарки для user_id={user_id}")
    
    response = requests.get(f"{API_BASE}/users/{user_id}/avatar")
    
    if response.status_code == 200:
        # Сохраняем полученное изображение
        with open("downloaded_avatar.jpg", "wb") as f:
            f.write(response.content)
        print(f"✅ Аватарка получена и сохранена в downloaded_avatar.jpg")
        print(f"   Content-Type: {response.headers.get('content-type')}")
        print(f"   Размер: {len(response.content)} байт")
        return True
    else:
        print(f"❌ Ошибка получения: {response.status_code} - {response.text}")
        return False

def test_delete_avatar(user_id=1):
    """Тест удаления аватарки"""
    print(f"\n🗑️  Тест удаления аватарки для user_id={user_id}")
    
    response = requests.delete(f"{API_BASE}/users/{user_id}/avatar")
    
    if response.status_code == 204:
        print("✅ Аватарка удалена")
        return True
    else:
        print(f"❌ Ошибка удаления: {response.status_code} - {response.text}")
        return False

def test_large_image():
    """Тест загрузки большого изображения (должно быть сжато автоматически)"""
    print("\n🖼️  Тест загрузки большого изображения (3000x3000)")
    
    # Создаем большое изображение
    filename = "large_avatar.jpg"
    create_test_image(filename, size=(3000, 3000), color=(255, 100, 50))
    
    with open(filename, 'rb') as f:
        response = requests.post(
            f"{API_BASE}/users/1/avatar",
            files={'file': f}
        )
    
    if response.status_code == 201:
        print(f"✅ Большое изображение загружено и оптимизировано")
        return True
    else:
        print(f"❌ Ошибка: {response.status_code} - {response.text}")
        return False

def test_invalid_format():
    """Тест загрузки неподдерживаемого формата (должно быть отклонено)"""
    print("\n🚫 Тест загрузки неподдерживаемого формата (.txt)")
    
    # Создаем текстовый файл
    with open("test.txt", "w") as f:
        f.write("This is not an image")
    
    with open("test.txt", 'rb') as f:
        response = requests.post(
            f"{API_BASE}/users/1/avatar",
            files={'file': ('test.txt', f, 'text/plain')}
        )
    
    if response.status_code == 400:
        print(f"✅ Неверный формат правильно отклонен: {response.json()}")
        return True
    else:
        print(f"❌ Ожидалась ошибка 400, получен {response.status_code}")
        return False

def run_all_tests():
    """Запустить все тесты"""
    print("="*60)
    print("🧪 ТЕСТИРОВАНИЕ СИСТЕМЫ АВАТАРОК")
    print("="*60)
    
    # Проверка доступности API
    try:
        response = requests.get(f"{API_BASE}/health")
        if response.status_code != 200:
            print("❌ API недоступен. Запустите сервер: uvicorn app.main:app --reload --port 8001")
            return
    except requests.exceptions.ConnectionError:
        print("❌ API недоступен. Запустите сервер: uvicorn app.main:app --reload --port 8001")
        return
    
    print("✅ API доступен\n")
    
    # Запуск тестов
    tests = [
        ("Загрузка аватарки", test_upload_avatar),
        ("Получение аватарки", test_get_avatar),
        ("Большое изображение", test_large_image),
        ("Неверный формат", test_invalid_format),
        ("Удаление аватарки", test_delete_avatar),
    ]
    
    results = []
    for name, test_func in tests:
        try:
            result = test_func()
            results.append((name, result))
        except Exception as e:
            print(f"❌ Исключение в тесте '{name}': {e}")
            results.append((name, False))
    
    # Итоги
    print("\n" + "="*60)
    print("📊 РЕЗУЛЬТАТЫ ТЕСТОВ")
    print("="*60)
    
    for name, result in results:
        status = "✅ PASS" if result else "❌ FAIL"
        print(f"{status} - {name}")
    
    total = len(results)
    passed = sum(1 for _, r in results if r)
    print(f"\nИтого: {passed}/{total} тестов пройдено")

if __name__ == "__main__":
    run_all_tests()
