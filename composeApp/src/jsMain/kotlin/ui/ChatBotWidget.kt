package ui

import io.kvision.core.Container
import io.kvision.core.onClick
import io.kvision.core.onEvent
import io.kvision.form.text.text
import io.kvision.html.button
import io.kvision.html.div
import io.kvision.html.span
import io.kvision.panel.SimplePanel
import org.w3c.dom.HTMLElement
import state.ChatAuthor
import state.ChatBotState

fun Container.chatBotWidget() {
    val state = ChatBotState
    lateinit var messagesContainer: SimplePanel
    lateinit var inputField: io.kvision.form.text.Text
    lateinit var sendButton: io.kvision.html.Button
    lateinit var chatWindow: SimplePanel
    lateinit var toggleButton: io.kvision.html.Button

    fun updateOpenState() {
        if (state.isOpen) {
            chatWindow.addCssClass("open")
        } else {
            chatWindow.removeCssClass("open")
        }
    }

    fun updateSendState() {
        sendButton.disabled = state.draft.trim().isBlank()
    }

    fun scrollMessagesToBottom() {
        val element = messagesContainer.getElement()?.unsafeCast<HTMLElement>()
        if (element != null) {
            element.scrollTop = element.scrollHeight.toDouble()
        }
    }

    fun renderMessages() {
        messagesContainer.removeAll()
        if (state.messages.isEmpty()) {
            messagesContainer.div(className = "chat-bot-empty") {
                span("Задайте вопрос — здесь появится история переписки.")
            }
        } else {
            state.messages.forEach { message ->
                val className = when (message.author) {
                    ChatAuthor.USER -> "chat-bot-message chat-bot-message--user"
                    ChatAuthor.BOT -> "chat-bot-message chat-bot-message--bot"
                }
                messagesContainer.div(className = className) {
                    span(message.text)
                }
            }
        }
        scrollMessagesToBottom()
    }

    chatWindow = div(className = "chat-bot-window") {
        div(className = "chat-bot-header") {
            span("Чат-бот", className = "chat-bot-title")
            button("Закрыть", className = "chat-bot-close").onClick {
                state.setOpen(false)
                updateOpenState()
            }
        }
        messagesContainer = div(className = "chat-bot-messages")
        div(className = "chat-bot-composer") {
            inputField = text(value = state.draft) {
                placeholder = "Введите сообщение"
                addCssClass("chat-bot-input")
                onEvent {
                    input = { _ ->
                        state.updateDraft(value ?: "")
                        updateSendState()
                    }
                }
            }
            sendButton = button("Отправить", className = "chat-bot-send")
            sendButton.onClick {
                state.sendMessage()
                inputField.value = state.draft
                renderMessages()
                updateSendState()
            }
        }
    }

    toggleButton = button("\uD83D\uDCAC", className = "chat-bot-toggle")
    toggleButton.onClick {
        state.toggleOpen()
        updateOpenState()
    }

    renderMessages()
    updateSendState()
    updateOpenState()
}