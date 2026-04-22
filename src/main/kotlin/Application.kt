package ru.telegramapi

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ru.telegramapi.routes.configureRouting

@Serializable
data class ErrorResponse(val ok: Boolean = false, val error: String)

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    val apiKey = environment.config.property("api.key").getString()

    val httpClient = HttpClient(CIO) {
        install(ClientContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        expectSuccess = false
    }

    install(ContentNegotiation) {
        json()
    }

    install(CallLogging)

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.application.log.error("Unhandled exception on ${call.request.httpMethod.value} ${call.request.uri}: ${cause.message}", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(error = cause.message ?: "Internal server error")
            )
        }
    }

    configureRouting(apiKey, httpClient)
}
