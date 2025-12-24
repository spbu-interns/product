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

    /**
     * Открытие/закрытие чата
     */
    fun toggleOpen(userId: Int, onUpdate: () -> Unit = {}) {
    isOpen = !isOpen

    if (isOpen) {
        // если открыли чат для нового пользователя, очищаем историю
        if (messagesStore.isNotEmpty() && sessionId != null) {
            messagesStore.clear()
            sessionId = null
        }
        loadLastSession(userId, onUpdate)
    }
}


    fun updateDraft(value: String) {
        draft = value
    }

    /**
     * Отправка сообщения пользователя и получение ответа бота
     */
    fun sendMessage(userId: Int, onUpdate: () -> Unit = {}) {
    val text = draft.trim()
    if (text.isBlank()) return

    // Добавляем сообщение пользователя
    messagesStore += ChatMessage(
        id = nextId(),
        author = ChatAuthor.USER,
        text = text
    )

    draft = ""
    onUpdate()

    // Добавляем временное сообщение от бота ("typing indicator")
    val botTypingId = nextId()
    messagesStore += ChatMessage(
        id = botTypingId,
        author = ChatAuthor.BOT,
        text = "Бот думает..."
    )
    onUpdate()

    CoroutineScope(Dispatchers.Default).launch {
        try {
            val response = ChatApi.sendMessage(
                userId = userId,
                message = text,
                sessionId = sessionId
            )

            sessionId = response.sessionId

            // Обновляем временное сообщение на реальный ответ
            val index = messagesStore.indexOfFirst { it.id == botTypingId }
            if (index != -1) {
                messagesStore[index] = ChatMessage(
                    id = botTypingId,
                    author = ChatAuthor.BOT,
                    text = response.response
                )
            }
            onUpdate()
        } catch (e: Exception) {
            val index = messagesStore.indexOfFirst { it.id == botTypingId }
            if (index != -1) {
                messagesStore[index] = ChatMessage(
                    id = botTypingId,
                    author = ChatAuthor.BOT,
                    text = "Ошибка связи с сервером"
                )
            }
            onUpdate()
        }
    }
}


    /**
     * Загрузка последней сессии чата
     */
    fun loadLastSession(userId: Int, onUpdate: () -> Unit = {}) {
    messagesStore.clear()
    sessionId = null

    CoroutineScope(Dispatchers.Default).launch {
        val (sid, history) = ChatApi.loadLastSession(userId)
        sessionId = sid

        history.forEach { (role, text) ->
            messagesStore += ChatMessage(
                id = nextId(),
                author = if (role == "user") ChatAuthor.USER else ChatAuthor.BOT,
                text = text
            )
        }

        onUpdate()
    }
}


    private fun nextId() = (++nextId).toString()
}
