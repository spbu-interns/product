"""
Medical Assistant Chat with OpenRouter API
Provides doctor recommendations based on symptoms with context preservation
Uses free DeepSeek R1 Chimera model with excellent Russian language support
"""

import os
import requests
from typing import List, Dict

# OpenRouter API configuration
OPENROUTER_API_KEY = os.getenv("OPENROUTER_API_KEY")

if not OPENROUTER_API_KEY:
    raise ValueError("OPENROUTER_API_KEY must be set in environment variables")

OPENROUTER_API_URL = "https://openrouter.ai/api/v1/chat/completions"

# System prompt - focus ONLY on recommending the right specialist
SYSTEM_PROMPT = """
Ты - ассистент для записи к врачу. Твоя ЕДИНСТВЕННАЯ задача - определить, к какому специалисту направить пациента.

Доступные специалисты:
- Терапевт (общие вопросы здоровья, простуда, профилактика)
- Кардиолог (сердце, давление, боли в груди)
- Невролог (головные боли, головокружение, нервная система)
- Педиатр (здоровье детей)
- Стоматолог (зубы, десны, полость рта)
- Хирург (травмы, острые боли)
- Офтальмолог (зрение, глаза)

СТРОГО ЗАПРЕЩЕНО:
- Давать медицинские советы (пить воду, принимать лекарства, делать тесты)
- Ставить диагнозы (ОРВИ, грипп, COVID и т.д.)
- Объяснять причины симптомов
- Описывать что будет делать врач

РАЗРЕШЕНО ТОЛЬКО:
- Назвать специалиста
- Кратко (5-15 слов) объяснить почему именно этот специалист нужен

ФОРМАТ (СТРОГО):
"Рекомендую обратиться к [специалист]. [Причина в 5-15 словах]."

ПРИМЕРЫ ПРАВИЛЬНЫХ ОТВЕТОВ:
"Рекомендую обратиться к терапевту. Он проведет первичный осмотр."
"Рекомендую обратиться к кардиологу. Боли в груди требуют его консультации."
"Рекомендую обратиться к педиатру. Он специализируется на детских заболеваниях."
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
        "model": "tngtech/deepseek-r1t2-chimera:free",
        "messages": messages,
        "temperature": 0.6,
        "max_tokens": 1500  # Increased for DeepSeek to avoid mid-sentence cutoffs
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
        error_msg = f"OpenRouter API error {response.status_code}: {response.text}"
        print(f"❌ API Error: {error_msg}")
        raise ValueError(error_msg)
    
    # Extract response text
    try:
        result = response.json()
    except Exception as e:
        error_msg = f"Failed to parse OpenRouter response: {response.text[:500]}"
        print(f"❌ JSON Parse Error: {error_msg}")
        raise ValueError(error_msg)
    
    if "error" in result:
        error_msg = f"OpenRouter API error: {result['error']}"
        print(f"❌ API returned error: {error_msg}")
        raise ValueError(error_msg)
    
    # Extract and clean response text
    try:
        content = result["choices"][0]["message"]["content"]
    except (KeyError, IndexError) as e:
        print(f"❌ Unexpected response structure: {result}")
        raise ValueError(f"Unexpected OpenRouter response format: {result}")
    
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





