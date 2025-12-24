package state

import api.ChatApi
import kotlinx.coroutines.*
import kotlin.js.Date

enum class ChatAuthor {
    USER,
    BOT
}

data class ChatMessage(
    val id: String,
    val author: ChatAuthor,
    val text: String,
    val createdAt: Double = Date().getTime()
)

object ChatBotState {

    private var nextId = 0
    private val messagesStore = mutableListOf<ChatMessage>()

    val messages: List<ChatMessage>
        get() = messagesStore

    var sessionId: String? = null
        private set

    var isOpen: Boolean = false
        private set

    var draft: String = ""
        private set

    fun toggleOpen(userId: Int) {
        isOpen = !isOpen
        if (isOpen && messagesStore.isEmpty()) {
            loadLastSession(userId)
        }
    }

    fun updateDraft(value: String) {
        draft = value
    }

    fun sendMessage(userId: Int) {
        val text = draft.trim()
        if (text.isBlank()) return

        messagesStore += ChatMessage(
            id = nextId(),
            author = ChatAuthor.USER,
            text = text
        )
        draft = ""

        CoroutineScope(Dispatchers.Default).launch {
            try {
                val response = ChatApi.sendMessage(
                    userId = userId,
                    message = text,
                    sessionId = sessionId
                )

                sessionId = response.sessionId

                messagesStore += ChatMessage(
                    id = nextId(),
                    author = ChatAuthor.BOT,
                    text = response.response
                )
            } catch (e: Exception) {
                messagesStore += ChatMessage(
                    id = nextId(),
                    author = ChatAuthor.BOT,
                    text = "Ошибка связи с сервером"
                )
            }
        }
    }

    private fun loadLastSession(userId: Int) {
        CoroutineScope(Dispatchers.Default).launch {
            val (sid, history) = ChatApi.loadLastSession(userId)
            sessionId = sid
            messagesStore.clear()

            history.forEach { (role, text) ->
                messagesStore += ChatMessage(
                    id = nextId(),
                    author = if (role == "user") ChatAuthor.USER else ChatAuthor.BOT,
                    text = text
                )
            }
        }
    }

    private fun nextId() = (++nextId).toString()
}