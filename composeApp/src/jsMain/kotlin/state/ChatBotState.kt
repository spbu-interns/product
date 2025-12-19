package state

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

    var isOpen: Boolean = false
        private set

    var draft: String = ""
        private set

    fun toggleOpen() {
        isOpen = !isOpen
    }

    fun setOpen(value: Boolean) {
        isOpen = value
    }

    fun updateDraft(value: String) {
        draft = value
    }

    fun sendMessage() {
        val trimmed = draft.trim()
        if (trimmed.isBlank()) return

        messagesStore += ChatMessage(id = nextMessageId(), author = ChatAuthor.USER, text = trimmed)
        draft = ""
        messagesStore += ChatMessage(
            id = nextMessageId(),
            author = ChatAuthor.BOT,
            text = "Мы готовим интеграцию с чат-ботом. Пока что можно оставлять заметки в истории."
        )
    }

    private fun nextMessageId(): String = (++nextId).toString()
}