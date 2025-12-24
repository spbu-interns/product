package api

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.http.*
import kotlinx.serialization.json.*
import org.interns.project.dto.ChatRequest
import org.interns.project.dto.ChatResponse
import org.interns.project.dto.ChatSessionOut

class ChatApiClient {

    private val client = ApiConfig.httpClient

    private suspend inline fun <reified T> parse(response: HttpResponse): T {
        if (!response.status.isSuccess()) {
            throw IllegalStateException("HTTP ${response.status.value}")
        }
        return response.body()
    }

    suspend fun sendMessage(
        request: ChatRequest
    ): ChatResponse {
        val response = client.post("${ApiConfig.BASE_URL}/chat/message") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        return parse(response)
    }

    suspend fun getActiveSession(
        userId: Int
    ): ChatSessionOut? {
        val response = client.get("${ApiConfig.BASE_URL}/chat/history/$userId") {
            parameter("limit", 1)
        }
        val sessions: List<ChatSessionOut> = parse(response)
        return sessions.firstOrNull()
    }

    /**
     * JSONB → (role, text)
     */
    fun extractMessages(
        session: ChatSessionOut
    ): List<Pair<String, String>> {
        return session.messages.mapNotNull { element ->
            try {
                val obj = element.jsonObject

                val role = obj["role"]
                    ?.jsonPrimitive
                    ?.contentOrNull
                    ?: return@mapNotNull null

                val text = obj["parts"]
                    ?.jsonArray
                    ?.firstOrNull()
                    ?.jsonObject
                    ?.get("text")
                    ?.jsonPrimitive
                    ?.contentOrNull
                    ?: return@mapNotNull null

                role to text
            } catch (e: Exception) {
                null
            }
        }
    }
}

object ChatApi {
    private val client = ChatApiClient()

    suspend fun sendMessage(
        userId: Int,
        message: String,
        sessionId: String?
    ): ChatResponse =
        client.sendMessage(
            ChatRequest(
                userId = userId,
                message = message,
                sessionId = sessionId
            )
        )

    suspend fun loadLastSession(
        userId: Int
    ): Pair<String?, List<Pair<String, String>>> {
        val session = client.getActiveSession(userId) ?: return null to emptyList()
        return session.sessionId to client.extractMessages(session)
    }
}