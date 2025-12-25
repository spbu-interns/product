package ui

import io.kvision.core.Container
import io.kvision.core.onEvent
import io.kvision.form.text.TextArea
import io.kvision.form.text.textArea
import io.kvision.html.button
import io.kvision.html.div
import io.kvision.html.image
import io.kvision.html.span
import io.kvision.panel.SimplePanel
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLTextAreaElement
import state.ChatAuthor
import state.ChatBotState
import kotlin.math.min

fun Container.chatBotWidget(userId: Int) {

    val state = ChatBotState

    lateinit var messagesContainer: SimplePanel
    lateinit var inputField: TextArea
    lateinit var sendButton: io.kvision.html.Button
    lateinit var chatWindow: SimplePanel
    lateinit var toggleButton: io.kvision.html.Button

    /* ---------- UI helpers ---------- */

    fun updateOpenState() {
        chatWindow.addCssClass("open")
    }

    fun closeChat() {
        chatWindow.removeCssClass("open")
    }

    fun autoResizeInput() {
        val root = inputField.getElement() ?: return
        val el = (root.querySelector("textarea") as? HTMLTextAreaElement)
            ?: root.unsafeCast<HTMLTextAreaElement>()

        val maxLines = 10
        el.style.height = "auto"

        val lineHeight = (js("parseFloat(getComputedStyle(el).lineHeight)") as Double)
            .takeIf { !it.isNaN() && it > 0 } ?: 18.0

        val maxHeight = lineHeight * maxLines
        el.style.height = "${min(el.scrollHeight.toDouble(), maxHeight)}px"
    }
    
    fun resetInputHeight() {
        val root = inputField.getElement() ?: return
        val el = (root.querySelector("textarea") as? HTMLTextAreaElement)
            ?: root.unsafeCast<HTMLTextAreaElement>()

        el.style.height = ""
        el.scrollTop = 0.0
    }

    fun updateSendState() {
        sendButton.disabled = state.draft.trim().isBlank()
    }

    fun scrollMessagesToBottom() {
        messagesContainer.getElement()
            ?.unsafeCast<HTMLElement>()
            ?.let { it.scrollTop = it.scrollHeight.toDouble() }
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

    /* ---------- UI ---------- */

    chatWindow = div(className = "chat-bot-window") {

        div(className = "chat-bot-header") {
            span("🤖", className = "chat-bot-title__icon")
            span("ИИ-консультант", className = "chat-bot-title__text")

            button("✕", className = "chat-bot-close").onClick {
                state.toggleOpen(userId) { renderMessages() }
                closeChat()
            }
        }

        messagesContainer = div(className = "chat-bot-messages")

        div(className = "chat-bot-composer") {

            inputField = textArea(value = state.draft) {
                placeholder = "Введите сообщение"
                addCssClass("chat-bot-input")

                onEvent {
                    input = {
                        state.updateDraft(value ?: "")
                        updateSendState()
                        autoResizeInput()
                    }
                }
            }

            sendButton = button("", className = "chat-bot-send") {
                image(src = "images/send.png", alt = "Отправить") {
                    addCssClass("chat-bot-send__icon")
                }
            }

            sendButton.onClick {
                state.sendMessage(userId) { 
                    renderMessages()
                    updateSendState()
                    resetInputHeight()
                    autoResizeInput()
                }
                inputField.value = ""
            }
        }
    }

    toggleButton = button("💬", className = "chat-bot-toggle")
    toggleButton.onClick {
        state.toggleOpen(userId) { renderMessages() }

        if (chatWindow.hasCssClass("open")) {
            closeChat()
        } else {
            updateOpenState()
            renderMessages()
        }
    }

    /* ---------- init ---------- */

    renderMessages()
    updateSendState()
}
