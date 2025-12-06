"""
Medical Assistant Chat with OpenRouter API
Provides doctor recommendations based on symptoms with context preservation
Uses free Meta Llama 3.1 8B model with Russian language support
"""

import os
import requests
from typing import List, Dict

# OpenRouter API configuration
OPENROUTER_API_KEY = os.getenv("OPENROUTER_API_KEY")

if not OPENROUTER_API_KEY:
    raise ValueError("OPENROUTER_API_KEY must be set in environment variables")

OPENROUTER_API_URL = "https://openrouter.ai/api/v1/chat/completions"

# System prompt with medical disclaimer
SYSTEM_PROMPT = """
Ты медицинский ассистент клиники. Твоя задача - помочь пациенту определить, к какому врачу обратиться на основе симптомов.

ДОСТУПНЫЕ СПЕЦИАЛИЗАЦИИ:
- Терапевт (общие заболевания, первичный осмотр)
- Кардиолог (сердце, давление, боли в груди)
- Невролог (головные боли, головокружения, нервная система)
- Педиатр (детские заболевания)
- Стоматолог (зубы, десны, полость рта)
- Хирург (травмы, операции, острые состояния)
- Офтальмолог (глаза, зрение)

ПРАВИЛА:
1. ОБЯЗАТЕЛЬНО в конце КАЖДОГО ответа добавляй:
   "⚠️ Я искусственный интеллект, не врач. Мои рекомендации не заменяют консультацию специалиста."

2. Рекомендуй конкретных врачей из списка выше
3. Объясняй, почему рекомендуешь именно этого специалиста
4. Если симптомы серьезные (боль в груди, высокая температура >39°C, потеря сознания) - рекомендуй немедленно обратиться к врачу или вызвать скорую
5. Не ставь диагнозы, только направляй к нужному специалисту
6. Задавай уточняющие вопросы если информации недостаточно

ФОРМАТ ОТВЕТА:
На основе симптомов рекомендую обратиться к:
• [Специалист] - [краткая причина]

[Дополнительные рекомендации если нужно]

⚠️ Я искусственный интеллект, не врач. Мои рекомендации не заменяют консультацию специалиста.
"""


def convert_history_to_openai_format(history: List[dict]) -> List[dict]:
    """
    Convert stored JSONB messages to OpenAI-compatible format
    
    Input format (from DB):
    [{"role": "user", "parts": [{"text": "message"}]}, ...]
    
    Output format (for OpenRouter/OpenAI):
    [{"role": "user", "content": "message"}, {"role": "assistant", "content": "response"}, ...]
    """
    openai_messages = []
    for msg in history:
        role = msg.get("role")
        parts = msg.get("parts", [])
        
        # Extract text from parts
        text = ""
        for part in parts:
            if isinstance(part, dict) and "text" in part:
                text = part["text"]
                break
            elif isinstance(part, str):
                text = part
                break
        
        if text:
            # Convert role: "model" -> "assistant" for OpenAI format
            openai_role = "assistant" if role == "model" else role
            openai_messages.append({
                "role": openai_role,
                "content": text
            })
    
    return openai_messages


def send_message_with_context(user_message: str, history: List[dict]) -> str:
    """
    Send message to OpenRouter API with conversation history
    
    Args:
        user_message: New message from user
        history: Previous messages in JSONB format from DB
    
    Returns:
        AI response text
    """
    # Convert history to OpenAI format
    messages = convert_history_to_openai_format(history)
    
    # Add system prompt as first message if history is empty
    if not messages:
        messages.insert(0, {
            "role": "system",
            "content": SYSTEM_PROMPT
        })
    
    # Add user message
    messages.append({
        "role": "user",
        "content": user_message
    })
    
    # Prepare request
    headers = {
        "Authorization": f"Bearer {OPENROUTER_API_KEY}",
        "HTTP-Referer": "http://localhost:8001",
        "X-Title": "Medical Assistant Chat"
    }
    
    payload = {
        "model": "mistralai/mistral-7b-instruct:free",
        "messages": messages,
        "temperature": 0.6,
        "max_tokens": 500
    }
    
    # Send request
    response = requests.post(
        OPENROUTER_API_URL, 
        headers=headers, 
        json=payload,
        timeout=60
    )
    
    # Check response
    if response.status_code != 200:
        raise ValueError(f"OpenRouter API error {response.status_code}: {response.text}")
    
    # Extract response text
    try:
        result = response.json()
    except Exception as e:
        raise ValueError(f"Failed to parse OpenRouter response: {response.text[:500]}")
    
    if "error" in result:
        raise ValueError(f"OpenRouter API error: {result['error']}")
    
    # Extract and clean response text
    content = result["choices"][0]["message"]["content"]
    
    # Remove special tokens like <s>, </s>, [INST], etc.
    content = content.replace("<s>", "").replace("</s>", "")
    content = content.replace("[INST]", "").replace("[/INST]", "")
    content = content.replace("[OUT]", "").replace("[/OUT]", "")
    cleaned_content = content.strip()
    
    # If empty after cleaning, return error message
    if not cleaned_content:
        print(f"⚠️ Warning: Empty response from API. Raw content: {repr(content)}")
        return "Извините, не смог сформировать ответ. Попробуйте переформулировать вопрос."
    
    return cleaned_content


def format_message_for_db(role: str, text: str) -> dict:
    """
    Format message for storing in JSONB
    
    Returns dict in format: {"role": "user"|"model", "parts": [{"text": "..."}]}
    Note: We use "model" role to keep compatibility with existing DB structure
    """
    return {
        "role": role,
        "parts": [{"text": text}]
    }





