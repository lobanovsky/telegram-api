package ru.telegramapi

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.*
import kotlin.test.*
import ru.telegramapi.routes.configureUpdatesRouting

class GetUpdatesRouteTest {
    @Test
    fun authenticatesValidatesAndForwardsMembershipUpdates() = testApplication {
        var requests = 0
        val body = """{"ok":true,"result":[{"update_id":42,"my_chat_member":{"chat":{"id":-100456,"type":"supergroup"},"new_chat_member":{"status":"member"}}}]}"""
        val upstream = HttpClient(MockEngine { request ->
            requests++
            assertEquals("https://api.telegram.org/bot123456:token/getUpdates", request.url.toString())
            val payload = Json.parseToJsonElement(request.body.toByteArray().decodeToString()).jsonObject
            assertEquals(41, payload.getValue("offset").jsonPrimitive.int)
            assertEquals(15, payload.getValue("timeout").jsonPrimitive.int)
            assertEquals(listOf("my_chat_member"), payload.getValue("allowed_updates").jsonArray.map { it.jsonPrimitive.content })
            assertFalse(payload.containsKey("bot_token"))
            respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))
        }) {
            install(ClientContentNegotiation) { json() }
            install(HttpTimeout)
        }
        application {
            install(ContentNegotiation) { json() }
            configureUpdatesRouting("testkey", upstream)
        }
        val unauthorized = client.post("/get-updates") {
            contentType(ContentType.Application.Json)
            setBody("""{"bot_token":"123456:token"}""")
        }
        assertEquals(HttpStatusCode.Unauthorized, unauthorized.status)
        for (invalid in listOf(
            """{"bot_token":"123456:token","offset":-1}""",
            """{"bot_token":"123456:token","timeout":21}""",
            """{"bot_token":"invalid/token"}"""
        )) {
            assertEquals(HttpStatusCode.BadRequest, client.post("/get-updates") {
                header("X-Api-Key", "testkey")
                contentType(ContentType.Application.Json)
                setBody(invalid)
            }.status)
        }
        assertEquals(0, requests)
        val response = client.post("/get-updates") {
            header("X-Api-Key", "testkey")
            contentType(ContentType.Application.Json)
            setBody("""{"bot_token":"123456:token","offset":41,"timeout":15}""")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(body, response.bodyAsText())
        assertEquals(1, requests)
        upstream.close()
    }
}
