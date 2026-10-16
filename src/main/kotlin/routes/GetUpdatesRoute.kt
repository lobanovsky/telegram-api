package ru.telegramapi.routes

import io.ktor.client.*
import io.ktor.client.plugins.timeout
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import ru.telegramapi.ErrorResponse

@Serializable
data class GetUpdatesRequest(val bot_token: String, val offset: Long = 0, val timeout: Int = 15)

@Serializable
data class TelegramGetUpdatesRequest(
    val offset: Long,
    val timeout: Int,
    val allowed_updates: List<String>
)

fun Application.configureUpdatesRouting(apiKey: String, httpClient: HttpClient) {
    routing {
        post("/get-updates") {
            if (call.request.header("X-Api-Key") != apiKey) {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse(error = "Unauthorized"))
                return@post
            }
            val request = call.receive<GetUpdatesRequest>()
            if (!request.bot_token.matches(Regex("[0-9]+:[A-Za-z0-9_-]+")) || request.offset < 0 || request.timeout !in 0..20) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse(error = "Invalid getUpdates parameters"))
                return@post
            }
            try {
                val response = httpClient.post("https://api.telegram.org/bot${request.bot_token}/getUpdates") {
                    contentType(ContentType.Application.Json)
                    timeout { requestTimeoutMillis = (request.timeout + 10) * 1000L }
                    setBody(TelegramGetUpdatesRequest(request.offset, request.timeout, listOf("my_chat_member")))
                }
                call.respondText(response.bodyAsText(), ContentType.Application.Json, response.status)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                // Исключения HTTP-клиента могут содержать URL с токеном бота.
                application.log.warn("Telegram getUpdates request failed")
                call.respond(HttpStatusCode.BadGateway, ErrorResponse(error = "Telegram getUpdates request failed"))
            }
        }
    }
}
