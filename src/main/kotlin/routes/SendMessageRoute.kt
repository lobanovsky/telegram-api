package ru.telegramapi.routes

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import ru.telegramapi.ErrorResponse

@Serializable
data class SendMessageRequest(
    val bot_token: String,
    val chat_id: Long,
    val text: String
)

@Serializable
data class TelegramSendRequest(
    val chat_id: Long,
    val text: String
)

fun Application.configureRouting(apiKey: String, httpClient: HttpClient) {
    routing {
        post("/send-message") {
            val key = call.request.header("X-Api-Key")
            if (key != apiKey) {
                application.log.warn("Unauthorized request from ${call.request.origin.remoteHost}, key=${key?.take(8)?.let { "$it…" } ?: "<missing>"}")
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse(error = "Unauthorized"))
                return@post
            }

            val request = call.receive<SendMessageRequest>()

            val response = httpClient.post("https://api.telegram.org/bot${request.bot_token}/sendMessage") {
                contentType(ContentType.Application.Json)
                setBody(TelegramSendRequest(chat_id = request.chat_id, text = request.text))
            }

            val body = response.bodyAsText()
            if (!response.status.isSuccess()) {
                application.log.warn("Telegram API error: status=${response.status}, chat_id=${request.chat_id}, response=$body")
            }
            call.respondText(body, ContentType.Application.Json, response.status)
        }
    }
}
