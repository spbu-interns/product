#!/usr/bin/env python3
"""
Test script for OpenRouter AI Medical Assistant Chat
Tests message sending and context preservation
"""

import requests
import json

BASE_URL = "http://localhost:8001"

def test_chat_conversation():
    """Test a multi-turn conversation with context"""
    print("=" * 60)
    print("Testing OpenRouter Chat API - Context Preservation")
    print("=" * 60)
    
    user_id = 1  # Test with first client from 016_test_data.sql
    
    # Message 1: Initial symptoms
    print("\n📤 Message 1: У меня болит голова 3 дня")
    response1 = requests.post(
        f"{BASE_URL}/chat/message",
        json={
            "user_id": user_id,
            "message": "У меня болит голова 3 дня"
        }
    )
    
    if response1.status_code != 200:
        print(f"❌ Error: {response1.status_code}")
        print(response1.text)
        return
    
    data1 = response1.json()
    session_id = data1["session_id"]
    print(f"✅ Session ID: {session_id}")
    print(f"🤖 Response:\n{data1['response']}\n")
    
    # Message 2: Add symptoms (testing context)
    print("\n📤 Message 2: А еще температура 38 и слабость")
    response2 = requests.post(
        f"{BASE_URL}/chat/message",
        json={
            "user_id": user_id,
            "message": "А еще температура 38 и слабость",
            "session_id": session_id
        }
    )
    
    if response2.status_code != 200:
        print(f"❌ Error: {response2.status_code}")
        print(response2.text)
        return
    
    data2 = response2.json()
    print(f"🤖 Response:\n{data2['response']}\n")
    
    # Message 3: Ask clarifying question
    print("\n📤 Message 3: К какому врачу мне лучше обратиться?")
    response3 = requests.post(
        f"{BASE_URL}/chat/message",
        json={
            "user_id": user_id,
            "message": "К какому врачу мне лучше обратиться?",
            "session_id": session_id
        }
    )
    
    if response3.status_code != 200:
        print(f"❌ Error: {response3.status_code}")
        print(response3.text)
        return
    
    data3 = response3.json()
    print(f"🤖 Response:\n{data3['response']}\n")
    
    # Get chat history
    print("\n📜 Retrieving chat history...")
    history_response = requests.get(f"{BASE_URL}/chat/history/{user_id}")
    
    if history_response.status_code != 200:
        print(f"❌ Error: {history_response.status_code}")
        print(history_response.text)
        return
    
    history = history_response.json()
    print(f"✅ Found {len(history)} session(s)")
    
    if history:
        latest_session = history[0]
        print(f"\n📝 Latest session messages ({len(latest_session['messages'])} total):")
        for i, msg in enumerate(latest_session["messages"], 1):
            role = msg["role"]
            text = msg["parts"][0]["text"] if msg["parts"] else ""
            icon = "👤" if role == "user" else "🤖"
            print(f"  {i}. {icon} {role}: {text[:80]}...")
    
    print("\n" + "=" * 60)
    print("✅ Test completed successfully!")
    print("=" * 60)
    
    # Cleanup: delete test session
    print(f"\n🗑️  Deleting test session {session_id}...")
    delete_response = requests.delete(f"{BASE_URL}/chat/session/{session_id}")
    if delete_response.status_code == 204:
        print("✅ Session deleted")
    else:
        print(f"⚠️  Delete failed: {delete_response.status_code}")


def test_different_symptoms():
    """Test with different medical scenarios"""
    print("\n" + "=" * 60)
    print("Testing Different Medical Scenarios")
    print("=" * 60)
    
    scenarios = [
        ("Боль в груди при физической нагрузке", 2),
        ("У ребенка температура 39 и кашель", 3),
        ("Болит зуб уже неделю", 4),
        ("Размытое зрение и мушки перед глазами", 5),
    ]
    
    for symptom, user_id in scenarios:
        print(f"\n📤 Scenario: {symptom}")
        response = requests.post(
            f"{BASE_URL}/chat/message",
            json={
                "user_id": user_id,
                "message": symptom
            }
        )
        
        if response.status_code == 200:
            data = response.json()
            print(f"🤖 Response:\n{data['response']}\n")
            
            # Cleanup
            requests.delete(f"{BASE_URL}/chat/session/{data['session_id']}")
        else:
            print(f"❌ Error: {response.status_code}")


if __name__ == "__main__":
    try:
        # Check if API is running
        health = requests.get(f"{BASE_URL}/health", timeout=2)
        if health.status_code != 200:
            print("❌ API is not responding properly")
            exit(1)
        
        # Run tests
        test_chat_conversation()
        test_different_symptoms()
        
    except requests.exceptions.ConnectionError:
        print("❌ Error: Cannot connect to API")
        print(f"   Make sure the server is running on {BASE_URL}")
        print("   Run: ./scripts/run-server.sh")
        exit(1)
    except Exception as e:
        print(f"❌ Unexpected error: {e}")
        exit(1)
