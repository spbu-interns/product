#!/usr/bin/env python3
"""
Test script for OpenRouter AI Medical Assistant Chat
Tests various symptom descriptions of different lengths
"""

import requests
import json

BASE_URL = "http://localhost:8001"


def run_single_test(test_name: str, user_id: int, message: str):
    """Run a single symptom test"""
    print("\n" + "=" * 70)
    print(f"🧪 {test_name}")
    print("=" * 70)
    print(f"\n📤 Симптомы:\n{message}\n")
    
    response = requests.post(
        f"{BASE_URL}/chat/message",
        json={
            "user_id": user_id,
            "message": message
        }
    )
    
    if response.status_code != 200:
        print(f"❌ Error: {response.status_code}")
        print(response.text)
        return False
    
    data = response.json()
    print(f"Ответ ллм:\n{data['response']}\n")
    print(f"✅ Session ID: {data['session_id']}")
    
    # Cleanup
    delete_response = requests.delete(f"{BASE_URL}/chat/session/{data['session_id']}")
    if delete_response.status_code == 204:
        print("🗑️  Session deleted")
    
    return True


def test_short_symptom():
    """Test 1: Краткое описание (1 предложение)"""
    return run_single_test(
        "Тест 1: Краткое описание",
        user_id=1,
        message="У меня болит голова и температура 37.5."
    )


def test_medium_symptom():
    """Test 2: Среднее описание (2 предложения)"""
    return run_single_test(
        "Тест 2: Среднее описание",
        user_id=2,
        message="У меня сильная боль в груди при физических нагрузках. Температура нормальная, но чувствую одышку."
    )


def test_detailed_symptom():
    """Test 3: Подробное описание (3 предложения)"""
    return run_single_test(
        "Тест 3: Подробное описание",
        user_id=3,
        message="У ребенка высокая температура 39.2 третий день. Сильный кашель, особенно ночью, и насморк. Аппетит пропал, жалуется на слабость."
    )


def test_very_detailed_symptom():
    """Test 4: Очень подробное описание (4 предложения)"""
    return run_single_test(
        "Тест 4: Очень подробное описание",
        user_id=4,
        message="Болит зуб в левой нижней части челюсти уже неделю. Боль усиливается при жевании и от горячего. Температура поднималась до 37.8 вчера вечером. Десна вокруг зуба покраснела и немного опухла."
    )


if __name__ == "__main__":
    try:
        # Check if API is running
        health = requests.get(f"{BASE_URL}/health", timeout=2)
        if health.status_code != 200:
            print("❌ API is not responding properly")
            exit(1)
        
        print("\n" + "🏥" * 35)
        print("OpenRouter Medical Assistant - Symptom Tests")
        print("🏥" * 35)
        
        # Run all tests
        results = []
        results.append(("Тест 1 (краткий)", test_short_symptom()))
        results.append(("Тест 2 (средний)", test_medium_symptom()))
        results.append(("Тест 3 (подробный)", test_detailed_symptom()))
        results.append(("Тест 4 (очень подробный)", test_very_detailed_symptom()))
        
        print("\n" + "=" * 70)
        print("Итоги тестов")
        print("=" * 70)
        
        passed = sum(1 for _, result in results if result)
        total = len(results)
        
        for name, result in results:
            status = "✅ PASS" if result else "❌ FAIL"
            print(f"{status} - {name}")
        
        print(f"\nИтого: {passed}/{total} тестов пройдено")
        
        if passed == total:
            print("\n🎉 Все тесты успешно пройдены!")
        else:
            print(f"\n⚠️  Не пройдено тестов: {total - passed}")
            exit(1)
        
    except requests.exceptions.ConnectionError:
        print("❌ Error: Cannot connect to API")
        print(f"   Make sure the server is running on {BASE_URL}")
        print("   Run: ./scripts/run-server.sh")
        exit(1)
    except Exception as e:
        print(f"❌ Unexpected error: {e}")
        exit(1)
