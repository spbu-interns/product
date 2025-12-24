package org.interns.project.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ChatMessagePart(
    @SerialName("text")
    val text: String
)

@Serializable
data class ChatMessage(
    @SerialName("role")
    val role: String, // "user" или "model"

    @SerialName("parts")
    val parts: List<ChatMessagePart>
)

@Serializable
data class ChatRequest(
    @SerialName("user_id")
    val userId: Int,

    @SerialName("message")
    val message: String,

    @SerialName("session_id")
    val sessionId: String? = null
)

@Serializable
data class ChatResponse(
    @SerialName("response")
    val response: String,

    @SerialName("session_id")
    val sessionId: String
)

@Serializable
data class ChatSessionOut(
    @SerialName("id")
    val id: Int,

    @SerialName("user_id")
    val userId: Int,

    @SerialName("session_id")
    val sessionId: String,

    @SerialName("messages")
    val messages: List<JsonElement>,

    @SerialName("created_at")
    val createdAt: String,

    @SerialName("updated_at")
    val updatedAt: String
)