package com.lfr.dynamicforms.routes

import com.lfr.dynamicforms.module
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.*
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AdminRoutesTest {

    private val testDbFile = File.createTempFile("adminRoutes", ".db")
    private val testDbUrl = "jdbc:sqlite:${testDbFile.absolutePath}"

    @AfterTest
    fun cleanup() {
        testDbFile.delete()
    }

    private val newFormJson = """
    {
        "formId": "test_form",
        "title": "Test Form",
        "description": "A test form",
        "pages": [{
            "pageId": "test_page_1",
            "title": "Page One",
            "elements": [{
                "type": "text_field",
                "id": "test_field",
                "label": "Test Field"
            }]
        }]
    }
    """.trimIndent()

    @Test
    fun `POST admin forms creates a form`() = testApplication {
        application { module(testDbUrl) }
        val response = client.post("/admin/forms") {
            basicAuth("admin", "admin")
            contentType(ContentType.Application.Json)
            setBody(newFormJson)
        }
        assertEquals(HttpStatusCode.Created, response.status)

        val getResponse = client.get("/forms/test_form")
        assertEquals(HttpStatusCode.OK, getResponse.status)
    }

    @Test
    fun `PUT admin forms updates a form`() = testApplication {
        application { module(testDbUrl) }
        val updatedJson = """
        {
            "formId": "registration_v1",
            "title": "Updated Registration",
            "description": "Updated description",
            "pages": [{
                "pageId": "p1",
                "title": "Page 1",
                "elements": [{
                    "type": "text_field",
                    "id": "name",
                    "label": "Name"
                }]
            }]
        }
        """.trimIndent()
        val response = client.put("/admin/forms/registration_v1") {
            basicAuth("admin", "admin")
            contentType(ContentType.Application.Json)
            setBody(updatedJson)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals("Updated Registration", body["title"]?.jsonPrimitive?.content)
    }

    @Test
    fun `PUT admin forms returns 404 for unknown form`() = testApplication {
        application { module(testDbUrl) }
        val response = client.put("/admin/forms/unknown") {
            basicAuth("admin", "admin")
            contentType(ContentType.Application.Json)
            setBody(newFormJson)
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `DELETE admin forms removes a form`() = testApplication {
        application { module(testDbUrl) }
        val response = client.delete("/admin/forms/feedback_v1") {
            basicAuth("admin", "admin")
        }
        assertEquals(HttpStatusCode.NoContent, response.status)

        val getResponse = client.get("/forms/feedback_v1")
        assertEquals(HttpStatusCode.NotFound, getResponse.status)
    }

    @Test
    fun `DELETE admin forms returns 404 for unknown form`() = testApplication {
        application { module(testDbUrl) }
        val response = client.delete("/admin/forms/nonexistent") {
            basicAuth("admin", "admin")
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `GET admin submissions returns empty list initially`() = testApplication {
        application { module(testDbUrl) }
        val response = client.get("/admin/forms/registration_v1/submissions") {
            basicAuth("admin", "admin")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonArray
        assertEquals(0, body.size)
    }

    @Test
    fun `GET admin submissions returns submissions after submit`() = testApplication {
        application { module(testDbUrl) }
        client.post("/forms/feedback_v1/submit") {
            contentType(ContentType.Application.Json)
            setBody("""{"formId":"feedback_v1","values":{"rating":"4","recommend":"yes"}}""")
        }
        val response = client.get("/admin/forms/feedback_v1/submissions") {
            basicAuth("admin", "admin")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonArray
        assertEquals(1, body.size)
    }

    @Test
    fun `admin routes return 401 without credentials`() = testApplication {
        application { module(testDbUrl) }
        val response = client.get("/admin/forms/registration_v1/submissions")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}
