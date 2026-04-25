package org.interns.project.controller

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.interns.project.dto.*
import org.interns.project.users.repo.ApiUserRepo
import org.slf4j.LoggerFactory

class ChatController(private val apiUserRepo: ApiUserRepo) {
    private val logger = LoggerFactory.getLogger(ChatController::class.java)

    fun registerRoutes(routing: Route) {
        routing.route("/chat") {
            // Отправить сообщение
            post("/message") {
                try {
                    val request = call.receive<ChatRequest>()

                    // Валидация
                    if (request.userId <= 0) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ApiResponse<ChatResponse>(
                                success = false,
                                error = "Некорректный ID пользователя"
                            )
                        )
                        return@post
                    }

                    if (request.message.isBlank()) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ApiResponse<ChatResponse>(
                                success = false,
                                error = "Сообщение не может быть пустым"
                            )
                        )
                        return@post
                    }

                    // Отправка запроса на FastAPI
                    val response = apiUserRepo.sendChatMessage(request)

                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse<ChatResponse>(
                            success = true,
                            data = response
                        )
                    )
                } catch (e: Exception) {
                    logger.error("Ошибка при отправке сообщения", e)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<ChatResponse>(
                            success = false,
                            error = "Не удалось отправить сообщение: ${e.message}"
                        )
                    )
                }
            }

            // Получить историю чата
            get("/history/{user_id}") {
                try {
                    val userId = call.parameters["user_id"]?.toIntOrNull()
                        ?: throw IllegalArgumentException("Некорректный ID пользователя")

                    val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10

                    if (limit !in 1..100) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ApiResponse<Any>(
                                success = false,
                                error = "Лимит должен быть в диапазоне от 1 до 100"
                            )
                        )
                        return@get
                    }

                    val sessions = apiUserRepo.getChatHistory(userId, limit)

                    if (sessions.isEmpty()) {
                        call.respond(
                            HttpStatusCode.OK,
                            ApiResponse(
                                success = true,
                                data = emptyList<ChatSessionOut>()
                            )
                        )
                    } else {
                        call.respond(
                            HttpStatusCode.OK,
                            ApiResponse(
                                success = true,
                                data = sessions
                            )
                        )
                    }
                } catch (e: NumberFormatException) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<Any>(
                            success = false,
                            error = "Некорректный формат ID пользователя"
                        )
                    )
                } catch (e: Exception) {
                    logger.error("Ошибка при получении истории чата", e)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<Any>(
                            success = false,
                            error = "Не удалось получить историю чата: ${e.message}"
                        )
                    )
                }
            }

            // Удалить сессию
            delete("/session/{session_id}") {
                try {
                    val sessionId = call.parameters["session_id"]
                        ?: throw IllegalArgumentException("ID сессии обязателен")

                    val deleted = apiUserRepo.deleteChatSession(sessionId)

                    if (deleted) {
                        call.respond(
                            HttpStatusCode.OK,
                            ApiResponse(
                                success = true,
                                data = "Сессия успешно удалена"
                            )
                        )
                    } else {
                        call.respond(
                            HttpStatusCode.NotFound,
                            ApiResponse<Any>(
                                success = false,
                                error = "Сессия не найдена"
                            )
                        )
                    }
                } catch (e: Exception) {
                    logger.error("Ошибка при удалении сессии", e)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<Any>(
                            success = false,
                            error = "Не удалось удалить сессию: ${e.message}"
                        )
                    )
                }
            }

            // Получить активную сессию пользователя
            get("/active-session/{user_id}") {
                try {
                    val userId = call.parameters["user_id"]?.toIntOrNull()
                        ?: throw IllegalArgumentException("Некорректный ID пользователя")

                    // Получаем последнюю сессию
                    val sessions = apiUserRepo.getChatHistory(userId, 1)

                    if (sessions.isEmpty()) {
                        call.respond(
                            HttpStatusCode.OK,
                            ApiResponse(
                                success = true,
                                data = null
                            )
                        )
                    } else {
                        call.respond(
                            HttpStatusCode.OK,
                            ApiResponse<ChatSessionOut>(
                                success = true,
                                data = sessions.first()
                            )
                        )
                    }
                } catch (e: NumberFormatException) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<Any>(
                            success = false,
                            error = "Некорректный формат ID пользователя"
                        )
                    )
                } catch (e: Exception) {
                    logger.error("Ошибка при получении активной сессии", e)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<Any>(
                            success = false,
                            error = "Не удалось получить активную сессию: ${e.message}"
                        )
                    )
                }
            }
        }
    }
}