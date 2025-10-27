package api

import io.ktor.client.*
import io.ktor.client.engine.js.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

object ApiConfig {
    const val BASE_URL = "http://localhost:8000"

    object Endpoints {
        const val LOGIN = "$BASE_URL/api/auth/login"
        const val REGISTER = "$BASE_URL/api/auth/register"
        const val EMAIL_VERIFY = "$BASE_URL/api/auth/email/verify"
        const val PASSWORD_FORGOT = "$BASE_URL/api/auth/password/forgot"
        const val PASSWORD_RESET = "$BASE_URL/api/auth/password/reset"
        const val EMAIL_START_VERIFICATION = "$BASE_URL/api/auth/email/start"
    }

    const val TOKEN_STORAGE_KEY = "auth_token"
    private const val LANGUAGE_STORAGE_KEY = "ui_language"

    private fun storage(): dynamic = js(
        """
        (typeof globalThis !== 'undefined' && globalThis.localStorage)
            ? globalThis.localStorage
            : (typeof window !== 'undefined' && window.localStorage)
                ? window.localStorage
                : null
        """
    )

    private fun getItem(key: String): String? {
        val store = storage()
        return if (store != null) store.getItem(key) as String? else null
    }

    private fun setItem(key: String, value: String) {
        val store = storage()
        store?.setItem(key, value)
    }

    private fun removeItem(key: String) {
        val store = storage()
        store?.removeItem(key)
    }
    val httpClient = HttpClient(Js) {
        install(ContentNegotiation) {
            json(
                Json {
                    prettyPrint = false
                    isLenient = true
                    ignoreUnknownKeys = true
                }
            )
        }

        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.HEADERS
        }

        defaultRequest {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
        }
    }

    fun getToken(): String? =
        getItem(TOKEN_STORAGE_KEY)

    fun setToken(token: String) =
        setItem(TOKEN_STORAGE_KEY, token)

    fun clearToken() =
        removeItem(TOKEN_STORAGE_KEY)

    fun isAuthenticated(): Boolean =
        getToken() != null

    fun getLanguagePreference(): String? =
        getItem(LANGUAGE_STORAGE_KEY)

    fun setLanguagePreference(language: String) =
        setItem(LANGUAGE_STORAGE_KEY, language)
}
