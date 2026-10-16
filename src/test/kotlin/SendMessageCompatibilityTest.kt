package ru.telegramapi

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.*
import kotlin.test.*
import ru.telegramapi.routes.configureRouting

class SendMessageCompatibilityTest {
    @Test
    fun sendsToOrdinaryChatAndExistingTopic() = testApplication {
        var requests = 0
        val upstream = HttpClient(MockEngine { request ->
            assertEquals("https://api.telegram.org/bot123456:token/sendMessage", request.url.toString())
            val body = Json.parseToJsonElement(request.body.toByteArray().decodeToString()).jsonObject
            assertEquals(-100123L, body.getValue("chat_id").jsonPrimitive.long)
            assertEquals("test", body.getValue("text").jsonPrimitive.content)
            assertFalse(body.containsKey("bot_token"))
            if (requests++ == 0) assertNull(body["message_thread_id"]?.jsonPrimitive?.contentOrNull)
            else assertEquals(2, body.getValue("message_thread_id").jsonPrimitive.int)
            respond("""{"ok":true}""", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))
        }) { install(ClientContentNegotiation) { json() } }
        application {
            install(ContentNegotiation) { json() }
            configureRouting("testkey", upstream)
        }
        for (suffix in listOf("", ",\"message_thread_id\":2")) {
            assertEquals(HttpStatusCode.OK, client.post("/send-message") {
                header("X-Api-Key", "testkey")
                contentType(ContentType.Application.Json)
                setBody("""{"bot_token":"123456:token","chat_id":-100123,"text":"test"$suffix}""")
            }.status)
        }
        assertEquals(2, requests)
        upstream.close()
    }
}
