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
    fun `POST submit with Idempotency-Key stores only one submission`() = testApplication {
        application { module() }
        val key = "test-idempotency-key-123"
        val body = """{"formId":"feedback_v1","values":{"rating":"4","recommend":"yes"}}"""

        // First submission
        val response1 = client.post("/forms/feedback_v1/submit") {
            contentType(ContentType.Application.Json)
            header("Idempotency-Key", key)
            setBody(body)
        }
        assertEquals(HttpStatusCode.OK, response1.status)
        val body1 = Json.parseToJsonElement(response1.bodyAsText()).jsonObject
        assertEquals(true, body1["success"]?.jsonPrimitive?.boolean)

        // Second submission with same key
        val response2 = client.post("/forms/feedback_v1/submit") {
            contentType(ContentType.Application.Json)
            header("Idempotency-Key", key)
            setBody(body)
        }
        assertEquals(HttpStatusCode.OK, response2.status)
        val body2 = Json.parseToJsonElement(response2.bodyAsText()).jsonObject
        assertEquals(true, body2["success"]?.jsonPrimitive?.boolean)

        // Verify only 1 submission stored via admin endpoint
        val adminResponse = client.get("/admin/forms/feedback_v1/submissions")
        val submissions = Json.parseToJsonElement(adminResponse.bodyAsText()).jsonArray
        // Count submissions with our values - should be 1 not 2
        val matchingSubmissions = submissions.filter { sub ->
            sub.jsonObject["values"]?.jsonObject?.get("rating")?.jsonPrimitive?.content == "4"
        }
        assertEquals(1, matchingSubmissions.size)
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
