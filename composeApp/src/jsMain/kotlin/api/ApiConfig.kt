package api

import kotlin.js.JSON
import kotlin.js.json

object ApiConfig {
    const val API_BASE_URL = "http://localhost:8080"
    
    fun getDefaultHeaders(): Map<String, String> = mapOf(
        "Content-Type" to "application/json"
    )
    
    fun getAuthHeaders(token: String): Map<String, String> = getDefaultHeaders() + mapOf(
        "Authorization" to "Bearer $token"
    )
    
    fun saveToken(token: String) {
        localStorage.setItem("auth_token", token)
    }
    
    fun getToken(): String? {
        return localStorage.getItem("auth_token")
    }
    
    fun clearToken() {
        localStorage.removeItem("auth_token")
    }
    
    fun saveRole(role: String) {
        localStorage.setItem("user_role", role)
    }
    
    fun getRole(): String? {
        return localStorage.getItem("user_role")
    }
    
    fun clearRole() {
        localStorage.removeItem("user_role")
    }
}

external val localStorage: Storage

external interface Storage {
    fun getItem(key: String): String?
    fun setItem(key: String, value: String)
    fun removeItem(key: String)
}