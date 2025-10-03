package api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

object ApiTest {
    
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
            })
        }
    }

    fun testConnection() {
        console.log("Testing API connection to ${ApiConfig.API_BASE_URL}")
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val response = client.get("${ApiConfig.API_BASE_URL}/api/ping")
                console.log("Ping response status: ${response.status}")
                console.log("Ping response: ${response.bodyAsText()}")
                
                val options = client.options("${ApiConfig.API_BASE_URL}/auth/login") {
                    headers {
                        append("Origin", "http://localhost:8080")
                    }
                }
                console.log("OPTIONS response status: ${options.status}")
                console.log("OPTIONS response headers: ${options.headers}")
                
                console.log("API connection test completed")
            } catch (e: Exception) {
                console.error("API connection test failed: ${e.message}")
                console.error(e)
            }
        }
    }

    fun runDiagnostics() {
        console.log("Running API diagnostics...")
        testConnection()
    }
}