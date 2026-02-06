package com.lfr.dynamicforms.routes

import com.lfr.dynamicforms.module
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FormRoutesTest {

    @Test
    fun `GET forms returns list of form summaries`() = testApplication {
        application { module() }
        val response = client.get("/forms")
        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonArray
        assertEquals(5, body.size)
        val first = body.first().jsonObject
        assertTrue(first.containsKey("formId"))
        assertTrue(first.containsKey("title"))
        assertTrue(first.containsKey("pageCount"))
        assertTrue(first.containsKey("fieldCount"))
    }

    @Test
    fun `GET forms by id returns form definition`() = testApplication {
        application { module() }
        val response = client.get("/forms/registration_v1")
        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals("registration_v1", body["formId"]?.jsonPrimitive?.content)
        assertEquals("User Registration", body["title"]?.jsonPrimitive?.content)
        assertTrue(body.containsKey("pages"))
    }

    @Test
    fun `GET forms by unknown id returns 404`() = testApplication {
        application { module() }
        val response = client.get("/forms/unknown_form")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `POST submit with valid data returns success`() = testApplication {
        application { module() }
        val response = client.post("/forms/feedback_v1/submit") {
            contentType(ContentType.Application.Json)
            setBody("""{"formId":"feedback_v1","values":{"rating":"4","recommend":"yes"}}""")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals(true, body["success"]?.jsonPrimitive?.boolean)
    }

    @Test
    fun `POST submit with validation errors returns field errors`() = testApplication {
        application { module() }
        val response = client.post("/forms/registration_v1/submit") {
            contentType(ContentType.Application.Json)
            setBody("""{"formId":"registration_v1","values":{}}""")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals(false, body["success"]?.jsonPrimitive?.boolean)
        assertTrue(body["fieldErrors"]?.jsonObject?.isNotEmpty() == true)
    }
}
